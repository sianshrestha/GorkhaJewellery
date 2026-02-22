package com.gorkha.gorkhajewellery.service;

import com.gorkha.gorkhajewellery.model.Invoice;
import com.gorkha.gorkhajewellery.model.InvoiceItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    // --- BRANDING COLORS ---
    private static final Color BRAND_COLOR = new Color(128, 0, 0); // Maroon
    private static final Color SILVER_COLOR = new Color(105, 0, 0); // Dark Gray for Silver Invoice

    // --- FONTS ---
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BRAND_COLOR);
    private static final Font TITLE_FONT_SILVER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, SILVER_COLOR);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

    public void generatePdf(Invoice invoice) throws Exception {
        // 1. Separate items by Purity
        List<InvoiceItem> goldItems = invoice.getItems().stream()
                .filter(i -> !"Silver".equalsIgnoreCase(i.getPurity()))
                .collect(Collectors.toList());

        List<InvoiceItem> silverItems = invoice.getItems().stream()
                .filter(i -> "Silver".equalsIgnoreCase(i.getPurity()))
                .collect(Collectors.toList());

        boolean hasGold = !goldItems.isEmpty();
        boolean hasSilver = !silverItems.isEmpty();

        // 2. Generate PDFs
        if (hasGold) {
            // Gold gets the deductions (Old Gold, Discount, Advance) primarily
            generateCategoryPdf(invoice, goldItems, "Gold", true);
        }

        if (hasSilver) {
            // If there's no Gold, Silver gets the deductions. Otherwise, Gold already took them.
            generateCategoryPdf(invoice, silverItems, "Silver", !hasGold);
        }

        if (invoice.getOldGoldAmount() > 0) {
            generateOldGoldPdf(invoice);
        }
    }

    private void generateCategoryPdf(Invoice invoice, List<InvoiceItem> items, String category, boolean applyDeductions) throws Exception {
        // 1. Setup Folders (Invoices/Gold/ or Invoices/Silver/)
        String userHome = System.getProperty("user.home");
        String folderPath = userHome + "/Documents/GorkhaJewellery/Invoices/" + category + "/";

        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String filename = folderPath + invoice.getInvoiceNumber() + "_" + category + ".pdf";
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // --- 2. CALCULATE MATH FOR THIS SPECIFIC PDF ---
        double subTotal = items.stream().mapToDouble(InvoiceItem::getLineTotal).sum();
        double oldGold = applyDeductions ? invoice.getOldGoldAmount() : 0.0;
        double discount = applyDeductions ? invoice.getDiscountAmount() : 0.0;
        double advance = applyDeductions ? invoice.getAdvancePayment() : 0.0;
        double gstPercent = invoice.getGstPercent();

        double taxable = subTotal - oldGold;
        if (taxable < 0) taxable = 0; // Prevent negative GST if Old Gold covers everything

        double gstAmount = taxable * (gstPercent / 100.0);
        double grandTotal = taxable + gstAmount - discount;
        double balance = grandTotal - advance;

        Color themeColor = category.equals("Silver") ? SILVER_COLOR : BRAND_COLOR;
        Font titleFont = category.equals("Silver") ? TITLE_FONT_SILVER : TITLE_FONT;

        // 3. LOGO & HEADER
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image logo = Image.getInstance("logo.png");
            logo.scaleToFit(120, 80);
            logo.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(logo);
        } catch (Exception e) {
            Paragraph p = new Paragraph("GJ", titleFont);
            p.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(p);
        }
        headerTable.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph title = new Paragraph("Gurkha Gold And Silver Palace", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(title);

        Paragraph sub = new Paragraph("TRADITIONAL AND MODERN JEWELLERIES", SUBTITLE_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(sub);

        Paragraph addr = new Paragraph("Shop 12A/62-72 Queen Street, Auburn, NSW, 2144\nPhone: 0450567422 | ABN: 80615342982", SUBTITLE_FONT);
        addr.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(addr);

        // Indicate Invoice Type (Gold or Silver)
        Paragraph typeLabel = new Paragraph(category.toUpperCase() + " INVOICE", BOLD_FONT);
        typeLabel.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(typeLabel);

        headerTable.addCell(titleCell);
        document.add(headerTable);

        Paragraph line = new Paragraph("______________________________________________________________________________");
        line.setAlignment(Element.ALIGN_CENTER);
        line.getFont().setColor(Color.LIGHT_GRAY);
        document.add(line);
        document.add(new Paragraph(" "));

        // 4. CUSTOMER & INVOICE DETAILS
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(10f);

        PdfPCell leftInfo = new PdfPCell();
        leftInfo.setBorder(Rectangle.BOX);
        leftInfo.setBorderColor(Color.LIGHT_GRAY);
        leftInfo.setPadding(10);
        Paragraph customerData = new Paragraph("Invoice To: " + invoice.getCustomerName() + "\nPhone No: " + invoice.getCustomerPhone() + "\nAddress: " + invoice.getCustomerAddress(), BOLD_FONT);
        customerData.setLeading(14);
        leftInfo.addElement(customerData);
        infoTable.addCell(leftInfo);

        PdfPCell rightInfo = new PdfPCell();
        rightInfo.setBorder(Rectangle.BOX);
        rightInfo.setBorderColor(Color.LIGHT_GRAY);
        rightInfo.setPadding(10);
        Paragraph invoiceDetails = new Paragraph("Invoice No: " + invoice.getInvoiceNumber() + "\n\nDate: " + invoice.getDate().toString(), BOLD_FONT);
        invoiceDetails.setLeading(14);
        rightInfo.addElement(invoiceDetails);
        infoTable.addCell(rightInfo);

        document.add(infoTable);

        // Rates Strip (Show Silver rate on Silver invoice, Gold rates on Gold invoice)
        PdfPTable rateTable = new PdfPTable(1);
        rateTable.setWidthPercentage(100);

        // NOTE: Make sure you have added `getRateSilver()` to your Invoice.java model.
        // If it throws an error here, you need to add `private double rateSilver;` and its Getters/Setters to Invoice.java.
        String ratesText = category.equals("Gold") ?
                "Gold Rates:   $" + invoice.getRate22k() + " (22K)   |   $" + invoice.getRate24k() + " (24K)" :
                "Silver Rate:   (Applied as per item)"; // Placeholder until rateSilver is fully wired to your Invoice.java model

        PdfPCell rateCell = new PdfPCell(new Phrase(ratesText, BOLD_FONT));
        rateCell.setBackgroundColor(category.equals("Silver") ? new Color(240, 240, 240) : new Color(255, 250, 205));
        rateCell.setPadding(6);
        rateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rateCell.setBorderColor(Color.LIGHT_GRAY);
        rateTable.addCell(rateCell);
        document.add(rateTable);
        document.add(new Paragraph(" "));

        // 5. ITEMS TABLE
        float[] cols = {0.8f, 4, 1.2f, 1.5f, 1.5f, 1.5f, 1.5f, 2};
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        String[] headers = {"SN", "Particulars", "Net Weight", "Wastage", "Total Weight", "Stones", "Wages", "Total Amount"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(themeColor);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int sn = 1;
        boolean alternate = false;
        for (InvoiceItem item : items) {
            Color rowColor = alternate ? new Color(245, 245, 245) : Color.WHITE;

            addCell(table, String.valueOf(sn++), rowColor, Element.ALIGN_CENTER);
            addCell(table, item.getDescription() + " (" + item.getPurity() + ")", rowColor, Element.ALIGN_LEFT);
            addCell(table, item.getNetWeightLal() + " " + item.getWeightUnit(), rowColor, Element.ALIGN_CENTER);

            // Checks if WastageUnit is null and defaults to Lal so PDF doesn't crash
            String wUnit = (item.getWastageUnit() != null) ? item.getWastageUnit() : "Lal";
            addCell(table, item.getWastageLal() + " " + wUnit, rowColor, Element.ALIGN_CENTER);

            addCell(table, item.getDisplayTotalWeight() + " " + item.getWeightUnit(), rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getStoneCost()), rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getWages()), rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getLineTotal()), rowColor, Element.ALIGN_CENTER);

            alternate = !alternate;
        }
        document.add(table);

        // 6. FOOTER & TOTALS
        document.add(new Paragraph(" "));
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{3, 2});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        leftCell.addElement(new Paragraph("Terms & Conditions:", BOLD_FONT));
        leftCell.addElement(new Paragraph("* Jewelleries sold are not returnable.", DATA_FONT));
        leftCell.addElement(new Paragraph("* Manufacturing loss is not exchangeable.", DATA_FONT));
        leftCell.addElement(new Paragraph("\n\n"));

        Paragraph soldBy = new Paragraph("SOLD BY: " + (invoice.getSoldBy() != null ? invoice.getSoldBy().toUpperCase() : ""), BOLD_FONT);
        soldBy.setAlignment(Element.ALIGN_LEFT);
        soldBy.setIndentationLeft(40);
        leftCell.addElement(soldBy);
        leftCell.addElement(new Paragraph("\n"));

        try {
            Image signature = Image.getInstance("signature.png");
            signature.scaleToFit(100, 50);
            signature.setAlignment(Element.ALIGN_LEFT);
            signature.setIndentationLeft(40);
            leftCell.addElement(signature);
        } catch (Exception e) {
            Paragraph p = new Paragraph("(Authorized Signature)", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
            p.setAlignment(Element.ALIGN_LEFT);
            p.setIndentationLeft(50);
            leftCell.addElement(p);
        }

        footerTable.addCell(leftCell);

        // Right Side: Totals Box
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);

        addRow(totalsTable, "Subtotal:", df.format(subTotal), false);
        if (oldGold > 0) addRow(totalsTable, "Old Gold:", "-" + df.format(oldGold), false);
        if (discount > 0) addRow(totalsTable, "Discount:", "-" + df.format(discount), false);

        addRow(totalsTable, "GST (" + gstPercent + "%):", df.format(gstAmount), false);

        PdfPCell labelCell = new PdfPCell(new Phrase("GRAND TOTAL:", BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setPadding(6);
        PdfPCell valueCell = new PdfPCell(new Phrase("$" + df.format(grandTotal), BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totalsTable.addCell(labelCell);
        totalsTable.addCell(valueCell);

        if (advance > 0) {
            addRow(totalsTable, "Paid:", df.format(advance), false);
            addRow(totalsTable, "Balance Due:", df.format(balance), true);
        }

        PdfPCell rightCell = new PdfPCell(totalsTable);
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setBorderColor(themeColor); // Grey border for silver, maroon for gold
        rightCell.setPadding(10);
        footerTable.addCell(rightCell);

        document.add(footerTable);
        document.close();

        openPdfSafely(filename);

        // Auto Open the PDF
        try {
            Runtime.getRuntime().exec("open " + filename);
        } catch (Exception e) {
        }
        try {
            Runtime.getRuntime().exec("explorer.exe " + filename);
        } catch (Exception e) {
        }
    }

    // --- Helper Methods ---
    private void addCell(PdfPTable table, String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, DATA_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, String value, boolean isRed) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, DATA_FONT));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(4);

        Font valFont = isRed ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED) : DATA_FONT;
        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(4);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
    }


    // --- Helper Methods ---

    private void generateOldGoldPdf(Invoice invoice) {
        try {
            String userHome = System.getProperty("user.home");
            String folderPath = userHome + "/Documents/GorkhaJewellery/OldGoldInvoices/";
            java.io.File directory = new java.io.File(folderPath);
            if (!directory.exists()) directory.mkdirs();

            String filename = folderPath + "OldGold_" + invoice.getInvoiceNumber() + ".pdf";
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            // 1. LOGO & HEADER (Same as Main Invoice)
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            try {
                Image logo = Image.getInstance("logo.png");
                logo.scaleToFit(120, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logo);
            } catch (Exception e) {
            }
            headerTable.addCell(logoCell);

            // Shop Details Cell
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph title = new Paragraph("Gurkha Gold And Silver Palace", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(title);

            Paragraph blankSpace = new Paragraph("\n");
            titleCell.addElement(blankSpace);

            Paragraph addr = new Paragraph("Shop 12A/62-72 Queen Street, Auburn, NSW, 2144\nPhone: 0450567422 | ABN: 80615342982", SUBTITLE_FONT);
            addr.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(addr);

            Paragraph oldGold = new Paragraph("\nOLD GOLD / EXCHANGE RECEIPT", BOLD_FONT);
            oldGold.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(oldGold);

            headerTable.addCell(titleCell);
            document.add(headerTable);

            Paragraph line = new Paragraph("______________________________________________________________________________");
            line.setAlignment(Element.ALIGN_CENTER);
            line.getFont().setColor(Color.LIGHT_GRAY);
            document.add(line);
            document.add(new Paragraph(" "));

            // 2. CUSTOMER & INVOICE DETAILS BOX
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(10f);

            PdfPCell leftInfo = new PdfPCell();
            leftInfo.setBorder(Rectangle.BOX);
            leftInfo.setBorderColor(Color.LIGHT_GRAY);
            leftInfo.setPadding(10);

            Paragraph customerData = new Paragraph("Invoice To: " + invoice.getCustomerName() + "\nPhone: " + invoice.getCustomerPhone() + "\nAddress: " + invoice.getCustomerAddress(), BOLD_FONT);
            customerData.setLeading(14);
            leftInfo.addElement(customerData);


            PdfPCell rightInfo = new PdfPCell();
            rightInfo.setBorder(Rectangle.BOX);
            rightInfo.setBorderColor(Color.LIGHT_GRAY);
            rightInfo.setPadding(10);

            Paragraph rightData = new Paragraph(new Phrase("Ref Invoice No: " + invoice.getInvoiceNumber() + "\nDate: " + invoice.getDate().toString(), BOLD_FONT));
            rightData.setLeading(14);
            rightInfo.addElement(rightData);

            infoTable.addCell(leftInfo);
            infoTable.addCell(rightInfo);
            document.add(infoTable);

            // 3. ITEMS TABLE
            float[] cols = {0.8f, 4, 1.5f, 1.5f, 1.5f, 1.5f, 2};
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);

            // Headers
            String[] headers = {"SN", "Description", "Purity", "Gross Weight", "Purity Loss", "Net Weight", "Amount ($)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
                cell.setBackgroundColor(BRAND_COLOR);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data Rows
            int sn = 1;
            boolean alternate = false;
            for (com.gorkha.gorkhajewellery.model.OldGoldItem item : invoice.getOldGoldItems()) {
                Color rowColor = alternate ? new Color(245, 245, 245) : Color.WHITE;
                addCell(table, String.valueOf(sn++), rowColor, Element.ALIGN_CENTER);
                addCell(table, item.getDescription(), rowColor, Element.ALIGN_LEFT);
                addCell(table, item.getPurity(), rowColor, Element.ALIGN_CENTER);
                addCell(table, df.format(item.getGrossWeight()), rowColor, Element.ALIGN_CENTER);
                addCell(table, df.format(item.getPurityLoss()), rowColor, Element.ALIGN_CENTER);
                addCell(table, df.format(item.getNetWeight()), rowColor, Element.ALIGN_CENTER);
                addCell(table, df.format(item.getAmount()), rowColor, Element.ALIGN_CENTER);
                alternate = !alternate;
            }
            document.add(table);

            // 4. FOOTER & TOTAL
            document.add(new Paragraph(" "));
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{3, 2});

            PdfPCell sigCell = new PdfPCell(new Phrase("\n\n\nCustomer Signature: __________________\n\nAuthorized Signature: __________________", DATA_FONT));
            sigCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(sigCell);

            PdfPTable totalBox = new PdfPTable(2);
            PdfPCell lbl = new PdfPCell(new Phrase("TOTAL AMOUNT:", BOLD_FONT));
            lbl.setBorder(Rectangle.NO_BORDER);
            lbl.setPadding(6);
            PdfPCell val = new PdfPCell(new Phrase("$" + df.format(invoice.getOldGoldAmount()), BOLD_FONT));
            val.setBorder(Rectangle.NO_BORDER);
            val.setPadding(6);
            val.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalBox.addCell(lbl);
            totalBox.addCell(val);

            PdfPCell totalCellContainer = new PdfPCell(totalBox);
            totalCellContainer.setBorder(Rectangle.BOX);
            totalCellContainer.setBorderColor(BRAND_COLOR);
            footerTable.addCell(totalCellContainer);

            document.add(footerTable);
            document.close();

            openPdfSafely(filename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This method safely opens files on Mac/Windows even if there are spaces in the file path
    private void openPdfSafely(String filename) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", filename});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", filename});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", filename});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}