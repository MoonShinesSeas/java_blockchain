package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

// TransactionOperation.java
@Entity
@Table(name = "transactions")
@Data
public class TransactionOperation {
    @Id
    @Column(name = "request_id")
    private String requestId;

    @Column(name = "product_id")
    private String productId;
    
    private String saler;
    private String buyer;
    private Float amount;
    private Float balance;
    private String block_hash;
    private Long timestamp;
}
