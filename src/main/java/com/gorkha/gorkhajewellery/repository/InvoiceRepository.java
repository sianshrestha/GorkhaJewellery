package com.gorkha.gorkhajewellery.repository;

import com.gorkha.gorkhajewellery.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // This magic method creates a search query automatically
    List<Invoice> findByCustomerNameContainingIgnoreCase(String customerName);
}
