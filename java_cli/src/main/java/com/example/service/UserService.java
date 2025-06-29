package com.example.service;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.util.BLS;
import com.example.util.CryptoUtil;
import com.example.util.Util;

import it.unisa.dia.gas.jpbc.Element;

@Service
public class UserService {

    private final static String certPath = "/app/certs";
    private final static Logger logger = LoggerFactory.getLogger(UserService.class);
    // private final static String root_cert_path =
    // "src\\main\\resources\\root\\root_ca.cer";

    // private final static String org1_cert_path = "src\\main\\resources\\cert\\org\\org1\\org_cert.cer";

    // private final static String org2_cert_path = "src\\main\\resources\\cert\\org\\org2\\org_cert.cer";

    // private final static String org1_private_key_path = "src\\main\\resources\\cert\\org\\org1\\org_sk.key";

    // private final static String org2_private_key_path = "src\\main\\resources\\cert\\org\\org2\\org_sk.key";

    // 动态生成文件路径
    private String getOrgCertPath(String org) {
        return certPath + "/org/" + org + "/org_cert.cer";
    }

    private String getOrgPrivateKeyPath(String org) {
        return certPath + "/org/" + org + "/org_sk.key";
    }

    // 根据颁发者的OU字段确定组织
    public String getOrganization(X509Certificate cert) {
        String issuer = cert.getIssuerX500Principal().getName();
        if (issuer.contains("OU=org1"))
            return "org1";
        else if (issuer.contains("OU=org2"))
            return "org2";
        return null;
    }

    // // 加载中间证书
    // public X509Certificate loadIntermediateCert(String org) {
    // String path = org.equals("org1") ? org1_cert_path : org2_cert_path;
    // return Util.parseCertificate(path);
    // }

    // // 加载中间私钥
    // public Element loadIntermediaPrivateKey(String org) {
    // String path = org.equals("org1") ? org1_private_key_path :
    // org2_private_key_path;
    // return Util.readPrivateKey(path);
    // }
    public X509Certificate loadIntermediateCert(String org) {
        String path = getOrgCertPath(org);
        return Util.parseCertificate(path);
    }

    public Element loadIntermediaPrivateKey(String org) {
        String path = getOrgPrivateKeyPath(org);
        return Util.readPrivateKey(path);
    }

    // UserService.java
    public Map<String, String> handleUserCert(String cert) {
        if (!verifyCertificate(cert)) {
            logger.info("证书验证错误");
            return null;
        }
        // 提取公钥和用户名哈希
        Element client_pk = getCertPublicKey(cert);
        if (client_pk == null) {
            System.out.println("获取公钥失败");
            return null;
        }
        String usernameHash = CryptoUtil.SHA256(Util.PkToPem(client_pk));

        // 获取组织信息
        X509Certificate clientCert = Util.parseCertificateFromString(cert);
        String org = getOrganization(clientCert);

        // 返回组织信息和用户名哈希
        Map<String, String> result = new HashMap<>();
        result.put("username", usernameHash);
        result.put("organization", org);
        return result;
    }

    public boolean verifyCertificate(String cert) {
        // 从消息加载客户端证书
        X509Certificate client_cert = Util.parseCertificateFromString(cert);
        String org = getOrganization(client_cert);

        // 从磁盘加载中间证书
        X509Certificate org_cert = loadIntermediateCert(org);

        // 提取证书中的公钥
        Element org_pk;
        try {
            org_pk = BLS.decodePublicKey(org_cert.getPublicKey().getEncoded());
        } catch (Exception e) {
            System.out.println("获取证书公钥失败" + e.getMessage());
            return false;
        }
        try {
            /*
             * 验证客户端证书
             */
            // 获取 DER 编码的 TBS 数据（直接使用字节数组，不转为字符串）
            byte[] tbsData = client_cert.getTBSCertificate();
            // 提取证书签名并解码 DER Octet String
            byte[] cert_signature = client_cert.getSignature();
            ASN1InputStream asn = new ASN1InputStream(cert_signature);
            ASN1Primitive asn1 = asn.readObject();
            byte[] cert_sigBytes = ((DEROctetString) asn1).getOctets(); // 提取原始签名字节
            Element cert_sig = Util.PemToSig(cert_sigBytes); // 使用 BLS 库方法解码
            asn.close();
            if (!BLS.verify(cert_sig, org_pk, new String(tbsData)))
                return false;
        } catch (Exception e) {
            System.out.println("验证证书异常" + e.getMessage());
            return false;
        }
        return true;
    }

    // 验证用户名是不是一致
    public boolean verifyUsername(String cert, String providedUsername) {
        if (!verifyCertificate(cert))
            return false;
        Element client_pk = getCertPublicKey(cert);
        // 验证用户名一致性
        return CryptoUtil.SHA256(Util.PkToPem(client_pk)).equals(providedUsername);
    }

    // 从证书提取公钥
    public Element getCertPublicKey(String cert) {
        X509Certificate client_cert = Util.parseCertificateFromString(cert);

        Element client_pk;
        try {
            client_pk = BLS.decodePublicKey(client_cert.getPublicKey().getEncoded());
        } catch (Exception e) {
            System.out.println("获取公钥失败" + e.getMessage());
            return null;
        }
        return client_pk;
    }

    // 从证书提取公钥
    public Element getCertPublicKey(X509Certificate cert) {
        Element client_pk;
        try {
            client_pk = BLS.decodePublicKey(cert.getPublicKey().getEncoded());
        } catch (Exception e) {
            System.out.println("获取公钥失败" + e.getMessage());
            return null;
        }
        return client_pk;
    }
}
