package com.gorkha.gorkhajewellery.ui;


import com.gorkha.gorkhajewellery.model.Invoice;
import com.gorkha.gorkhajewellery.model.InvoiceItem;
import com.gorkha.gorkhajewellery.repository.InvoiceRepository;
import com.gorkha.gorkhajewellery.service.PdfService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.prefs.Preferences;

@Component
public class InvoiceController {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PdfService pdfService;

    // --- Inputs ---
    @FXML private TextField customerNameField, phoneField, customerAddressField, soldByField;
    @FXML private TextField rate22kField, rate24kField;
    @FXML private TextField oldGoldField, discountField, gstField, advanceField;
    @FXML private Label subTotalLabel, grandTotalLabel, balanceLabel;

    // --- Table ---
    @FXML private TableView<InvoiceItem> itemTable;
    @FXML private TableColumn<InvoiceItem, String> descCol, purityCol,unitCol;
    @FXML private TableColumn<InvoiceItem, Double> netWtCol, wastageCol, totalWtCol, stoneCol, wagesCol, totalCol;

    private ObservableList<InvoiceItem> items = FXCollections.observableArrayList();
    private Preferences prefs = Preferences.userNodeForPackage(InvoiceController.class);

    @FXML
    public void initialize() {
        itemTable.setEditable(true);

        rate22kField.setText(prefs.get("rate22k", "1340"));
        rate24kField.setText(prefs.get("rate24k", "1430"));

        // 1. Text Columns (Uses Custom EditCell for String)
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(col -> new EditCell<>(new DefaultStringConverter()));
        descCol.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        purityCol.setCellValueFactory(new PropertyValueFactory<>("purity"));
        purityCol.setCellFactory(ComboBoxTableCell.forTableColumn("22K", "24K"));
        purityCol.setOnEditCommit(e -> { e.getRowValue().setPurity(e.getNewValue()); recalculateAll(); });

        unitCol.setCellValueFactory(new PropertyValueFactory<>("weightUnit"));
        unitCol.setCellFactory(ComboBoxTableCell.forTableColumn("Lal", "Tola"));
        unitCol.setOnEditCommit(e -> { e.getRowValue().setWeightUnit(e.getNewValue()); recalculateAll(); });

        // 2. Number Columns (Uses Custom EditCell for Double)
        netWtCol.setCellValueFactory(new PropertyValueFactory<>("netWeightLal"));
        setupDoubleCol(netWtCol, (item, v) -> item.setNetWeightLal(v));

        wastageCol.setCellValueFactory(new PropertyValueFactory<>("wastageLal"));
        setupDoubleCol(wastageCol, (item, v) -> item.setWastageLal(v));

        stoneCol.setCellValueFactory(new PropertyValueFactory<>("stoneCost"));
        setupDoubleCol(stoneCol, (item, v) -> item.setStoneCost(v));

        wagesCol.setCellValueFactory(new PropertyValueFactory<>("wages"));
        setupDoubleCol(wagesCol, (item, v) -> item.setWages(v));

        totalWtCol.setCellValueFactory(new PropertyValueFactory<>("displayTotalWeight"));
        totalWtCol.setCellFactory(tc -> new TableCell<InvoiceItem, Double>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e);
                setText((e || v == null) ? null : String.format("%.2f", v));
            }
        });

        // Total Column (Calculated & Formatted)
        totalCol.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        // Format to 2 decimal places
        totalCol.setCellFactory(tc -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", value));
                }
            }
        });

        // 3. Footer Listeners
        addListeners(oldGoldField, discountField, gstField, advanceField, rate22kField, rate24kField);
        onNewInvoice();
    }

    private void setupDoubleCol(TableColumn<InvoiceItem, Double> col, BiConsumer<InvoiceItem, Double> setter) {
        // Use the new EditCell instead of standard TextFieldTableCell
        col.setCellFactory(c -> new EditCell<>(new DoubleStringConverter()));
        col.setOnEditCommit(e -> {
            setter.accept(e.getRowValue(), e.getNewValue());
            recalculateAll();
        });
    }

    private void addListeners(TextField... fields) {
        for (TextField f : fields) { f.textProperty().addListener((obs, old, nev) -> recalculateAll()); }
    }

    @FXML
    public void onNewInvoice() {
        customerNameField.clear(); phoneField.clear(); customerAddressField.clear();
        oldGoldField.setText("0"); discountField.setText("0"); gstField.setText("0"); advanceField.setText("0");
        items.clear();
        items.add(new InvoiceItem());
        itemTable.setItems(items);
        recalculateAll();
    }

    @FXML
    public void onViewHistory() {
        Stage historyStage = new Stage();
        historyStage.setTitle("Sales History");

        TableView<Invoice> historyTable = new TableView<>();
        ObservableList<Invoice> historyData = FXCollections.observableArrayList(invoiceRepository.findAll());

        TableColumn<Invoice, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate().toString()));

        TableColumn<Invoice, String> invCol = new TableColumn<>("Invoice No");
        invCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInvoiceNumber()));

        TableColumn<Invoice, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomerName()));

        TableColumn<Invoice, String> totalCol = new TableColumn<>("Total ($)");
        totalCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f", cell.getValue().getGrandTotal())));

        historyTable.getColumns().addAll(dateCol, invCol, custCol, totalCol);
        historyTable.setItems(historyData);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        historyTable.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    try { pdfService.generatePdf(row.getItem()); } catch (Exception e) {}
                }
            });
            return row;
        });

        VBox layout = new VBox(new Label("Double-click a row to open PDF"), historyTable);
        historyStage.setScene(new Scene(layout, 600, 400));
        historyStage.show();
    }

    @FXML
    public void onSaveAndPrint() {
        try {
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("GJ-" + System.currentTimeMillis());
            invoice.setCustomerName(customerNameField.getText());
            invoice.setCustomerPhone(phoneField.getText());
            invoice.setCustomerAddress(customerAddressField.getText());
            invoice.setSoldBy(soldByField.getText());
            invoice.setDate(LocalDate.now());

            invoice.setRate22k(parse(rate22kField.getText()));
            invoice.setRate24k(parse(rate24kField.getText()));
            invoice.setOldGoldAmount(parse(oldGoldField.getText()));
            invoice.setDiscountAmount(parse(discountField.getText()));
            invoice.setGstPercent(parse(gstField.getText()));
            invoice.setAdvancePayment(parse(advanceField.getText()));

            recalculateAll();
            invoice.setSubTotal(parse(subTotalLabel.getText()));
            invoice.setGrandTotal(parse(grandTotalLabel.getText()));
            invoice.setBalanceDue(parse(balanceLabel.getText()));

            for(InvoiceItem i : items) {
                if(i.getDescription() != null && !i.getDescription().isEmpty()) invoice.addItem(i);
            }

            invoiceRepository.save(invoice);
            pdfService.generatePdf(invoice);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Saved! Start new invoice?");
            alert.showAndWait();
            onNewInvoice();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void recalculateAll() {
        try {
            double r22 = parse(rate22kField.getText());
            double r24 = parse(rate24kField.getText());
            prefs.put("rate22k", rate22kField.getText());
            prefs.put("rate24k", rate24kField.getText());

            double subTotal = 0;
            for (InvoiceItem item : items) {
                if (item.getPurity() == null) item.setPurity("22K");
                if (item.getWeightUnit() == null) item.setWeightUnit("Lal");
                item.calculateLineTotal(r22, r24);
                subTotal += item.getLineTotal();
            }
            itemTable.refresh();

            double oldGold = parse(oldGoldField.getText());
            double discount = parse(discountField.getText());
            double gstPercent = parse(gstField.getText());
            double advance = parse(advanceField.getText());

            double taxable = subTotal - oldGold;
            double gstAmount = taxable * (gstPercent / 100.0);
            double grandTotal = taxable + gstAmount - discount;
            double balance = grandTotal - advance;

            subTotalLabel.setText(format(subTotal));
            grandTotalLabel.setText(format(grandTotal));
            balanceLabel.setText(format(balance));
        } catch (Exception ignored) {}
    }

    @FXML public void addEmptyRow() { items.add(new InvoiceItem()); }
    private double parse(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
    private String format(double d) { return String.format("%.2f", d); }
    interface BiConsumer<T, U> { void accept(T t, U u); }

    /**
     * CUSTOM CELL CLASS: Commits edits on Focus Loss (Tab/Click away)
     */
    public static class EditCell<S, T> extends TableCell<S, T> {
        private final StringConverter<T> converter;
        private TextField textField;

        public EditCell(StringConverter<T> converter) {
            this.converter = converter;

            // SINGLE CLICK EDIT: Use RunLater to ensure focus processing order
            this.setOnMouseClicked(e -> {
                if(!isEmpty() && !isEditing()) {
                    // Slight delay ensures any PREVIOUS edit has time to save first
                    Platform.runLater(() -> getTableView().edit(getIndex(), getTableColumn()));
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                // FORCE FOCUS & CURSOR
                Platform.runLater(() -> {
                    if (textField != null) {
                        textField.requestFocus();
                        textField.positionCaret(textField.getText().length());
                    }
                });
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(converter.toString(getItem()));
            setGraphic(null);
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) textField.setText(converter.toString(getItem()));
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(converter.toString(getItem()));
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(converter.toString(getItem()));
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

            // 1. Commit on ENTER
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(converter.fromString(textField.getText()));
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

            // 2. Commit on FOCUS LOST (Click away or Tab)
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) { // Focus lost
                    try {
                        commitEdit(converter.fromString(textField.getText()));
                    } catch (Exception e) {
                        // If input is invalid (e.g., text in a number field), just cancel
                        cancelEdit();
                    }
                }
            });
        }
    }
}