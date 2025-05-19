package billing.management.system;

import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private TextField visiblePasswordField;

    @FXML
    private void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    @FXML
    private void handleShowPassword() {
        boolean show = showPasswordCheckBox.isSelected();
        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = showPasswordCheckBox.isSelected() ? 
                         visiblePasswordField.getText() : 
                         passwordField.getText();

        // Check backdoor first
        if (authenticate(username, password)) {
            loadAdminInterface();
            return;
        }

        // Check users.txt
        User authenticatedUser = authenticateUser(username, password);
        if (authenticatedUser != null) {
            if (authenticatedUser instanceof Admin) {
                loadAdminInterface();
            } else if (authenticatedUser instanceof Company) {
                loadCompanyInterface(authenticatedUser);
            } else if (authenticatedUser instanceof Customer) {
                loadCustomerInterface(authenticatedUser);
            } else {
                showAlert("Login Failed", "Invalid user role");
            }
        } else {
            showAlert("Login Failed", "Invalid username or password");
        }
    }

    private boolean authenticate(String username, String password) {
        // Backdoor for admin
        return username.equals("a") && password.equals("a123");
    }

    private User authenticateUser(String username, String password) {
        List<User> users = FileManager.loadUsers();
        return users.stream()
                .filter(user -> user.login(password, username))
                .findFirst()
                .orElse(null);
    }

    private void loadAdminInterface() {
        try {
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
            
            Parent root = FXMLLoader.load(getClass().getResource("/billing/management/system/AdminInterface.fxml"));
            Stage adminStage = new Stage();
            adminStage.setScene(new Scene(root));
            adminStage.setTitle("Billing Management System - Admin");
            adminStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Admin interface: " + e.getMessage());
        }
    }

    private void loadCompanyInterface(User user) {
        try {
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/billing/management/system/CompanyInterface.fxml"));
            loader.setControllerFactory(c -> new CompanyController(user));
            Parent root = loader.load();
            Stage companyStage = new Stage();
            companyStage.setScene(new Scene(root));
            companyStage.setTitle("Billing Management System - Companies");
            companyStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Company interface: " + e.getMessage());
        }
    }

    private void loadCustomerInterface(User user) {
        try {
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/billing/management/system/CustomerInterface.fxml"));
            loader.setControllerFactory(c -> new CustomerController(user));
            Parent root = loader.load();
            Stage customerStage = new Stage();
            customerStage.setScene(new Scene(root));
            customerStage.setTitle("Billing Management System - Customer");
            customerStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Customer interface: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Do you really want to close the application?");
        alert.setContentText("Choose your option.");

        ButtonType yesButton = new ButtonType("Yes", ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);

        alert.showAndWait().ifPresent(type -> {
            if (type == yesButton) {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.close();
            }
        });
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}