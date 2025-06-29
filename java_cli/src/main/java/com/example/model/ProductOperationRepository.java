package com.example.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOperationRepository extends JpaRepository<ProductOperation, String> {
    // 按拥有者查询
    List<ProductOperation> findByOwner(String owner);
}