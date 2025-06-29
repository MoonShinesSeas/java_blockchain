package com.example.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOperationRepository extends JpaRepository<TransactionOperation, String> {
        List<TransactionOperation> findByBuyer(String buyer);
        List<TransactionOperation> findBySaler(String saler);
}