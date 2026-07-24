package com.maple.launcher.ui;

import com.maple.launcher.AccountManager;
import com.maple.launcher.AuthManager;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class AccountsController implements RootAware {
    @FXML private TextField offlineUserField;
    @FXML private ListView<String> accountList;
    @FXML private Button useSelectedBtn;
    @FXML private Button deleteBtn;

    private RootController root;

    @Override
    public void setRootController(RootController root) {
        this.root = root;
        refresh();
    }

    @FXML
    public void initialize() {
        accountList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            useSelectedBtn.setDisable(!has);
            deleteBtn.setDisable(!has);
        });
        useSelectedBtn.setDisable(true);
        deleteBtn.setDisable(true);
    }

    private void refresh() {
        List<AuthInfos> accs = AccountManager.getSavedAccounts();

        // Map the AuthInfos to a formatted String: "Username (ROLE)"
        List<String> displayList = accs.stream().map(auth -> {
            AccountManager.AccountRecord rec = AccountManager.find(auth.getUsername());
            if (rec != null) {
                return String.format("%s (%s)", rec.name(), rec.role());
            }
            return auth.getUsername(); // Fallback if record not found
        }).collect(Collectors.toList());

        accountList.setItems(FXCollections.observableArrayList(displayList));

        // Selection logic needs to find the item that starts with the current name
        if (root != null && root.getSelectedAccountName() != null) {
            String current = root.getSelectedAccountName();
            accountList.getItems().stream()
                    .filter(item -> item.startsWith(current + " ("))
                    .findFirst()
                    .ifPresent(item -> accountList.getSelectionModel().select(item));
        } else if (!accountList.getItems().isEmpty()) {
            accountList.getSelectionModel().selectFirst();
        }
    }

    // Helper to extract "Maple" from "Maple (PREMIUM)"
    private String getRawUsername(String selectedItem) {
        if (selectedItem == null) return null;
        if (selectedItem.contains(" (")) {
            return selectedItem.substring(0, selectedItem.lastIndexOf(" ("));
        }
        return selectedItem;
    }

    @FXML
    public void onAddOffline() {
        String u = offlineUserField.getText() == null ? "" : offlineUserField.getText().trim();
        if (u.isEmpty()) return;

        AccountManager.saveAccount(AuthManager.loginCracked(u), false);
        offlineUserField.clear();

        refresh();
        if (root != null) {
            root.setSelectedAccount(u);
        }
    }

    @FXML
    public void onAddPremium() {
        new Thread(() -> {
            try {
                AuthInfos a = AuthManager.loginMicrosoft();
                AccountManager.saveAccount(a, true);
                Platform.runLater(() -> {
                    refresh();
                    if (root != null) root.setSelectedAccount(a.getUsername());
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    public void onUseSelected() {
        String sel = getRawUsername(accountList.getSelectionModel().getSelectedItem());
        if (sel != null && root != null) {
            root.setSelectedAccount(sel);
        }
    }

    @FXML
    public void onDelete() {
        String sel = getRawUsername(accountList.getSelectionModel().getSelectedItem());
        if (sel == null) return;

        AccountManager.deleteAccount(sel);
        refresh();

        if (root != null) {
            if (!accountList.getItems().isEmpty()) {
                root.setSelectedAccount(getRawUsername(accountList.getItems().get(0)));
            } else {
                root.setSelectedAccount(null);
            }
        }
    }
}