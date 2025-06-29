package com.example.model;

import lombok.Data;

@Data
public class TransactionRequest {
    private Transaction transaction;
    private float price;//单价
    private String username;
    private float balance;
    private String certificate;
}
