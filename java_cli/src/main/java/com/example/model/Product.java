package com.example.model;

import lombok.Data;

@Data
public class Product {
    private String owner;
    private String org;
    private String type;
    private Integer amount;
    private float price;
}