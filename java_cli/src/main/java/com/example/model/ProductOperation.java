package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

// ProductOperation.java
@Entity
@Table(name = "product")
@Data
public class ProductOperation {
    @Id
    @Column(name = "request_id")
    private String requestId;
    private String org;
    private String owner;
    private String type;
    private Integer amount;
    private Float price;
    private Integer status;
    private String block_hash;
    private Long timestamp;
}