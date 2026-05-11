package com.maple.launcher.ui;

import com.maple.launcher.model.Installation;
import com.maple.launcher.services.InstallationStore;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InstallationsController implements RootAware {
    @FXML private ListView<Installation> list;
    @FXML private Button dupBtn, delBtn, editBtn;
    private RootController root;

    @Override public void setRootController(RootController root) {
        this.root = root;
        refresh();
    }

    @FXML public void initialize() {
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Installation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + " (" + item.type + " " + item.mcVersion + ")");
            }
        });

        list.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            dupBtn.setDisable(!has); delBtn.setDisable(!has); editBtn.setDisable(!has);
            if (has && root != null) root.setSelectedInstallation(n.id);
        });
    }

    private void refresh() {
        InstallationStore.State s = root.getState();
        list.setItems(FXCollections.observableArrayList(s.installations));
        s.installations.stream().filter(x -> x.id.equals(s.selectedId)).findFirst().ifPresent(list.getSelectionModel()::select);
    }

    @FXML public void onCreate() {
        Installation i = InstallationStore.createDefault();
        i.name = "New installation";
        root.getState().installations.add(i);
        root.setSelectedInstallation(i.id);
        InstallationStore.save(root.getState());
        root.openEditInstallation(i.id);
    }

    @FXML public void onDuplicate() {
        Installation selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Installation c = InstallationStore.duplicate(selected);
        root.getState().installations.add(c);
        root.setSelectedInstallation(c.id);
        InstallationStore.save(root.getState());
        refresh();
    }

    @FXML public void onDelete() {
        InstallationStore.State s = root.getState();
        if (s.installations.size() <= 1) {
            new Alert(Alert.AlertType.INFORMATION, "You can’t delete the last installation.\nCreate another one first.").showAndWait();
            return;
        }
        Installation selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        s.installations.remove(selected);
        if (selected.id.equals(s.selectedId)) {
            s.selectedId = s.installations.get(0).id;
        }
        InstallationStore.save(s);
        root.notifyInstallationChanged();
        refresh();
    }

    @FXML public void onEdit() {
        Installation selected = list.getSelectionModel().getSelectedItem();
        if (selected != null) root.openEditInstallation(selected.id);
    }
}