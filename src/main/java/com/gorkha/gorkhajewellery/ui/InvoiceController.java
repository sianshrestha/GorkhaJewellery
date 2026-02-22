package com.gorkha.gorkhajewellery.ui;

import com.gorkha.gorkhajewellery.model.Invoice;
import com.gorkha.gorkhajewellery.model.InvoiceItem;
import com.gorkha.gorkhajewellery.model.OldGoldItem;
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
import javafx.scene.layout.GridPane;
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
    @FXML private TextField rate22kField, rate24kField, rateSilverField;
    @FXML private TextField oldGoldField, discountField, gstField, advanceField;
    @FXML private Label subTotalLabel, grandTotalLabel, balanceLabel;

    // --- Table ---
    @FXML private TableView<InvoiceItem> itemTable;
    @FXML private TableColumn<InvoiceItem, String> descCol, purityCol, unitCol, wastageUnitCol;
    @FXML private TableColumn<InvoiceItem, Double> netWtCol, wastageCol, totalWtCol, stoneCol, wagesCol, totalCol;

    private ObservableList<InvoiceItem> items = FXCollections.observableArrayList();
    private ObservableList<OldGoldItem> ogItems = FXCollections.observableArrayList();

    private Preferences prefs = Preferences.userNodeForPackage(InvoiceController.class);

    // Temporary variables to hold Old Gold data
    private String ogDesc = "";
    private String ogWeight = "";
    private String ogPurity = "";

    @FXML
    public void initialize() {
        itemTable.setEditable(true);

        rate22kField.setText(prefs.get("rate22k", "1340"));
        rate24kField.setText(prefs.get("rate24k", "1430"));
        rateSilverField.setText(prefs.get("rateSilver", "150"));

        descCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.25));
        purityCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.08));
        unitCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.08));
        netWtCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));
        wastageUnitCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.08));
        wastageCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));
        totalWtCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));
        stoneCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));
        wagesCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));
        totalCol.prefWidthProperty().bind(itemTable.widthProperty().multiply(0.12));

        // 1. Text Columns (Uses Custom EditCell)
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(col -> new EditCell<>(new DefaultStringConverter()));
        descCol.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        purityCol.setCellValueFactory(new PropertyValueFactory<>("purity"));
        purityCol.setCellFactory(ComboBoxTableCell.forTableColumn("22K", "24K", "Silver"));
        purityCol.setOnEditCommit(e -> { e.getRowValue().setPurity(e.getNewValue()); recalculateAll(); });

        unitCol.setCellValueFactory(new PropertyValueFactory<>("weightUnit"));
        unitCol.setCellFactory(ComboBoxTableCell.forTableColumn("Lal", "Tola"));
        unitCol.setOnEditCommit(e -> { e.getRowValue().setWeightUnit(e.getNewValue()); recalculateAll(); });

        // 2. Number Columns (Uses Custom EditCell)
        netWtCol.setCellValueFactory(new PropertyValueFactory<>("netWeightLal"));
        setupDoubleCol(netWtCol, (item, v) -> item.setNetWeightLal(v));

        // Wastage UNIT (Single Click Dropdown)
        wastageUnitCol.setCellValueFactory(new PropertyValueFactory<>("wastageUnit"));
        wastageUnitCol.setCellFactory(ComboBoxTableCell.forTableColumn("Lal", "Tola"));
        wastageUnitCol.setOnEditCommit(e -> { e.getRowValue().setWastageUnit(e.getNewValue()); recalculateAll(); });

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

        // Total Column
        totalCol.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        totalCol.setCellFactory(tc -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText((empty || value == null) ? null : String.format("%.2f", value));
            }
        });

        addListeners(oldGoldField, discountField, gstField, advanceField, rate22kField, rate24kField, rateSilverField);
        onNewInvoice();
    }

    private void setupDoubleCol(TableColumn<InvoiceItem, Double> col, BiConsumer<InvoiceItem, Double> setter) {
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
        ogItems.clear(); // Clear old gold list for new invoice
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
    public void openOldGoldPopup() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Old Gold System");
        dialog.setHeaderText("Add Old Gold / Exchange Items");

        ButtonType applyButtonType = new ButtonType("Apply & Calculate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        VBox layout = new VBox(10);
        layout.setPrefWidth(800);
        layout.setPrefHeight(400);

        TableView<OldGoldItem> ogTable = new TableView<>(ogItems);
        ogTable.setEditable(true);
        ogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Columns
        TableColumn<OldGoldItem, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(c -> new EditCell<>(new DefaultStringConverter()));
        descCol.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        TableColumn<OldGoldItem, String> purityCol = new TableColumn<>("Purity");
        purityCol.setCellValueFactory(new PropertyValueFactory<>("purity"));
        purityCol.setCellFactory(c -> new EditCell<>(new DefaultStringConverter()));
        purityCol.setOnEditCommit(e -> e.getRowValue().setPurity(e.getNewValue()));

        TableColumn<OldGoldItem, Double> grossCol = new TableColumn<>("Gross Wt");
        grossCol.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        setupOgDoubleCol(grossCol, (item, val) -> item.setGrossWeight(val));

        TableColumn<OldGoldItem, Double> lossCol = new TableColumn<>("Purity Loss");
        lossCol.setCellValueFactory(new PropertyValueFactory<>("purityLoss"));
        setupOgDoubleCol(lossCol, (item, val) -> item.setPurityLoss(val));

        TableColumn<OldGoldItem, Double> netCol = new TableColumn<>("Net Wt");
        netCol.setCellValueFactory(new PropertyValueFactory<>("netWeight"));
        setupOgDoubleCol(netCol, (item, val) -> item.setNetWeight(val));

        TableColumn<OldGoldItem, Double> amountCol = new TableColumn<>("Amount ($)");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        setupOgDoubleCol(amountCol, (item, val) -> item.setAmount(val));

        ogTable.getColumns().addAll(descCol, purityCol, grossCol, lossCol, netCol, amountCol);

        Button btnAddRow = new Button("+ Add Row");
        btnAddRow.setOnAction(e -> ogItems.add(new OldGoldItem()));

        // Add 1 default row if empty
        if(ogItems.isEmpty()) ogItems.add(new OldGoldItem());

        layout.getChildren().addAll(ogTable, btnAddRow);
        dialog.getDialogPane().setContent(layout);

        dialog.setResultConverter(btn -> {
            if (btn == applyButtonType) {
                // Sum all amounts manually entered by client
                double totalOg = ogItems.stream().mapToDouble(OldGoldItem::getAmount).sum();
                oldGoldField.setText(format(totalOg));
                recalculateAll();
            }
            return btn;
        });

        dialog.showAndWait();
    }

    // Helper method specifically for the Old Gold Table
    private void setupOgDoubleCol(TableColumn<OldGoldItem, Double> col, BiConsumer<OldGoldItem, Double> setter) {
        col.setCellFactory(c -> new EditCell<>(new DoubleStringConverter()));
        col.setOnEditCommit(e -> { setter.accept(e.getRowValue(), e.getNewValue()); });
    }

    @FXML
    public void onSaveAndPrint() {
        try {
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("GJ-" + System.currentTimeMillis());
            invoice.setCustomerName(customerNameField.getText());
            invoice.setCustomerPhone(phoneField.getText());
            invoice.setCustomerAddress(customerAddressField.getText());
            invoice.setDate(LocalDate.now());

            invoice.setRate22k(parse(rate22kField.getText()));
            invoice.setRate24k(parse(rate24kField.getText()));
            invoice.setRateSilver(parse(rateSilverField.getText()));
            invoice.setOldGoldAmount(parse(oldGoldField.getText()));
            invoice.setDiscountAmount(parse(discountField.getText()));
            invoice.setGstPercent(parse(gstField.getText()));
            invoice.setAdvancePayment(parse(advanceField.getText()));

            recalculateAll();
            invoice.setSubTotal(parse(subTotalLabel.getText()));
            invoice.setGrandTotal(parse(grandTotalLabel.getText()));
            invoice.setBalanceDue(parse(balanceLabel.getText()));

            for(InvoiceItem i : items) {
                if(i.getDescription() != null && !i.getDescription().isEmpty()) {
                    i.setId(null); // <--- THIS IS THE FIX. It detaches the item from the previous save.
                    invoice.addItem(i);
                }
            }

            // Save valid old gold items to the invoice
            for(OldGoldItem ogi : ogItems) {
                if(ogi.getDescription() != null && !ogi.getDescription().isEmpty()) {
                    ogi.setId(null);
                    invoice.addOldGoldItem(ogi);
                }
            }

            invoiceRepository.save(invoice);
            pdfService.generatePdf(invoice);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Invoice saved and PDF generated successfully!", ButtonType.OK);
            alert.showAndWait();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void recalculateAll() {
        try {
            double r22 = parse(rate22kField.getText());
            double r24 = parse(rate24kField.getText());
            double rSilver = parse(rateSilverField.getText());

            prefs.put("rate22k", rate22kField.getText());
            prefs.put("rate24k", rate24kField.getText());
            prefs.put("rateSilver", rateSilverField.getText());

            double subTotal = 0;
            for (InvoiceItem item : items) {
                if (item.getPurity() == null) item.setPurity("22K");
                if (item.getWeightUnit() == null) item.setWeightUnit("Lal");
                if (item.getWastageUnit() == null) item.setWastageUnit("Lal");

                item.calculateLineTotal(r22, r24, rSilver);
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
     * CUSTOM CELL CLASS: Handles Saving on Row Switch
     */
    public static class EditCell<S, T> extends TableCell<S, T> {
        private final StringConverter<T> converter;
        private TextField textField;
        private boolean escapePressed = false; // Flag to track real cancellations

        public EditCell(StringConverter<T> converter) {
            this.converter = converter;

            // Allow single click edit
            this.setOnMouseClicked(e -> {
                if(!isEmpty() && !isEditing()) {
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
                escapePressed = false; // Reset flag
                Platform.runLater(() -> {
                    if (textField != null) {
                        textField.requestFocus();
                        textField.positionCaret(textField.getText().length());
                    }
                });
            }
        }

        /**
         * CRITICAL FIX: This method is called when row selection changes.
         * We intercept it to SAVE the data instead of cancelling it.
         */
        @Override
        public void cancelEdit() {
            // Only truly cancel if the user hit ESCAPE
            if (escapePressed) {
                super.cancelEdit();
                setText(converter.toString(getItem()));
                setGraphic(null);
            } else {
                // Otherwise (click away, row change), try to COMMIT
                if (textField != null) {
                    try {
                        commitEdit(converter.fromString(textField.getText()));
                    } catch (Exception e) {
                        // If invalid input, then we cancel
                        super.cancelEdit();
                        setText(converter.toString(getItem()));
                        setGraphic(null);
                    }
                } else {
                    super.cancelEdit();
                }
            }
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

            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(converter.fromString(textField.getText()));
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    escapePressed = true; // Mark as explicit cancel
                    cancelEdit();
                }
            });

            // Backup focus listener
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && textField != null) {
                    try {
                        commitEdit(converter.fromString(textField.getText()));
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });
        }
    }
}