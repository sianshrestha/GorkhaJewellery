package com.gorkha.gorkhajewellery.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OldGoldItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description = "";
    private String purity = "";
    private double grossWeight = 0.0;
    private double purityLoss = 0.0;
    private double netWeight = 0.0;
    private double amount = 0.0;
}