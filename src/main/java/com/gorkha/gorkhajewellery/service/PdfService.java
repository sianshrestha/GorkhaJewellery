package com.gorkha.gorkhajewellery.service;

import com.gorkha.gorkhajewellery.model.Invoice;
import com.gorkha.gorkhajewellery.model.InvoiceItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

@Service
public class PdfService {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    // --- BRANDING COLORS ---
    private static final Color BRAND_COLOR = new Color(128, 0, 0); // Maroon
    private static final Color HEADER_BG = new Color(230, 230, 230); // Light Gray
    private static final Color ACCENT_COLOR = new Color(255, 215, 0); // Gold-ish (optional use)

    // --- FONTS ---
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BRAND_COLOR);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

    public void generatePdf(Invoice invoice) throws Exception {
        // 1. Get the User's Documents Folder
        String userHome = System.getProperty("user.home");
        String folderPath = userHome + "/Documents/GorkhaJewellery/Invoices/";

        // 2. Create the folder if it doesn't exist
        java.io.File directory = new java.io.File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 3. Save file there
        String filename = folderPath + "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // 1. LOGO & HEADER
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        // Logo Cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image logo = Image.getInstance("logo.png"); // Make sure logo.png is in your project folder
            logo.scaleToFit(120, 80);
            logo.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(logo);
        } catch (Exception e) {
            Paragraph p = new Paragraph("GJ", TITLE_FONT);
            p.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(p);
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

        Paragraph sub = new Paragraph("TRADITIONAL AND MODERN JEWELLERIES", SUBTITLE_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(sub);

        Paragraph addr = new Paragraph("Shop 12A/62-72 Queen Street, Auburn, NSW, 2144\nPhone: 0450567422 | ABN: 80615342982", SUBTITLE_FONT);
        addr.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(addr);

        headerTable.addCell(titleCell);
        document.add(headerTable);

        // Horizontal Line
        Paragraph line = new Paragraph("______________________________________________________________________________");
        line.setAlignment(Element.ALIGN_CENTER);
        line.getFont().setColor(Color.LIGHT_GRAY);
        document.add(line);
        document.add(new Paragraph(" ")); // Spacer

        // 2. CUSTOMER & INVOICE DETAILS BOX
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(10f);

        // Left Cell (Customer)
        PdfPCell leftInfo = new PdfPCell();
        leftInfo.setBorder(Rectangle.BOX);
        leftInfo.setBorderColor(Color.LIGHT_GRAY);
        leftInfo.setPadding(10);

        Paragraph customerData = new Paragraph("Invoice To: " + invoice.getCustomerName() + "\nPhone No: " + invoice.getCustomerPhone() + "\nAddress: " + invoice.getCustomerAddress(), BOLD_FONT);
        customerData.setLeading(14); // Increases vertical space between lines
        leftInfo.addElement(customerData);
        infoTable.addCell(leftInfo);

        // Right Cell (Invoice Data)
        PdfPCell rightInfo = new PdfPCell();
        rightInfo.setBorder(Rectangle.BOX);
        rightInfo.setBorderColor(Color.LIGHT_GRAY);
        rightInfo.setPadding(10);

        // Use a mini table inside to align Label and Value nicely
        Paragraph invoiceDetails = new Paragraph("Invoice No: " + invoice.getInvoiceNumber() + "\n\nDate: " + invoice.getDate().toString(), BOLD_FONT);
        invoiceDetails.setLeading(14); // Increases vertical space between lines
        rightInfo.addElement(invoiceDetails);
        infoTable.addCell(rightInfo);

        document.add(infoTable);

        // Gold Rates Strip
        PdfPTable rateTable = new PdfPTable(1);
        rateTable.setWidthPercentage(100);
        PdfPCell rateCell = new PdfPCell(new Phrase("Gold Rates:   $" + invoice.getRate22k() + " (22K)   |   $" + invoice.getRate24k() + " (24K)", BOLD_FONT));
        rateCell.setBackgroundColor(new Color(255, 250, 205)); // Light Yellow
        rateCell.setPadding(6);
        rateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rateCell.setBorderColor(Color.LIGHT_GRAY);
        rateTable.addCell(rateCell);
        document.add(rateTable);
        document.add(new Paragraph(" "));

        // 3. ITEMS TABLE (The "Pretty" Grid)
        float[] cols = {0.8f, 4, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2};
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        // Headers
        String[] headers = {"SN", "Particulars", "Net Weight", "Wastage","Total Weight", "Stones", "Wages", "Total Amount"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(BRAND_COLOR); // Maroon Header
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data Rows
        int sn = 1;
        boolean alternate = false;
        for (InvoiceItem item : invoice.getItems()) {
            Color rowColor = alternate ? new Color(245, 245, 245) : Color.WHITE; // Zebra Striping

            addCell(table, String.valueOf(sn++), rowColor, Element.ALIGN_CENTER);
            addCell(table, item.getDescription() + " (" + item.getPurity() + ")", rowColor, Element.ALIGN_LEFT);
            addCell(table, String.valueOf(item.getNetWeightLal()) + " Lal", rowColor, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(item.getWastageLal()) + " Lal", rowColor, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(item.getTotalWeightLal()) + " Lal", rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getStoneCost()), rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getWages()), rowColor, Element.ALIGN_CENTER);
            addCell(table, df.format(item.getLineTotal()), rowColor, Element.ALIGN_CENTER);

            alternate = !alternate;
        }
        document.add(table);

        // 4. FOOTER & TOTALS
        document.add(new Paragraph(" "));
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{3, 2});

        // Left Side: Notes & Signature
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        leftCell.addElement(new Paragraph("Terms & Conditions:", BOLD_FONT));
        leftCell.addElement(new Paragraph("* Jewelleries sold are not returnable.", DATA_FONT));
        leftCell.addElement(new Paragraph("* Manufacturing loss is not exchangeable.", DATA_FONT));
        leftCell.addElement(new Paragraph("\n\n")); // Space for signature

        // Signature Image
        Paragraph soldBy = new Paragraph("SOLD BY: " + (invoice.getSoldBy() != null ? invoice.getSoldBy().toUpperCase() : ""), BOLD_FONT);
        soldBy.setAlignment(Element.ALIGN_LEFT);
        soldBy.setIndentationLeft(40);
        leftCell.addElement(soldBy);

        leftCell.addElement(blankSpace);

        try {
            Image signature = Image.getInstance("signature.png");
            signature.scaleToFit(100, 50);
            signature.setAlignment(Element.ALIGN_LEFT);
            signature.setIndentationLeft(40); // Center the image itself
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

        addRow(totalsTable, "Subtotal:", df.format(invoice.getSubTotal()), false);
        if (invoice.getOldGoldAmount() > 0) addRow(totalsTable, "Less Old Gold:", "-" + df.format(invoice.getOldGoldAmount()), false);
        if (invoice.getDiscountAmount() > 0) addRow(totalsTable, "Discount:", "-" + df.format(invoice.getDiscountAmount()), false);

        addRow(totalsTable, "GST (" + invoice.getGstPercent() + "%):", df.format((invoice.getSubTotal() - invoice.getOldGoldAmount()) * (invoice.getGstPercent() / 100)), false);

        // Grand Total (Bold & Background)
        PdfPCell labelCell = new PdfPCell(new Phrase("GRAND TOTAL:", BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP); labelCell.setPadding(6);
        PdfPCell valueCell = new PdfPCell(new Phrase("$" + df.format(invoice.getGrandTotal()), BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP); valueCell.setPadding(6); valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totalsTable.addCell(labelCell);
        totalsTable.addCell(valueCell);

        if (invoice.getAdvancePayment() > 0) {
            addRow(totalsTable, "Paid:", df.format(invoice.getAdvancePayment()), false);
            addRow(totalsTable, "Balance Due:", df.format(invoice.getBalanceDue()), true); // Red Color for Due
        }

        PdfPCell rightCell = new PdfPCell(totalsTable);
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setBorderColor(BRAND_COLOR);
        rightCell.setPadding(10);
        footerTable.addCell(rightCell);

        document.add(footerTable);
        document.close();

        // Auto Open
        try { Runtime.getRuntime().exec("open " + filename); } catch (Exception e) {}
        try { Runtime.getRuntime().exec("explorer.exe " + filename); } catch (Exception e) {}
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
        c1.setBorder(Rectangle.NO_BORDER); c1.setPadding(4);

        Font valFont = isRed ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED) : DATA_FONT;
        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER); c2.setPadding(4); c2.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
    }
}