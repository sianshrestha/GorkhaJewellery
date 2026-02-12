package com.gorkha.gorkhajewellery.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description; // e.g., "Gold Ring"
    private String purity;      // e.g., "22K" or "24K"

    // --- The Jewelry Math Inputs ---
    private double netWeightLal;   // User types: 42
    private double wastageLal;     // User types: 3

    // --- The Financial Inputs ---
    private double wages;          // Making charges (e.g., 85.00)
    private double stoneCost;      // Cost of stones (if any)

    // --- Calculated Fields (Stored for History) ---
    private double totalWeightLal; // 45 (Net + Wastage)
    private double totalWeightTola;// 0.45 (Total Lal / 100)
    private double lineTotal;      // The final dollar amount for this row

    // --- The Logic ---
    // This method runs automatically before saving to ensure math is correct
    public void calculateLineTotal(double rate22k, double rate24k) {
        // 1. Calculate Total Weight (Net + Wastage)
        this.totalWeightLal = this.netWeightLal + this.wastageLal;

        // 2. Convert to Tola (1 Tola = 100 Lal)
        this.totalWeightTola = this.totalWeightLal / 100.0;

        // 3. Get the correct gold rate based on purity
        double selectedRate = "24K".equals(purity) ? rate24k : rate22k;

        // 4. Calculate Gold Cost: (Weight in Tola * Rate)
        double goldCost = this.totalWeightTola * selectedRate;

        // 5. Final Total: Gold Cost + Wages + Stones
        this.lineTotal = goldCost + this.wages + this.stoneCost;
    }
}
