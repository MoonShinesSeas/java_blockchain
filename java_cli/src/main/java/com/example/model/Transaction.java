package com.example.model;

import lombok.Data;

@Data
public class Transaction {
    private String product_id;
    private String saler;
    private String buyer;
    private Integer amount;
    private Float balance;
}
