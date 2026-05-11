package com.maple.launcher.ui;

import com.maple.launcher.model.Installation;
import com.maple.launcher.model.InstallationType;
import com.maple.launcher.services.InstallationStore;
import com.maple.launcher.services.VersionDiscovery;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InstallationEditController {
    @FXML private TextField nameField;
    @FXML private ComboBox<InstallationType> typeBox;

    @FXML private Label mcVersionLabel;
    @FXML private ComboBox<String> versionBox;

    @FXML private VBox fabricLoaderBox;
    @FXML private ComboBox<String> loaderBox;
    @FXML private Label loaderInfoLabel;

    @FXML private Spinner<Integer> ramSpinner;

    @FXML private ComboBox<String> javaBox;
    @FXML private CheckBox keepOpenBox;

    private RootController root;
    private Installation current;

    public void setRootController(RootController root, String id) {
        this.root = root;
        current = root.getState().installations.stream().filter(x -> x.id.equals(id)).findFirst().orElseThrow();

        typeBox.setItems(FXCollections.observableArrayList(InstallationType.values()));
        typeBox.setValue(current.type);

        nameField.setText(current.name);

        ramSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 32, current.ramGb == null ? 4 : current.ramGb
        ));
        keepOpenBox.setSelected(current.keepLauncherOpen);

        loadJavaChoices();
        selectJavaValue();

        // initial load
        updateFabricLoaderVisibility(typeBox.getValue());
        reloadMcVersions(typeBox.getValue(), current.mcVersion);

        // type switch
        typeBox.setOnAction(e -> {
            InstallationType t = typeBox.getValue();
            updateFabricLoaderVisibility(t);
            reloadMcVersions(t, null);
        });

        // MC version change -> update loaders if fabric
        versionBox.setOnAction(e -> {
            if (typeBox.getValue() == InstallationType.FABRIC) {
                reloadFabricLoaders(versionBox.getValue(), null);
            }
        });

        // loader change -> update label
        loaderBox.setOnAction(e -> {
            if (typeBox.getValue() == InstallationType.FABRIC) {
                String mc = versionBox.getValue();
                String loader = loaderBox.getValue();
                if (mc != null && loader != null) {
                    loaderInfoLabel.setText("Loader " + loader + " for Minecraft " + mc);
                }
            }
        });
    }

    private void updateFabricLoaderVisibility(InstallationType type) {
        boolean show = type == InstallationType.FABRIC;
        fabricLoaderBox.setManaged(show);
        fabricLoaderBox.setVisible(show);

        if (show) {
            mcVersionLabel.setText("Minecraft Version (Fabric)");
        } else if (type == InstallationType.LOCAL_ROAMING) {
            mcVersionLabel.setText("Installed Version (Roaming)");
        } else {
            mcVersionLabel.setText("Minecraft Version");
        }
    }

    private void reloadMcVersions(InstallationType type, String preferred) {
        new Thread(() -> {
            List<String> versions = VersionDiscovery.getMinecraftVersionsOnline(type);
            if (versions.isEmpty()) versions = List.of("1.21.1");

            List<String> finalVersions = versions;
            Platform.runLater(() -> {
                versionBox.setItems(FXCollections.observableArrayList(finalVersions));

                String toSelect = preferred;
                if (toSelect == null || !finalVersions.contains(toSelect)) {
                    toSelect = finalVersions.get(0);
                }
                versionBox.setValue(toSelect);

                // if fabric, load loaders
                if (type == InstallationType.FABRIC) {
                    reloadFabricLoaders(toSelect, current == null ? null : current.fabricLoader);
                }
            });
        }).start();
    }

    private void reloadFabricLoaders(String mcVersion, String preferredLoader) {
        if (mcVersion == null || mcVersion.isBlank()) return;

        new Thread(() -> {
            List<String> loaders = VersionDiscovery.getFabricLoaderVersionsForMc(mcVersion);
            if (loaders.isEmpty()) loaders = List.of("latest");

            List<String> finalLoaders = loaders;
            Platform.runLater(() -> {
                loaderBox.setItems(FXCollections.observableArrayList(finalLoaders));

                String toSelect = preferredLoader;
                if (toSelect == null || !finalLoaders.contains(toSelect)) {
                    toSelect = finalLoaders.get(0);
                }
                loaderBox.setValue(toSelect);
                loaderInfoLabel.setText("Loader " + toSelect + " for Minecraft " + mcVersion);
            });
        }).start();
    }

    private void loadJavaChoices() {
        List<String> items = new ArrayList<>();
        items.add("Default (bundled/system)");

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            String exe = javaHome + "\\bin\\javaw.exe";
            items.add(exe);
        }
        javaBox.setItems(FXCollections.observableArrayList(items));
    }

    private void selectJavaValue() {
        if (current == null) return;

        if (current.javaPath == null || current.javaPath.isBlank()) {
            javaBox.setValue("Default (bundled/system)");
        } else {
            if (!javaBox.getItems().contains(current.javaPath)) javaBox.getItems().add(current.javaPath);
            javaBox.setValue(current.javaPath);
        }
    }

    @FXML
    public void onBrowseJava() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Java (javaw.exe)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Executable", "javaw.exe", "java.exe"));
        File f = fc.showOpenDialog(nameField.getScene().getWindow());
        if (f != null) {
            if (!javaBox.getItems().contains(f.getAbsolutePath())) javaBox.getItems().add(f.getAbsolutePath());
            javaBox.setValue(f.getAbsolutePath());
        }
    }

    @FXML
    public void onSave() {
        if (current == null) return;

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) name = "Installation";

        current.name = name;
        current.type = typeBox.getValue() == null ? InstallationType.VANILLA : typeBox.getValue();
        current.mcVersion = versionBox.getValue();

        if (current.type == InstallationType.FABRIC) {
            current.fabricLoader = loaderBox.getValue();
        } else {
            current.fabricLoader = "";
        }

        current.ramGb = ramSpinner.getValue();

        String javaSel = javaBox.getValue();
        current.javaPath = (javaSel == null || javaSel.startsWith("Default")) ? "" : javaSel;

        current.keepLauncherOpen = keepOpenBox.isSelected();

        InstallationStore.save(root.getState());
        root.setSelectedInstallation(current.id);
        root.notifyInstallationChanged();
        root.openInstallationsPage();
    }

    @FXML
    public void onClose() {
        root.openInstallationsPage();
    }
}