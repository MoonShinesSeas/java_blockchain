package com.example.controller;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSON;
import com.example.client.ClientRequest;
import com.example.client.Request;
import com.example.model.ProductOperation;
import com.example.model.ProductOperationRepository;
import com.example.model.ProductRequest;
import com.example.model.TransactionOperation;
import com.example.model.TransactionOperationRepository;
import com.example.model.TransactionRequest;
import com.example.service.ClientService;
import com.example.service.UserService;
import com.example.util.CryptoUtil;
import com.example.util.Util;

import it.unisa.dia.gas.jpbc.Element;

@CrossOrigin
@RestController
public class ClientController {
    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductOperationRepository productOperationRepository;

    @Autowired
    private TransactionOperationRepository transactionOperationRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/product/submit")
    public ResponseEntity<?> submitProduct(@RequestBody ProductRequest productRequest) {
        // amount实际需要通过智能电表同步查询是不是正确的数值
        if (productRequest.getProduct().getPrice() > 0.56 * 1.1) 
            // 单价大于基础价格的1.1倍
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("单价不能超过0.56元的1.1倍");
        
        if (!userService.verifyCertificate(productRequest.getCertificate())) 
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("证书验证失败");
        
        if (!userService.verifyUsername(productRequest.getCertificate(), productRequest.getUsername())) 
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("用户名不匹配");
        
        // 获取组织信息
        // 从请求提取证书
        X509Certificate clientCert = Util.parseCertificateFromString(productRequest.getCertificate());
        // 从证书提取组织
        String org = userService.getOrganization(clientCert);
        // 根据组织提取组织证书
        X509Certificate org_cert = userService.loadIntermediateCert(org);
        // 设置数据
        String owner = productRequest.getUsername();
        productRequest.getProduct().setOwner(owner);// owner
        productRequest.getProduct().setOrg(org);
        String productJson = JSON.toJSONString(productRequest.getProduct());
        // 从组织证书提取公钥
        Element pk = userService.getCertPublicKey(org_cert);
        String clientId = CryptoUtil.SHA256(Util.PkToPem(pk));
        Request request = new Request(clientId, 0);// 客户端标识
        request.setData(productJson); // 扩展data字段
        // 使用ClientService发送请求...
        ClientRequest clientRequest = new ClientRequest();
        clientService.associateRequestWithUser(request.getRequestId(), owner);
        clientRequest.setRequest(request);
        clientRequest.sign(userService.loadIntermediaPrivateKey(org));
        // 提取证书的公钥
        clientRequest.setPublicKey(Util.PkToPem(pk));
        clientRequest.setDigest(CryptoUtil.SHA256(JSON.toJSONString(request)));
        clientService.onRequest(clientRequest);
        return ResponseEntity.ok(Collections.singletonMap("requestId", request.getRequestId()));
    }

    // 按商品ID（requestId）查询
    @GetMapping("/product/{requestId}")
    public ResponseEntity<ProductOperation> getByRequestId(@PathVariable String requestId) {
        Optional<ProductOperation> product = productOperationRepository.findById(requestId);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 全表查询
    @GetMapping("/product/all")
    public List<ProductOperation> getAllProducts() {
        return productOperationRepository.findAll();
    }

    // 按拥有者查询
    @GetMapping("/product/owner/{owner}")
    public List<ProductOperation> getProductByOwner(@PathVariable String owner) {
        return productOperationRepository.findByOwner(owner);
    }

    @PostMapping("/transaction/submit")
    public ResponseEntity<?> submitTransaction(@RequestBody TransactionRequest transactionRequest) {
        // 触发上链
        if (!userService.verifyCertificate(transactionRequest.getCertificate())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("证书验证失败");
        }
        if (!userService.verifyUsername(transactionRequest.getCertificate(), transactionRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("用户名不匹配");
        }
        if (transactionRequest.getUsername().equals(transactionRequest.getTransaction().getSaler())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("不能购买自己的商品");
        }

        String buyer = transactionRequest.getUsername();// buyer

        transactionRequest.getTransaction()
                .setBalance(transactionRequest.getPrice() * transactionRequest.getTransaction().getAmount());// 设置交易金额
        transactionRequest.getTransaction().setBuyer(buyer);// buyer

        // 获取组织信息
        // 从请求提取证书
        X509Certificate clientCert = Util.parseCertificateFromString(transactionRequest.getCertificate());
        // 从证书提取组织
        String org = userService.getOrganization(clientCert);
        // 根据组织提取组织证书
        X509Certificate org_cert = userService.loadIntermediateCert(org);

        // 从组织证书提取公钥
        Element pk = userService.getCertPublicKey(org_cert);
        String clientId = CryptoUtil.SHA256(Util.PkToPem(pk));
        Request request = new Request(clientId, 1);// 客户端标识
        String transactionJson = JSON.toJSONString(transactionRequest.getTransaction());
        request.setData(transactionJson); // data字段
        // 使用ClientService发送请求...
        ClientRequest clientRequest = new ClientRequest();
        clientService.associateRequestWithUser(request.getRequestId(), buyer);
        clientRequest.setRequest(request);
        clientRequest.sign(userService.loadIntermediaPrivateKey(org));
        clientRequest.setPublicKey(Util.PkToPem(pk));
        clientRequest.setDigest(CryptoUtil.SHA256(JSON.toJSONString(request)));
        System.out.println(JSON.toJSONString(clientRequest));
        clientService.onRequest(clientRequest);
        return ResponseEntity.ok(Collections.singletonMap("requestId", request.getRequestId()));
    }

    @GetMapping("/transaction/buyer/{buyer}")
    public List<TransactionOperation> getTransactionByBuyer(@PathVariable String buyer) {
        return transactionOperationRepository.findByBuyer(buyer);
    }

    @GetMapping("/transaction/saler/{saler}")
    public List<TransactionOperation> getTransactionBySaler(@PathVariable String saler) {
        return transactionOperationRepository.findBySaler(saler);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("cert") String cert) {
        Map<String, String> userInfo = userService.handleUserCert(cert);
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("证书验证失败");
        }
        return ResponseEntity.ok(userInfo);
    }

}
