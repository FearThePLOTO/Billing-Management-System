package billing.management.system;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Date;

public class EditUserController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> userTypeComboBox;
    @FXML private TextField idField;
    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField industryField;
    @FXML private Label industryLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateLabel;
    @FXML private Button saveButton;

    private User selectedUser;
    private boolean userUpdated = false;

    @FXML
    private void initialize() {
        userTypeComboBox.setItems(FXCollections.observableArrayList("Admin", "Company", "Customer"));

        // Listener to show/hide fields based on user type
        userTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if ("Company".equals(newValue)) {
                industryLabel.setVisible(true);
                industryField.setVisible(true);
                startDateLabel.setVisible(false);
                startDatePicker.setVisible(false);
            } else if ("Customer".equals(newValue)) {
                industryLabel.setVisible(false);
                industryField.setVisible(false);
                startDateLabel.setVisible(true);
                startDatePicker.setVisible(true);
            } else {
                industryLabel.setVisible(false);
                industryField.setVisible(false);
//                start ChibaLabel.setVisible(false);
                startDatePicker.setVisible(false);
            }
        });
    }

    @FXML
    private void handleSearch() {
        String username = searchField.getText().trim();
        if (username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Name", "Please enter a username to search.");
            return;
        }

        selectedUser = FileManager.loadUsers().stream()
                .filter(user -> user.getName().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);

        if (selectedUser == null) {
            showAlert(Alert.AlertType.ERROR, "User Not Found", "No user found with username: " + username);
            clearFields();
            disableEditFields(true);
            return;
        }

        // Populate fields
        idField.setText(String.valueOf(selectedUser.getId()));
        usernameField.setText(selectedUser.getName());
        passwordField.setText(selectedUser.getPassword());
        emailField.setText(selectedUser.getEmail());
        if (selectedUser instanceof Admin) {
            userTypeComboBox.getSelectionModel().select("Admin");
            industryField.setText("");
            startDatePicker.setValue(null);
        } else if (selectedUser instanceof Company company) {
            userTypeComboBox.getSelectionModel().select("Company");
            industryField.setText(company.getIndustry());
            startDatePicker.setValue(null);
        } else if (selectedUser instanceof Customer customer) {
            userTypeComboBox.getSelectionModel().select("Customer");
            industryField.setText("");
            startDatePicker.setValue(customer.getStartDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }
        disableEditFields(false);
    }

    @FXML
    private void handleSaveChanges() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();
        String userType = userTypeComboBox.getSelectionModel().getSelectedItem();
        String industry = industryField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || userType == null) {
            showAlert(Alert.AlertType.ERROR, "Missing Fields", "Please fill in all required fields.");
            return;
        }

        if ("Company".equals(userType) && industry.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Industry", "Please enter an industry for the company.");
            return;
        }

        if ("Customer".equals(userType) && startDate == null) {
            showAlert(Alert.AlertType.ERROR, "Missing Start Date", "Please select a start date for the customer.");
            return;
        }

        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        // Check for duplicate username (excluding current user)
        if (FileManager.loadUsers().stream()
                .anyMatch(u -> u.getName().equalsIgnoreCase(username) && u.getId() != selectedUser.getId())) {
            showAlert(Alert.AlertType.ERROR, "Duplicate Name", "Username already exists.");
            return;
        }

        // Create updated user
        try {
            User updatedUser;
            int id = selectedUser.getId();
            switch (userType) {
                case "Admin":
                    updatedUser = new Admin(username, password, id, email);
                    break;
                case "Company":
                    updatedUser = new Company(username, password, id, email, industry);
                    break;
                case "Customer":
                    Date userStartDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    updatedUser = new Customer(username, password, id, email, userStartDate);
                    break;
                default:
                    throw new IllegalStateException("Invalid user type");
            }
            FileManager.updateUserAndBills(updatedUser);
            userUpdated = true;
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully.");
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    private void clearFields() {
        idField.clear();
        usernameField.clear();
        passwordField.clear();
        emailField.clear();
        userTypeComboBox.getSelectionModel().clearSelection();
        industryField.clear();
        startDatePicker.setValue(null);
    }

    private void disableEditFields(boolean disable) {
        userTypeComboBox.setDisable(disable);
        usernameField.setDisable(disable);
        passwordField.setDisable(disable);
        emailField.setDisable(disable);
        industryField.setDisable(disable);
        startDatePicker.setDisable(disable);
        saveButton.setDisable(disable);
    }

    public boolean isUserUpdated() {
        return userUpdated;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}