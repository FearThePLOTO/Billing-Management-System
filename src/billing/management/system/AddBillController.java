package billing.management.system;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AddBillController {
    
    @FXML private TextField customerSearchField;
    @FXML private TableView<User> customerTable;
    @FXML private TextField amountField;
    @FXML private DatePicker billDatePicker;
    @FXML private DatePicker dueDatePicker;
    
    private User company;
    private boolean billAdded = false;
    
    public void setCompany(User company) {
        this.company = company;
        initializeCustomerTable();
    }
    
    public boolean isBillAdded() {
        return billAdded;
    }
    
    private void initializeCustomerTable() {
        if (customerTable != null) {
            TableColumn<User, Integer> idColumn = (TableColumn<User, Integer>) customerTable.getColumns().get(0);
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            
            TableColumn<User, String> nameColumn = (TableColumn<User, String>) customerTable.getColumns().get(1);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            
            TableColumn<User, String> emailColumn = (TableColumn<User, String>) customerTable.getColumns().get(2);
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
            
            loadCustomers("");
        }
    }
    
    private void loadCustomers(String searchTerm) {
        try {
            List<User> customers = FileManager.loadUsers().stream()
                    .filter(user -> user instanceof Customer)
                    .filter(user -> {
                        if (searchTerm.isEmpty()) return true;
                        String lowerSearch = searchTerm.toLowerCase();
                        return String.valueOf(user.getId()).contains(lowerSearch) ||
                               user.getName().toLowerCase().contains(lowerSearch) ||
                               user.getEmail().toLowerCase().contains(lowerSearch);
                    })
                    .collect(Collectors.toList());
            customerTable.setItems(FXCollections.observableArrayList(customers));
        } catch (Exception e) {
            showAlert("Error", "Failed to load customers: " + e.getMessage());
        }
    }
    
    @FXML
    private void searchCustomers() {
        String searchTerm = customerSearchField.getText().trim();
        loadCustomers(searchTerm);
    }
    
    @FXML
    private void addBill() {
        User selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null) {
            showAlert("No Selection", "Please select a customer.");
            return;
        }
        
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            showAlert("Input Error", "Please enter the bill amount.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert("Input Error", "Amount must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid amount format.");
            return;
        }
        
        LocalDate billDate = billDatePicker.getValue();
        if (billDate == null) {
            showAlert("Input Error", "Please select a bill date.");
            return;
        }
        if (billDate.isAfter(LocalDate.now())) {
            showAlert("Input Error", "Bill date cannot be in the future.");
            return;
        }
        
        LocalDate dueDate = dueDatePicker.getValue();
        if (dueDate == null) {
            dueDate = billDate.plusDays(30);
        }
        if (dueDate.isBefore(billDate)) {
            showAlert("Input Error", "Due date cannot be before bill date.");
            return;
        }
        if (dueDate.isBefore(LocalDate.now())) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Overdue Bill");
            confirm.setHeaderText("The due date (" + dueDate + ") is in the past, marking the bill as overdue.");
            confirm.setContentText("Do you want to proceed?");
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(yesButton, noButton);
            if (confirm.showAndWait().orElse(noButton) != yesButton) {
                return;
            }
        }
        
        try {
            int billId = FileManager.generateBillId();
            Date date = java.sql.Date.valueOf(billDate);
            Date dueDateObj = java.sql.Date.valueOf(dueDate);
            if (dueDateObj == null) {
                throw new IllegalStateException("Due date conversion resulted in null");
            }
            Bill newBill = new Bill(
                billId,
                amount,
                selectedCustomer.getId(),
                company.getId(),
                selectedCustomer.getName(),
                company.getName(),
                date,
                dueDateObj,
                false
            );
            
            FileManager.saveBill(newBill);
            billAdded = true;
            System.out.println("Added bill ID " + billId + ": amount=" + amount + ", billDate=" + billDate + ", dueDate=" + dueDate + ", isPaid=false");
            Stage stage = (Stage) amountField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("Error adding bill: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to add bill: " + e.getMessage());
        }
    }
    
    @FXML
    private void cancel() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}