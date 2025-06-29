package com.example.service;

import org.springframework.stereotype.Service;

import com.example.model.Block;
import com.example.model.ProductOperation;
import com.example.model.ProductOperationRepository;
import com.example.model.TransactionOperation;
import com.example.model.TransactionOperationRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlockProcessorService {
    private final ProductOperationRepository productRepo;
    private final TransactionOperationRepository txRepo;
    private final ObjectMapper objectMapper;

    public void processConsensusBlock(Block block) {
        String blockHash = block.getHash();

        for (Block.BlockInfoEntry entry : block.getBlockInfo()) {
            try {
                switch (entry.getOperation()) {
                    case 0:
                        processProductOperation(entry, blockHash);
                        break;
                    case 1:
                        processTransactionOperation(entry, blockHash);
                        break;
                    default:
                        System.out.println("未知操作" + entry.getOperation());
                        break;
                }
            } catch (JsonProcessingException e) {
                System.out.println("Failed to process block entry: " + entry.getRequestId() + e);
            }
        }
    }

    private void processProductOperation(Block.BlockInfoEntry entry, String blockHash) throws JsonProcessingException {
        ProductData data = objectMapper.readValue(entry.getData(), ProductData.class);

        ProductOperation po = new ProductOperation();
        po.setRequestId(entry.getRequestId());
        po.setOrg(data.getOrg());
        po.setOwner(data.getOwner());
        po.setType(data.getType());
        po.setAmount(data.getAmount());
        po.setPrice(data.getPrice());
        po.setStatus(0); // 0表示在售状态
        po.setBlock_hash(blockHash);
        po.setTimestamp(entry.getTimestamp());

        productRepo.save(po);
    }

    private void processTransactionOperation(Block.BlockInfoEntry entry, String blockHash)
            throws JsonProcessingException {
        TransactionData data = objectMapper.readValue(entry.getData(), TransactionData.class);

        TransactionOperation tx = new TransactionOperation();
        tx.setRequestId(entry.getRequestId());
        tx.setBuyer(data.getBuyer());
        tx.setSaler(data.getSaler());
        tx.setProductId(data.getProductId());
        tx.setAmount(data.getAmount());
        tx.setBalance(data.getBalance());
        tx.setBlock_hash(blockHash);
        tx.setTimestamp(entry.getTimestamp());

        txRepo.save(tx);

        // 更新对应商品状态为已售出
        productRepo.findById(data.getProductId())
                .ifPresent(product -> {
                    product.setStatus(1);
                    productRepo.save(product);
                });
    }

    // 辅助类用于JSON解析
    @Data
    private static class ProductData {
        private String owner;
        private String org;
        private String type;
        private Integer amount;
        private Float price;
    }

    // TransactionData类
    @Data
    private static class TransactionData {
        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("saler")
        private String saler;

        @JsonProperty("buyer")
        private String buyer;

        @JsonProperty("amount")
        private Float amount;

        @JsonProperty("balance")
        private Float balance;
    }
}