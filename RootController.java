package com.maple.launcher.ui;

import com.maple.launcher.RealDownloader;
import com.maple.launcher.Launcher;
import com.maple.launcher.AccountManager;
import com.maple.launcher.model.Installation;
import com.maple.launcher.model.InstallationType;
import com.maple.launcher.services.InstallationStore;
import com.maple.launcher.services.LauncherPaths;
import com.maple.launcher.services.VersionDiscovery;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;

public class RootController {
    @FXML private StackPane contentHost;
    @FXML private Label selectedInstallLabel, statusLabel, usernameLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button playButton;

    private InstallationStore.State state;

    // current selected account username
    private String selectedAccountName;

    @FXML
    public void initialize() {
        LauncherPaths.ROOT_DIR.mkdirs();
        LauncherPaths.INSTANCES_DIR.mkdirs();

        state = InstallationStore.load();

        // pick initial account (first one) if any
        List<AuthInfos> accounts = AccountManager.getSavedAccounts();
        if (!accounts.isEmpty()) selectedAccountName = accounts.get(0).getUsername();

        refreshAccountLabel();
        openInstallationsPage();
        refreshSelectedLabel();
    }

    public void setTheme(boolean isDark) {
        Scene scene = contentHost.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(isDark ? "/style-dark.css" : "/style-light.css").toExternalForm());
    }

    public InstallationStore.State getState() { return state; }

    public void setSelectedInstallation(String id) {
        state.selectedId = id;
        InstallationStore.save(state);
        refreshSelectedLabel();
    }

    public void setSelectedAccount(String username) {
        this.selectedAccountName = username;
        refreshAccountLabel();
    }

    public String getSelectedAccountName() {
        return selectedAccountName;
    }

    public void refreshAccountLabel() {
        if (selectedAccountName == null || selectedAccountName.isBlank()) {
            usernameLabel.setText("Not logged in");
        } else {
            usernameLabel.setText(selectedAccountName);
        }
    }

    private Installation selectedInstallation() {
        return state.installations.stream()
                .filter(x -> x.id.equals(state.selectedId))
                .findFirst()
                .orElse(state.installations.get(0));
    }

    private void refreshSelectedLabel() {
        Installation sel = selectedInstallation();

        // If it's "Latest Release" AND VANILLA, auto-resolve to latest from manifest (and persist)
        if ("Latest Release".equalsIgnoreCase(sel.name) && sel.type == InstallationType.VANILLA) {
            VersionDiscovery.getLatestReleaseFromManifest().ifPresent(latest -> {
                if (!latest.equals(sel.mcVersion)) {
                    sel.mcVersion = latest;
                    InstallationStore.save(state);
                }
            });
        }

        selectedInstallLabel.setText(sel.name + " • " + sel.type + " • " + sel.mcVersion);
    }

    @FXML public void openAccounts() { openPage("/pages/accounts.fxml"); }
    @FXML public void openInstallationsPage() { openPage("/pages/installations.fxml"); }
    @FXML public void openSettings() { openPage("/pages/settings.fxml"); }

    public void openEditInstallation(String id) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/pages/installation_edit.fxml"));
            Parent r = l.load();
            InstallationEditController c = l.getController();
            c.setRootController(this, id);
            contentHost.getChildren().setAll(r);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openPage(String fxml) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource(fxml));
            Parent r = l.load();
            if (l.getController() instanceof RootAware ra) ra.setRootController(this);
            contentHost.getChildren().setAll(r);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private AuthInfos resolveSelectedAuth() {
        if (selectedAccountName == null) return null;
        return AccountManager.getSavedAccounts().stream()
                .filter(a -> a.getUsername().equals(selectedAccountName))
                .findFirst()
                .orElse(null);
    }

    @FXML
    private void onPlay() {
        AuthInfos auth = resolveSelectedAuth();
        if (auth == null) {
            statusLabel.setText("Status: Select/create an account first");
            openAccounts();
            return;
        }

        Installation sel = selectedInstallation();
        playButton.setDisable(true);

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Downloading/Verifying...");
                    progressBar.setProgress(0);
                });

                File runDir = new File(sel.gameDir);
                runDir.mkdirs();

                String cat = sel.type == InstallationType.FABRIC ? "Fabric"
                        : (sel.type == InstallationType.SNAPSHOT ? "Snapshot" : "Vanilla");

                String finalVersion = RealDownloader.downloadGame(cat, sel.mcVersion, runDir, (prog, spd, files, task) -> {
                    Platform.runLater(() -> {
                        progressBar.setProgress(prog);
                        statusLabel.setText("Status: " + task + " • " + files);
                    });
                });

                Platform.runLater(() -> statusLabel.setText("Status: Launching..."));
                Launcher.start(auth, finalVersion, sel.ramGb == null ? 4 : sel.ramGb, sel.javaPath, runDir);

                if (!sel.keepLauncherOpen) Platform.runLater(() -> System.exit(0));
                else Platform.runLater(() -> {
                    playButton.setDisable(false);
                    statusLabel.setText("Status: Game is running");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    playButton.setDisable(false);
                    statusLabel.setText("Status: Launch failed (see console)");
                });
            }
        }).start();
    }

    // Called by editor after save to keep UI in sync
    public void notifyInstallationChanged() {
        refreshSelectedLabel();
    }
}