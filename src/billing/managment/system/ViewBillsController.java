package billing.management.system;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.scene.control.Button;

public class ViewBillsController {
    @FXML private Text titleText;
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, Integer> billIdColumn;
    @FXML private TableColumn<Bill, Double> amountColumn;
    @FXML private TableColumn<Bill, Date> dateColumn;
    @FXML private TableColumn<Bill, Date> dueDateColumn;
    @FXML private Button closeButton;

    private User user;

    public void setUser(User user) {
        this.user = user;
        titleText.setText("Bills for " + user.getName());
        loadBills();
    }

    @FXML
    private void initialize() {
        billIdColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBillId()).asObject());
        amountColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getValue()).asObject());
        dateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDate()));
        dateColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatter.format(item));
            }
        });
        dueDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDueDate()));
        dueDateColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatter.format(item));
            }
        });
    }

    private void loadBills() {
        List<Bill> bills = FileManager.loadBillsForUser(user.getId());
        billsTable.setItems(FXCollections.observableArrayList(bills));
        if (bills.isEmpty()) {
            billsTable.setPlaceholder(new javafx.scene.text.Text("No bills found for this user."));
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}