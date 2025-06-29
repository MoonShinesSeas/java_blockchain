package com.example.client;

import com.example.util.BLS;
import com.example.util.Util;

import it.unisa.dia.gas.jpbc.Element;
import lombok.Data;

@Data
public class ClientRequest {
    // 安全验证
    private String signature; // 签名

    private String publicKey;// 公钥

    private String digest;

    private Request request;

    public ClientRequest() {

    }

    // 生成签名（客户端调用）
    public void sign(Element privateKey) {
        // 对核心数据签名（不包含签名字段本身）
        // clientId + requestId + timestamp + operation;
        String coreData = this.request.getClientId() + request.getRequestId() + request.getTimestamp()
                + request.getOperation() + this.getRequest().getData();
        String sig = Util.SigToPem(BLS.sign(coreData,privateKey));
        this.signature = sig;
    }

}
