package com.example.model;

import lombok.Data;

@Data
public class ProductRequest {
    private Product product;
    private String certificate;
    private String username;
}
