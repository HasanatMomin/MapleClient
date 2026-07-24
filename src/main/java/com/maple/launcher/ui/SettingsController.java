package com.maple.launcher.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class SettingsController implements RootAware {
    @FXML private ComboBox<String> themeBox;
    private RootController root;

    @Override public void setRootController(RootController root) { this.root = root; }

    @FXML public void initialize() {
        themeBox.getItems().addAll("Dark Theme", "Light Theme");
        themeBox.setValue("Dark Theme");
        themeBox.setOnAction(e -> {
            if (root != null) root.setTheme(themeBox.getValue().equals("Dark Theme"));
        });
    }
}