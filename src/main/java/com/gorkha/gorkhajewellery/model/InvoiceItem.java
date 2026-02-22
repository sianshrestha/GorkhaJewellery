package com.gorkha.gorkhajewellery.model;

import jakarta.persistence.*;
import lombok.Data; // <--- This generates Getters/Setters automatically

@Entity
@Data // <--- If this is missing, the table becomes Read-Only!
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String purity;
    private String weightUnit = "Lal"; // "Lal" or "Tola"
    private String wastageUnit = "Lal"; // "Lal" or "Tola"

    private double netWeightLal;   // The core value
    private double wastageLal;
    private double wages;
    private double stoneCost;

    // --- Calculated Fields ---
    private double totalWeightLal;
    private double totalWeightTola;

    // This field allows the "Total Weight" column to show data
    private double displayTotalWeight;

    private double lineTotal;

    /**
     * Calculates totals based on the current unit (Lal/Tola).
     */
    // --- UPDATED LOGIC FOR SILVER ---
    public void calculateLineTotal(double rate22k, double rate24k, double rateSilver) {
        // 1. Convert Input to Lal
        double actualWeightInLal = this.netWeightLal;
        if ("Tola".equals(this.weightUnit)) {
            actualWeightInLal = this.netWeightLal * 100.0;
        }

        // 2. Convert Wastage to Lal
        double actualWastageInLal = this.wastageLal;
        if ("Tola".equals(this.wastageUnit)) {
            actualWastageInLal = this.wastageLal * 100.0;
        }

        // 2. Calc Total Weight in Lal
        this.totalWeightLal = actualWeightInLal + actualWastageInLal;

        // 3. Set Display Weight (What user sees in "Total Weight" column)
        if ("Tola".equals(this.weightUnit)) {
            this.displayTotalWeight = this.totalWeightLal / 100.0;
        } else {
            this.displayTotalWeight = this.totalWeightLal;
        }

        // 4. Cost Calculation (Check for Silver)
        this.totalWeightTola = this.totalWeightLal / 100.0;

        double selectedRate;
        if ("Silver".equals(purity)) {
            selectedRate = rateSilver;
        } else if ("24K".equals(purity)) {
            selectedRate = rate24k;
        } else {
            selectedRate = rate22k; // Default to 22K
        }

        double materialCost = this.totalWeightTola * selectedRate;
        this.lineTotal = materialCost + this.wages + this.stoneCost;
    }
}