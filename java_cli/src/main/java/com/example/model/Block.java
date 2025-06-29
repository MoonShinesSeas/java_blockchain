package com.example.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Block implements Serializable {

    private static final long serialVersionUID = 1L;

    private int index;

    private String hash;

    private String previousHash;

    private long timestamp;

    @JsonProperty("blockInfo")
    private List<BlockInfoEntry> blockInfo = new ArrayList<>();

    @Data
    public static class BlockInfoEntry {
        private String clientId;
        private String data;
        private String requestId;
        private int operation;
        private long timestamp;
    }

}