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
import javafx.stage.Stage;
import java.text.SimpleDateFormat;

public class CustomerController {
    
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
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Helsinki"); // EEST
    
    public CustomerController() {
        this(null);
    }
    
    public CustomerController(User user) {
        this.currentUser = user;
    }
    
    @FXML
    private void initialize() {
        System.out.println("Initializing CustomerController with timezone: " + ZONE_ID);
        
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
            endDatePicker.setValue(LocalDate.now(ZONE_ID));
            startDatePicker.setValue(LocalDate.now(ZONE_ID).minusDays(30));
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> searchBills());
            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> searchBills());
        }
        
        if (billsTable != null) {
            billsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            
            TableColumn<Bill, Integer> billIdColumn = (TableColumn<Bill, Integer>) billsTable.getColumns().get(0);
            billIdColumn.setCellValueFactory(new PropertyValueFactory<>("billId"));
            billIdColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, Double> amountColumn = (TableColumn<Bill, Double>) billsTable.getColumns().get(1);
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
            amountColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
            
            TableColumn<Bill, String> companyColumn = (TableColumn<Bill, String>) billsTable.getColumns().get(2);
            companyColumn.setCellValueFactory(new PropertyValueFactory<>("companyName"));
            companyColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.25));
            
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
                    LocalDate dueLocalDate = dueDate.toInstant().atZone(ZONE_ID).toLocalDate();
                    LocalDate today = LocalDate.now(ZONE_ID);
                    boolean isOverdue = dueLocalDate.isBefore(today);
                    System.out.println("Bill ID " + bill.getBillId() + ": dueDate=" + dueDate + " (" + dueDate.getTime() + "ms), dueLocalDate=" + dueLocalDate + ", today=" + today + ", isOverdue=" + isOverdue);
                    return new SimpleStringProperty(isOverdue ? "Overdue" : "Unpaid");
                } catch (Exception e) {
                    System.err.println("Error computing status for bill ID " + (cellData.getValue() != null ? cellData.getValue().getBillId() : "unknown") + ": " + e.getMessage());
                    return new SimpleStringProperty("Unpaid");
                }
            });
            statusColumn.prefWidthProperty().bind(billsTable.widthProperty().multiply(0.15));
        } else {
            System.err.println("Warning: billsTable is null in CustomerInterface.fxml");
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
                    .filter(bill -> bill.getCustomerId() == currentUser.getId())
                    .collect(Collectors.toList()));
            System.out.println("Loaded " + billList.size() + " bills for customer ID " + currentUser.getId());
            if (billsTable != null) {
                billsTable.setItems(billList);
                billsTable.refresh();
            }
        } catch (Exception e) {
            System.err.println("Error loading bills: " + e.getMessage());
            billList.clear();
            showAlert("Error", "Failed to load bills: " + e.getMessage());
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
                    .filter(bill -> bill.getCustomerId() == currentUser.getId())
                    .filter(bill -> {
                        if (searchTerm.isEmpty()) return true;
                        String billIdStr = String.valueOf(bill.getBillId());
                        String valueStr = String.format("%.2f", bill.getValue());
                        String companyName = bill.getCompanyName() != null ? bill.getCompanyName().toLowerCase() : "";
                        String dateStr = bill.getDate() != null ? sdf.format(bill.getDate()) : "-";
                        String dueDateStr = bill.getDueDate() != null ? sdf.format(bill.getDueDate()) : "-";
                        String statusStr;
                        try {
                            if (bill.isIspaid()) {
                                statusStr = "paid";
                            } else if (bill.getDueDate() == null) {
                                statusStr = "unpaid";
                            } else {
                                LocalDate dueLocalDate = bill.getDueDate().toInstant().atZone(ZONE_ID).toLocalDate();
                                LocalDate today = LocalDate.now(ZONE_ID);
                                boolean isOverdue = dueLocalDate.isBefore(today);
                                statusStr = isOverdue ? "overdue" : "unpaid";
                                System.out.println("Search: Bill ID " + bill.getBillId() + ": dueDate=" + dueLocalDate + ", today=" + today + ", isOverdue=" + isOverdue);
                            }
                        } catch (Exception e) {
                            System.err.println("Error computing search status for bill ID " + bill.getBillId() + ": " + e.getMessage());
                            statusStr = "unpaid";
                        }
                        return billIdStr.contains(searchTerm) || 
                               valueStr.contains(searchTerm) || 
                               companyName.contains(searchTerm) ||
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
                            LocalDate dueLocalDate = dueDate.toInstant().atZone(ZONE_ID).toLocalDate();
                            LocalDate today = LocalDate.now(ZONE_ID);
                            boolean isOverdue = dueLocalDate.isBefore(today);
                            System.out.println("Filter: Bill ID " + bill.getBillId() + ": dueDate=" + dueLocalDate + ", today=" + today + ", isOverdue=" + isOverdue);
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
    @FXML
    private void markBillAsPaid() {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a bill to mark as paid.");
            return;
        }
        try {
            FileManager.updateBillStatus(selected.getBillId(), true);
            loadUserBills();
            showAlert("Success", "Bill marked as paid.");
        } catch (Exception e) {
            showAlert("Error", "Failed to update bill status: " + e.getMessage());
        }
    }
    
    @FXML
    private void markBillAsUnpaid() {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a bill to mark as unpaid.");
            return;
        }
        try {
            FileManager.updateBillStatus(selected.getBillId(), false);
            loadUserBills();
            showAlert("Success", "Bill marked as unpaid.");
        } catch (Exception e) {
            showAlert("Error", "Failed to update bill status: " + e.getMessage());
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