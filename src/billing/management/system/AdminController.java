package billing.management.system;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController extends FileManager {
    // Main interface components
    @FXML private StackPane contentPane;
    @FXML private ComboBox<String> companyComboBox;
    @FXML private ComboBox<String> customerComboBox;

    // UserManagement.fxml components
    @FXML private TextField searchField;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private Pagination pagination;

    // CompanyReports.fxml components
    @FXML private TextField companySearchField;
    @FXML private TableView<Company> companyTable;
    @FXML private TableColumn<Company, Integer> companyIdColumn;
    @FXML private TableColumn<Company, String> companyNameColumn;
    @FXML private TableColumn<Company, Integer> companyBillsColumn;
    @FXML private TableColumn<Company, Double> companyRevenueColumn;
    @FXML private TableColumn<Company, String> companyIndustryColumn;
    @FXML private TableColumn<Company, Void> companyActionsColumn;
    @FXML private Pagination companyPagination;

    // CustomerReports.fxml components
    @FXML private TextField customerSearchField;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> customerIdColumn;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, Integer> customerBillsColumn;
    @FXML private TableColumn<Customer, Double> customerRevenueColumn;
    @FXML private TableColumn<Customer, Date> customerJoinDateColumn;
    @FXML private TableColumn<Customer, Double> customerPaidAmountColumn;
    @FXML private TableColumn<Customer, Void> customerActionsColumn;
    @FXML private Pagination customerPagination;

    private final ObservableList<String> sortOptions = FXCollections.observableArrayList(
            "Name (A-Z)", "Highest Revenue", "Most Bills");
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<Company> companyList = FXCollections.observableArrayList();
    private ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 26;
    private static final int ITEMS_PER_PAGE_2 = 19;
    

    @FXML
    private void initialize() {
        // Initialize ComboBoxes
        if (companyComboBox != null) {
            companyComboBox.setItems(sortOptions);
            companyComboBox.getSelectionModel().selectFirst();
            companyComboBox.setOnAction(event -> updateCompanyTable(0));
        }
        if (customerComboBox != null) {
            customerComboBox.setItems(sortOptions);
            customerComboBox.getSelectionModel().selectFirst();
            customerComboBox.setOnAction(event -> updateCustomerTable(0));
        }

        // Initialize User TableView columns
        if (usersTable != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            roleColumn.setCellValueFactory(cellData -> {
                User user = cellData.getValue();
                if (user instanceof Admin) return new SimpleStringProperty("Admin");
                if (user instanceof Company) return new SimpleStringProperty("Company");
                if (user instanceof Customer) return new SimpleStringProperty("Customer");
                return new SimpleStringProperty("Unknown");
            });
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        }

        // Initialize Company TableView columns
        if (companyTable != null) {
            companyIdColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
            companyNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
            companyBillsColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getTotalBills()).asObject());
            companyRevenueColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalrevenu()).asObject());
            companyIndustryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIndustry()));
            setupCompanyActionsColumn();
        }

        // Initialize Customer TableView columns
        if (customerTable != null) {
            customerIdColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
            customerNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
            customerBillsColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getTotalBills()).asObject());
            customerRevenueColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalrevenu()).asObject());
            customerJoinDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStartDate()));
            customerJoinDateColumn.setCellFactory(column -> new TableCell<>() {
                private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : formatter.format(item));
                }
            });
            customerPaidAmountColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalrevenu()).asObject());
            setupCustomerActionsColumn();
        }

        // Load initial data
        loadUsersData();
        loadCompanyData();
        loadCustomerData();
    }

    private void loadUsersData() {
        userList.setAll(FileManager.loadUsers());
        updatePagination();
        updateTableView(0);
    }

    private void loadCompanyData() {
        companyList.setAll(FileManager.loadUsers().stream()
                .filter(user -> user instanceof Company)
                .map(user -> (Company) user)
                .collect(Collectors.toList()));
        updateCompanyPagination();
        updateCompanyTable(0);
    }

    private void loadCustomerData() {
        customerList.setAll(FileManager.loadUsers().stream()
                .filter(user -> user instanceof Customer)
                .map(user -> (Customer) user)
                .collect(Collectors.toList()));
        updateCustomerPagination();
        updateCustomerTable(0);
    }

    private void updatePagination() {
        if (pagination == null) return;
        int pageCount = (int) Math.ceil((double) userList.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> updateTableView(newIndex.intValue()));
    }

    private void updateCompanyPagination() {
        if (companyPagination == null) return;
        int pageCount = (int) Math.ceil((double) companyList.size() / ITEMS_PER_PAGE_2);
        companyPagination.setPageCount(pageCount > 0 ? pageCount : 1);
        companyPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> updateCompanyTable(newIndex.intValue()));
    }

    private void updateCustomerPagination() {
        if (customerPagination == null) return;
        int pageCount = (int) Math.ceil((double) customerList.size() / ITEMS_PER_PAGE_2);
        customerPagination.setPageCount(pageCount > 0 ? pageCount : 1);
        customerPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> updateCustomerTable(newIndex.intValue()));
    }

    private void updateTableView(int pageIndex) {
        if (usersTable == null) return;
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, userList.size());
        usersTable.setItems(FXCollections.observableArrayList(userList.subList(fromIndex, toIndex)));
    }

    private void updateCompanyTable(int pageIndex) {
        if (companyTable == null) return;
        ObservableList<Company> sortedList = sortCompanies(companyList);
        int fromIndex = pageIndex * ITEMS_PER_PAGE_2;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE_2, sortedList.size());
        companyTable.setItems(FXCollections.observableArrayList(sortedList.subList(fromIndex, toIndex)));
    }

    private void updateCustomerTable(int pageIndex) {
        if (customerTable == null) return;
        ObservableList<Customer> sortedList = sortCustomers(customerList);
        int fromIndex = pageIndex * ITEMS_PER_PAGE_2;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE_2, sortedList.size());
        customerTable.setItems(FXCollections.observableArrayList(sortedList.subList(fromIndex, toIndex)));
    }

    private ObservableList<Company> sortCompanies(ObservableList<Company> companies) {
        String sortOption = companyComboBox != null ? companyComboBox.getSelectionModel().getSelectedItem() : "Name (A-Z)";
        ObservableList<Company> sortedList = FXCollections.observableArrayList(companies);
        switch (sortOption) {
            case "Name (A-Z)" -> sortedList.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
            case "Highest Revenue" -> sortedList.sort((c1, c2) -> Double.compare(c2.getTotalrevenu(), c1.getTotalrevenu()));
            case "Most Bills" -> sortedList.sort((c1, c2) -> Integer.compare(c2.getTotalBills(), c1.getTotalBills()));
        }
        return sortedList;
    }

    private ObservableList<Customer> sortCustomers(ObservableList<Customer> customers) {
        String sortOption = customerComboBox != null ? customerComboBox.getSelectionModel().getSelectedItem() : "Name (A-Z)";
        ObservableList<Customer> sortedList = FXCollections.observableArrayList(customers);
        switch (sortOption) {
            case "Name (A-Z)" -> sortedList.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
            case "Highest Revenue" -> sortedList.sort((c1, c2) -> Double.compare(c2.getTotalrevenu(), c1.getTotalrevenu()));
            case "Most Bills" -> sortedList.sort((c1, c2) -> Integer.compare(c2.getTotalBills(), c1.getTotalBills()));
        }
        return sortedList;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            userList.setAll(FileManager.loadUsers());
        } else {
            userList.setAll(FileManager.loadUsers().stream()
                    .filter(user -> {
                        String id = String.valueOf(user.getId()).toLowerCase();
                        String username = user.getName().toLowerCase();
                        String email = user.getEmail().toLowerCase();
                        String role = (user instanceof Admin ? "Admin" :
                                       user instanceof Company ? "Company" : "Customer").toLowerCase();
                        return id.contains(query) || username.contains(query) ||
                               email.contains(query) || role.contains(query);
                    })
                    .collect(Collectors.toList()));
        }
        updatePagination();
        updateTableView(0);
//        if (userList.isEmpty()) {
//            showAlert(Alert.AlertType.INFORMATION, "No Results", "No users found matching the search term.");
//        }
    }

    @FXML
    private void handleCompanySearch() {
        String query = companySearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadCompanyData();
        } else {
            companyList.setAll(FileManager.loadUsers().stream()
                    .filter(user -> user instanceof Company)
                    .map(user -> (Company) user)
                    .filter(company -> {
                        String id = String.valueOf(company.getId()).toLowerCase();
                        String name = company.getName().toLowerCase();
                        String industry = company.getIndustry().toLowerCase();
                        return id.contains(query) || name.contains(query) || industry.contains(query);
                    })
                    .collect(Collectors.toList()));
        }
        updateCompanyPagination();
        updateCompanyTable(0);
        if (companyList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No companies found matching the search term.");
        }
    }

    @FXML
    private void handleCustomerSearch() {
        String query = customerSearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadCustomerData();
        } else {
            customerList.setAll(FileManager.loadUsers().stream()
                    .filter(user -> user instanceof Customer)
                    .map(user -> (Customer) user)
                    .filter(customer -> {
                        String id = String.valueOf(customer.getId()).toLowerCase();
                        String name = customer.getName().toLowerCase();
                        String email = customer.getEmail().toLowerCase();
                        return id.contains(query) || name.contains(query) || email.contains(query);
                    })
                    .collect(Collectors.toList()));
        }
        updateCustomerPagination();
        updateCustomerTable(0);
        if (customerList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No customers found matching the search term.");
        }
    }

    private void setupCompanyActionsColumn() {
        companyActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBillsButton = new Button("View Bills");

            {
                viewBillsButton.setStyle("-fx-background-color: #0f3c6b; -fx-text-fill: white;");
                viewBillsButton.setOnAction(event -> {
                    Company company = getTableView().getItems().get(getIndex());
                    showBillsDialog(company);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10, viewBillsButton);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupCustomerActionsColumn() {
        customerActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBillsButton = new Button("View Bills");
            

            {
                viewBillsButton.setStyle("-fx-background-color: #0f3c6b; -fx-text-fill: white;");
                viewBillsButton.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    showBillsDialog(customer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10, viewBillsButton);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void showBillsDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewBills.fxml"));
            Parent root = loader.load();
            ViewBillsController controller = loader.getController();
            controller.setUser(user);
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("View Bills");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bills dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EditUserDialog.fxml"));
            Parent root = loader.load();
            EditUserController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Edit User");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
            if (controller.isUserUpdated()) {
                loadUsersData();
                loadCompanyData();
                loadCustomerData();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load edit user dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddUserDialog.fxml"));
            Parent root = loader.load();
            AddUserController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Add New User");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
            if (controller.isUserAdded()) {
                User newUser = controller.getNewUser();
                List<User> users = FileManager.loadUsers();
                users.add(newUser);
                FileManager.saveUsers(users);
                loadUsersData();
                loadCompanyData();
                loadCustomerData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully.");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load add user dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete " + selected.getName() + "?");
        alert.setContentText("This will also delete all associated bills for Company or Customer users. This action cannot be undone.");
        alert.showAndWait().ifPresent(type -> {
            if (type.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                List<User> users = FileManager.loadUsers();
                FileManager.deleteUser(selected.getId(), users);
                loadUsersData();
                loadCompanyData();
                loadCustomerData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User and associated bills (if any) deleted successfully.");
            }
        });
    }

    @FXML
    private void showUserManagement() {
        try {
            VBox userManagement = FXMLLoader.load(getClass().getResource("UserManagement.fxml"));
            contentPane.getChildren().setAll(userManagement);
            loadUsersData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showCompanyReports() {
        try {
            VBox reports = FXMLLoader.load(getClass().getResource("CompanyReports.fxml"));
            contentPane.getChildren().setAll(reports);
            loadCompanyData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showCustomerReports() {
        try {
            VBox reports = FXMLLoader.load(getClass().getResource("CustomerReports.fxml"));
            contentPane.getChildren().setAll(reports);
            loadCustomerData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadBillReports() {
        try {
            VBox reports = FXMLLoader.load(getClass().getResource("BillReports.fxml"));
            contentPane.getChildren().setAll(reports);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        try {
            Node source = (Node) contentPane;
            Stage currentStage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/billing/management/system/LoginScreen.fxml"));
            Parent root = loader.load();
            Scene loginScene = new Scene(root);
            currentStage.setScene(loginScene);
            currentStage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
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
                Node source = (Node) contentPane;
                Stage stage = (Stage) source.getScene().getWindow();
                stage.close();
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}