package billing.management.system;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javafx.stage.Stage;

public class AddUserController {
    @FXML private ComboBox<String> userTypeComboBox;
    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField industryField;
    @FXML private Label industryLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateLabel;

    private boolean userAdded = false;
    private User newUser;

    @FXML
    private void initialize() {
        userTypeComboBox.setItems(FXCollections.observableArrayList("Company", "Customer"));
        userTypeComboBox.getSelectionModel().selectFirst();
        industryLabel.setVisible(true);
        industryField.setVisible(true);
        // Listener to show/hide fields based on user type
        userTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if ("Company".equals(newValue)) {
                industryLabel.setVisible(true);
                industryField.setVisible(true);
                startDateLabel.setVisible(false);
                startDatePicker.setVisible(false);
            } else {
                industryLabel.setVisible(false);
                industryField.setVisible(false);
                startDateLabel.setVisible(true);
                startDatePicker.setVisible(true);
                startDatePicker.setValue(LocalDate.now()); // Default to today
            }
        });

        // Trigger initial visibility
        userTypeComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleAddUser() {
        String userType = userTypeComboBox.getSelectionModel().getSelectedItem();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();
        String industry = industryField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();

        // Validate inputs
        if (userType == null || username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Fields", "Please fill in all required fields.");
            return;
        }

        if ("Company".equals(userType) && industry.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Industry", "Please enter an industry for the company.");
            return;
        }

        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        // Check for duplicate username
        if (FileManager.loadUsers().stream()
                .anyMatch(u -> u.getName().equalsIgnoreCase(username))) {
            showAlert(Alert.AlertType.ERROR, "Duplicate Username", "Username already exists.");
            return;
        }

        // Generate user ID
        int userId = FileManager.generateUserId();

        // Set start date to today if not specified for Customer
        Date userStartDate = startDate != null 
            ? Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            : new Date();

        // Create new user
        try {
            if ("Company".equals(userType)) {
                newUser = new Company(username, password, userId, email, industry);
            } else {
                newUser = new Customer(username, password, userId, email, userStartDate);
            }
            userAdded = true;
            // Close dialog
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    public boolean isUserAdded() {
        return userAdded;
    }

    public User getNewUser() {
        return newUser;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}