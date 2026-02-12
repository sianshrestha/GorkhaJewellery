package com.gorkha.gorkhajewellery.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber; // e.g., "INV-1001"
    private LocalDate date;

    // --- Customer Details (Snapshot) ---
    // We save these directly here so if the customer changes their address later,
    // this old invoice remains historically accurate.
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    // Updated Financials
    private double rate22k;
    private double rate24k;

    private double subTotal;       // Sum of items
    private double oldGoldAmount;  // Trade-in (Less)
    private double gstPercent;     // e.g. 10%
    private double discountAmount; // (Less)
    private double grandTotal;     // Final to pay

    private double advancePayment; // Paid today
    private double balanceDue;     // Remaining

    private String soldBy;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    private List<InvoiceItem> items = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        this.items.add(item);
    }
}
