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
    public void calculateLineTotal(double rate22k, double rate24k) {
        // 1. Normalize Net Weight to Lal for Math
        double actualWeightInLal = this.netWeightLal;

        // If user selected Tola, convert input to Lal (1 Tola = 100 Lal approx, or 11.66g)
        // Adjust this formula if your Tola conversion is different (e.g. 11.664g)
        // For simple math: 1 Tola = 100 Lal in many local systems,
        // but if using standard units: 1 Tola = 11.6638 grams.
        // Assuming 1 Tola = 100 Lal for this specific logic:
        if ("Tola".equals(this.weightUnit)) {
            actualWeightInLal = this.netWeightLal * 100.0;
        }

        // 2. Calculate Total Weight in Lal (Net + Wastage)
        this.totalWeightLal = actualWeightInLal + this.wastageLal;

        // 3. Set Display Weight (Convert back if unit is Tola)
        if ("Tola".equals(this.weightUnit)) {
            this.displayTotalWeight = this.totalWeightLal / 100.0;
        } else {
            this.displayTotalWeight = this.totalWeightLal;
        }

        // 4. Calculate Costs (Rate is usually per Tola or 10g depending on region)
        // Assuming Rate is per Tola (100 Lal)
        this.totalWeightTola = this.totalWeightLal / 100.0;

        double selectedRate = "24K".equals(purity) ? rate24k : rate22k;
        double goldCost = this.totalWeightTola * selectedRate;

        this.lineTotal = goldCost + this.wages + this.stoneCost;
    }
}