package billing.management.system;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;

public class BillController {
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, Integer> billIdColumn;
    @FXML private TableColumn<Bill, Double> valueColumn;
    @FXML private TableColumn<Bill, String> customerNameColumn;
    @FXML private TableColumn<Bill, String> companyNameColumn;
    @FXML private TableColumn<Bill, Date> dateColumn;
    @FXML private TableColumn<Bill, Date> dueDateColumn;
    @FXML private TableColumn<Bill, Boolean> isPaidColumn;
    @FXML private Label totalRevenueLabel;
    @FXML private Label averageBillLabel;
    @FXML private Label paidBillsLabel;
    @FXML private Label unpaidBillsLabel;
    @FXML private PieChart paymentStatusChart;
    @FXML private LineChart<String, Number> revenueTrendChart;
    @FXML private Pagination billsPagination;

    private final ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "All Bills", "Paid", "Unpaid", "Overdue");
    private ObservableList<Bill> billList = FXCollections.observableArrayList();
    private static final int ITEMS_PER_PAGE = 21;

    @FXML
    private void initialize() {
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(statusOptions);
            statusFilterCombo.getSelectionModel().selectFirst();
        }

        if (endDatePicker != null) {
            endDatePicker.setValue(LocalDate.now());
        }
        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.now().minusDays(30));
        }

        if (billsTable != null) {
            billIdColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBillId()).asObject());
            valueColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getValue()).asObject());
            customerNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
            companyNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCompanyName()));
            dateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDate()));
            dateColumn.setCellFactory(column -> new TableCell<>() {
                private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : formatter.format(item));
                }
            });
            dueDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDueDate()));
            dueDateColumn.setCellFactory(column -> new TableCell<>() {
                private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : formatter.format(item));
                }
            });
            isPaidColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isIspaid()));
            isPaidColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : (item ? "Yes" : "No"));
                }
            });
        }

        if (billsPagination != null) {
            billsPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> updateBillsTable(newIndex.intValue()));
        }

        loadBills();
        updateBillsTable(0);
        updateStatistics();
        updateCharts();
    }

    private void loadBills() {
        billList.setAll(FileManager.loadBills());
        updatePagination();
    }

    private void updatePagination() {
        if (billsPagination == null) return;
        int pageCount = (int) Math.ceil((double) billList.size() / ITEMS_PER_PAGE);
        billsPagination.setPageCount(pageCount > 0 ? pageCount : 1);
    }

    private void updateBillsTable(int pageIndex) {
        if (billsTable == null) return;

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String status = statusFilterCombo.getValue();

        ObservableList<Bill> filteredBills = billList.filtered(bill -> {
            Date billDate = bill.getDate();
            LocalDate billLocalDate = billDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            boolean dateInRange = (startDate == null || !billLocalDate.isBefore(startDate)) &&
                                 (endDate == null || !billLocalDate.isAfter(endDate));

            boolean statusMatch;
            switch (status) {
                case "Paid":
                    statusMatch = bill.isIspaid();
                    break;
                case "Unpaid":
                    statusMatch = !bill.isIspaid();
                    break;
                case "Overdue":
                    statusMatch = !bill.isIspaid() && bill.getDueDate() != null && bill.getDueDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now());
                    break;
                case "All Bills":
                default:
                    statusMatch = true;
                    break;
            }

            return dateInRange && statusMatch;
        });

        int pageCount = (int) Math.ceil((double) filteredBills.size() / ITEMS_PER_PAGE);
        billsPagination.setPageCount(pageCount > 0 ? pageCount : 1);

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredBills.size());
        billsTable.setItems(FXCollections.observableArrayList(filteredBills.subList(fromIndex, toIndex)));

        if (filteredBills.isEmpty()) {
            billsTable.setPlaceholder(new javafx.scene.text.Text("No bills found for the selected filters."));
        }
    }

    @FXML
    private void applyBillFilters() {
        updateBillsTable(0);
        updateStatistics();
        updateCharts();
    }

    @FXML
    private void exportBillData() {
        showAlert(Alert.AlertType.INFORMATION, "Data Exported", "Bill data has been exported successfully (CSV export not implemented).");
    }

    private void updateStatistics() {
        if (billList == null || billList.isEmpty()) {
            totalRevenueLabel.setText("$0.00");
            averageBillLabel.setText("$0.00");
            paidBillsLabel.setText("0");
            unpaidBillsLabel.setText("0");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String status = statusFilterCombo.getValue();

        List<Bill> filteredBills = billList.stream().filter(bill -> {
            Date billDate = bill.getDate();
            LocalDate billLocalDate = billDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            boolean dateInRange = (startDate == null || !billLocalDate.isBefore(startDate)) &&
                                 (endDate == null || !billLocalDate.isAfter(endDate));

            boolean statusMatch;
            switch (status) {
                case "Paid":
                    statusMatch = bill.isIspaid();
                    break;
                case "Unpaid":
                    statusMatch = !bill.isIspaid();
                    break;
                case "Overdue":
                    statusMatch = !bill.isIspaid() && bill.getDueDate() != null && bill.getDueDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now());
                    break;
                case "All Bills":
                default:
                    statusMatch = true;
                    break;
            }

            return dateInRange && statusMatch;
        }).collect(Collectors.toList());

        double totalRevenue = filteredBills.stream().mapToDouble(Bill::getValue).sum();
        double averageBill = filteredBills.isEmpty() ? 0 : totalRevenue / filteredBills.size();
        long paidBills = filteredBills.stream().filter(Bill::isIspaid).count();
        long unpaidBills = filteredBills.stream().filter(bill -> !bill.isIspaid()).count();

        totalRevenueLabel.setText(String.format("$%.2f", totalRevenue));
        averageBillLabel.setText(String.format("$%.2f", averageBill));
        paidBillsLabel.setText(String.valueOf(paidBills));
        unpaidBillsLabel.setText(String.valueOf(unpaidBills));
    }

    private void updateCharts() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String status = statusFilterCombo.getValue();

        List<Bill> filteredBills = billList.stream().filter(bill -> {
            Date billDate = bill.getDate();
            LocalDate billLocalDate = billDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            boolean dateInRange = (startDate == null || !billLocalDate.isBefore(startDate)) &&
                                 (endDate == null || !billLocalDate.isAfter(endDate));

            boolean statusMatch;
            switch (status) {
                case "Paid":
                    statusMatch = bill.isIspaid();
                    break;
                case "Unpaid":
                    statusMatch = !bill.isIspaid();
                    break;
                case "Overdue":
                    statusMatch = !bill.isIspaid() && bill.getDueDate() != null && bill.getDueDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now());
                    break;
                case "All Bills":
                default:
                    statusMatch = true;
                    break;
            }

            return dateInRange && statusMatch;
        }).collect(Collectors.toList());

        long paidCount = filteredBills.stream().filter(Bill::isIspaid).count();
        long unpaidCount = filteredBills.stream().filter(bill -> !bill.isIspaid()).count();
        long overdueCount = filteredBills.stream()
                .filter(bill -> !bill.isIspaid() && bill.getDueDate() != null && bill.getDueDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now()))
                .count();

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Paid", paidCount),
                new PieChart.Data("Unpaid", unpaidCount),
                new PieChart.Data("Overdue", overdueCount)
        );
        paymentStatusChart.setData(pieChartData);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        java.util.Map<String, Double> monthlyRevenue = new java.util.TreeMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy");
        for (Bill bill : filteredBills) {
            String month = monthFormat.format(bill.getDate());
            monthlyRevenue.merge(month, bill.getValue(), Double::sum);
        }

        monthlyRevenue.forEach((month, revenue) -> series.getData().add(new XYChart.Data<>(month, revenue)));

        revenueTrendChart.getData().clear();
        revenueTrendChart.getData().add(series);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}