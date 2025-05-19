package billing.management.system;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.text.SimpleDateFormat;
import java.io.IOException;

public class CompanyController {
    
    @FXML private BorderPane contentPane;
    @FXML private TextField billSearchField;
    @FXML private ComboBox<String> billStatusFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Pagination billsPagination;
    @FXML private TableView<Bill> billsTable;
    
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "All Bills", "Paid", "Unpaid", "Overdue"
    );
    private ObservableList<Bill> billList = FXCollections.observableArrayList();
    private User currentUser;
    private static final int ITEMS_PER_PAGE = 10;
    
    public CompanyController() {
        this(null);
    }
    
    public CompanyController(User user) {
        this.currentUser = user;
    }
    
    @FXML
    private void initialize() {
        if (billStatusFilter != null) {
            billStatusFilter.setItems(statusOptions);
            billStatusFilter.getSelectionModel().selectFirst();
            billStatusFilter.setOnAction(event -> searchBills());
        }
        
        if (billSearchField != null) {
            billSearchField.textProperty().addListener((obs, oldVal, newVal) -> searchBills());
            billSearchField.setOnAction(event -> searchBills());
        }
        
        if (startDatePicker != null && endDatePicker != null) {
            endDatePicker.setValue(LocalDate.now());
            startDatePicker.setValue(LocalDate.now().minusDays(30));
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> searchBills());
            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> searchBills());
        }
        
        if (billsTable != null) {
            billsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            TableColumn<Bill, Integer> billIdColumn = (TableColumn<Bill, Integer>) billsTable.getColumns().get(0);
            billIdColumn.setCellValueFactory(new PropertyValueFactory<>("billId"));
            billIdColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, Double> amountColumn = (TableColumn<Bill, Double>) billsTable.getColumns().get(1);
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
            amountColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, String> customerColumn = (TableColumn<Bill, String>) billsTable.getColumns().get(2);
            customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            customerColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.25));
            
            TableColumn<Bill, String> dateColumn = (TableColumn<Bill, String>) billsTable.getColumns().get(3);
            dateColumn.setCellValueFactory(cellData -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return new SimpleStringProperty(sdf.format(cellData.getValue().getDate()));
                } catch (Exception e) {
                    System.err.println("Error formatting date for bill ID " + cellData.getValue().getBillId() + ": " + e.getMessage());
                    return new SimpleStringProperty("-");
                }
            });
            dateColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, String> dueDateColumn = (TableColumn<Bill, String>) billsTable.getColumns().get(4);
            dueDateColumn.setCellValueFactory(cellData -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date dueDate = cellData.getValue().getDueDate();
                    return new SimpleStringProperty(dueDate != null ? sdf.format(dueDate) : "-");
                } catch (Exception e) {
                    System.err.println("Error formatting due date for bill ID " + cellData.getValue().getBillId() + ": " + e.getMessage());
                    return new SimpleStringProperty("-");
                }
            });
            dueDateColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, String> statusColumn = (TableColumn<Bill, String>) billsTable.getColumns().get(5);
            statusColumn.setCellValueFactory(cellData -> {
                try {
                    Bill bill = cellData.getValue();
                    if (bill == null) {
                        System.err.println("Null bill in status column for bill ID unknown");
                        return new SimpleStringProperty("Unpaid");
                    }
                    if (bill.isIspaid()) {
                        System.out.println("Bill ID " + bill.getBillId() + " is paid");
                        return new SimpleStringProperty("Paid");
                    }
                    Date dueDate = bill.getDueDate();
                    if (dueDate == null) {
                        System.err.println("Null due date for bill ID " + bill.getBillId() + "; treating as Unpaid");
                        return new SimpleStringProperty("Unpaid");
                    }
                    LocalDate dueLocalDate = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = LocalDate.now();
                    boolean isOverdue = dueLocalDate.isBefore(today);
                    System.out.println("Bill ID " + bill.getBillId() + ": dueDate=" + dueLocalDate + ", today=" + today + ", isOverdue=" + isOverdue);
                    return new SimpleStringProperty(isOverdue ? "Overdue" : "Unpaid");
                } catch (Exception e) {
                    System.err.println("Error computing status for bill ID " + (cellData.getValue() != null ? cellData.getValue().getBillId() : "unknown") + ": " + e.getMessage());
                    return new SimpleStringProperty("Unpaid");
                }
            });
            statusColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
        } else {
            System.err.println("Warning: billsTable is null in CompanyInterface.fxml");
        }
        
        loadUserBills();
        updatePagination();
    }
    
    private void loadUserBills() {
        if (currentUser == null) {
            System.err.println("No user logged in; skipping bill loading");
            billList.clear();
            if (billsTable != null) {
                billsTable.setItems(billList);
            }
            updatePagination();
            return;
        }
        try {
            List<Bill> allBills = FileManager.loadBillsForUser(currentUser.getId());
            billList.setAll(allBills.stream()
                    .filter(bill -> bill.getCompanyId() == currentUser.getId())
                    .collect(Collectors.toList()));
            System.out.println("Loaded " + billList.size() + " bills for company ID " + currentUser.getId());
        } catch (Exception e) {
            System.err.println("Error loading bills: " + e.getMessage());
            billList.clear();
            showAlert("Error", "Failed to load bills: " + e.getMessage());
        }
        if (billsTable != null) {
            billsTable.setItems(billList);
            billsTable.refresh();
        }
        updatePagination();
    }
    
    @FXML
    private void searchBills() {
        if (currentUser == null) {
            System.err.println("No user; skipping search");
            return;
        }
        
        String searchTerm = billSearchField != null ? billSearchField.getText().trim().toLowerCase() : "";
        String statusFilter = billStatusFilter != null ? billStatusFilter.getValue() : "All Bills";
        LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;
        LocalDate endDate = endDatePicker != null ? endDatePicker.getValue() : null;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
            List<Bill> filteredBills = FileManager.loadBillsForUser(currentUser.getId()).stream()
                    .filter(bill -> bill.getCompanyId() == currentUser.getId())
                    .filter(bill -> {
                        if (searchTerm.isEmpty()) return true;
                        String billIdStr = String.valueOf(bill.getBillId());
                        String valueStr = String.format("%.2f", bill.getValue());
                        String customerName = bill.getCustomerName() != null ? bill.getCustomerName().toLowerCase() : "";
                        String dateStr = bill.getDate() != null ? sdf.format(bill.getDate()) : "-";
                        String dueDateStr = bill.getDueDate() != null ? sdf.format(bill.getDueDate()) : "-";
                        String statusStr;
                        try {
                            if (bill.isIspaid()) {
                                statusStr = "paid";
                            } else if (bill.getDueDate() == null) {
                                statusStr = "unpaid";
                            } else {
                                LocalDate dueLocalDate = bill.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                boolean isOverdue = dueLocalDate.isBefore(LocalDate.now());
                                statusStr = isOverdue ? "overdue" : "unpaid";
                                System.out.println("Search: Bill ID " + bill.getBillId() + ": dueDate=" + dueLocalDate + ", isOverdue=" + isOverdue);
                            }
                        } catch (Exception e) {
                            System.err.println("Error computing search status for bill ID " + bill.getBillId() + ": " + e.getMessage());
                            statusStr = "unpaid";
                        }
                        return billIdStr.contains(searchTerm) || 
                               valueStr.contains(searchTerm) || 
                               customerName.contains(searchTerm) ||
                               dateStr.contains(searchTerm) ||
                               dueDateStr.contains(searchTerm) ||
                               statusStr.contains(searchTerm);
                    })
                    .filter(bill -> {
                        if (statusFilter == null || statusFilter.equals("All Bills")) return true;
                        try {
                            if (bill.isIspaid()) return statusFilter.equals("Paid");
                            Date dueDate = bill.getDueDate();
                            if (dueDate == null) return statusFilter.equals("Unpaid");
                            LocalDate dueLocalDate = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            boolean isOverdue = dueLocalDate.isBefore(LocalDate.now());
                            System.out.println("Filter: Bill ID " + bill.getBillId() + ": dueDate=" + dueLocalDate + ", isOverdue=" + isOverdue);
                            switch (statusFilter) {
                                case "Paid": return bill.isIspaid();
                                case "Unpaid": return !bill.isIspaid() && !isOverdue;
                                case "Overdue": return !bill.isIspaid() && isOverdue;
                                default: return true;
                            }
                        } catch (Exception e) {
                            System.err.println("Error filtering status for bill ID " + bill.getBillId() + ": " + e.getMessage());
                            return statusFilter.equals("Unpaid");
                        }
                    })
                    .filter(bill -> {
                        if (startDate == null || endDate == null) return true;
                        try {
                            long billTime = bill.getDate().getTime();
                            long startTime = java.sql.Date.valueOf(startDate).getTime();
                            long endTime = java.sql.Date.valueOf(endDate.plusDays(1)).getTime();
                            return billTime >= startTime && billTime < endTime;
                        } catch (Exception e) {
                            System.err.println("Error filtering date for bill ID " + bill.getBillId() + ": " + e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            billList.setAll(filteredBills);
            System.out.println("Search returned " + filteredBills.size() + " bills for searchTerm='" + searchTerm + "', statusFilter='" + statusFilter + "', dateRange=" + (startDate != null ? startDate.toString() : "null") + " to " + (endDate != null ? endDate.toString() : "null"));
            if (billsTable != null) {
                billsTable.setItems(billList);
                billsTable.refresh();
            }
            updatePagination();
        } catch (Exception e) {
            System.err.println("Error searching bills: " + e.getMessage());
            showAlert("Error", "Failed to search bills: " + e.getMessage());
        }
    }
    
    private void updatePagination() {
        if (billsPagination == null || billList == null) {
            System.err.println("Pagination or billList is null; skipping pagination update");
            return;
        }
        try {
            int pageCount = (int) Math.ceil((double) billList.size() / ITEMS_PER_PAGE);
            billsPagination.setPageCount(pageCount == 0 ? 1 : pageCount);
            billsPagination.setPageFactory(pageIndex -> {
                int fromIndex = pageIndex * ITEMS_PER_PAGE;
                int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, billList.size());
                ObservableList<Bill> pageItems = FXCollections.observableArrayList();
                if (fromIndex < billList.size()) {
                    pageItems.addAll(billList.subList(fromIndex, toIndex));
                }
                if (billsTable != null) {
                    billsTable.setItems(pageItems);
                    billsTable.refresh();
                }
                return new VBox(billsTable != null ? billsTable : new Label("No bills to display"));
            });
        } catch (Exception e) {
            System.err.println("Error updating pagination: " + e.getMessage());
            showAlert("Error", "Failed to update bill table: " + e.getMessage());
        }
    }
    
    @FXML
    private void addBill() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddBillDialog.fxml"));
            Parent root = loader.load();
            AddBillController controller = loader.getController();
            controller.setCompany(currentUser);
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Add New Bill");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
            if (controller.isBillAdded()) {
                loadUserBills();
                showAlert("Success", "Bill added successfully.");
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to open add bill dialog: " + e.getMessage());
        }
    }
    
    @FXML
    private void deleteSelectedBills() {
        ObservableList<Bill> selectedBills = billsTable.getSelectionModel().getSelectedItems();
        if (selectedBills.isEmpty()) {
            showAlert("No Selection", "Please select at least one bill to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Are you sure you want to delete " + selectedBills.size() + " bill(s)?");
        confirm.setContentText("This action cannot be undone.");
        
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yesButton, noButton);
        
        confirm.showAndWait().ifPresent(type -> {
            if (type == yesButton) {
                try {
                    List<Integer> billIds = selectedBills.stream()
                            .map(Bill::getBillId)
                            .collect(Collectors.toList());
                    FileManager.deleteBills(billIds);
                    loadUserBills();
                    showAlert("Success", "Selected bills deleted successfully.");
                } catch (Exception e) {
                    System.err.println("Error deleting bills: " + e.getMessage());
                    showAlert("Error", "Failed to delete bills: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void logout() {
        try {
            Node source = contentPane;
            Stage currentStage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/billing/management/system/LoginScreen.fxml"));
            Parent root = loader.load();
            Scene loginScene = new Scene(root);
            currentStage.setScene(loginScene);
            currentStage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClose() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Do you really want to close the application?");
        alert.setContentText("Choose your option.");

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);

        alert.showAndWait().ifPresent(type -> {
            if (type == yesButton) {
                Node source = contentPane;
                Stage stage = (Stage) source.getScene().getWindow();
                stage.close();
            }
        });
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}