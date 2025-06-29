package com.example.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import it.unisa.dia.gas.jpbc.Element;

public class Util {
    // 私钥格式化
    public static String SkToPem(Element element) {
        ByteArrayOutputStream pemStream = new ByteArrayOutputStream();
        try {
            pemStream.write(("-----BEGIN PRIVATE KEY-----\n").getBytes());
            byte[] elementBytes = element.toBytes();
            String base64Encoded = Base64.getEncoder().encodeToString(elementBytes);
            int lineLength = 64;
            for (int i = 0; i < base64Encoded.length(); i += lineLength) {
                int endIndex = Math.min(i + lineLength, base64Encoded.length());
                pemStream.write((base64Encoded.substring(i, endIndex) + "\n").getBytes());
            }
            pemStream.write(("-----END PRIVATE KEY-----\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(pemStream.toByteArray());
    }

    // 公钥格式化
    public static String PkToPem(Element element) {
        ByteArrayOutputStream pemStream = new ByteArrayOutputStream();
        try {
            pemStream.write(("-----BEGIN PUBLIC KEY-----\n").getBytes());
            byte[] elementBytes = element.toBytes();
            String base64Encoded = Base64.getEncoder().encodeToString(elementBytes);
            int lineLength = 64;
            for (int i = 0; i < base64Encoded.length(); i += lineLength) {
                int endIndex = Math.min(i + lineLength, base64Encoded.length());
                pemStream.write((base64Encoded.substring(i, endIndex) + "\n").getBytes());
            }
            pemStream.write(("-----END PUBLIC KEY-----\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(pemStream.toByteArray());
    }

    // pem格式转公钥
    public static Element PemToPk(String pem) {
        // 去除 PEM 格式的头尾标记
        String base64Content = pem.replace("-----BEGIN PUBLIC KEY-----\n", "")
                .replace("-----END PUBLIC KEY-----\n", "")
                .replaceAll("\\s", "");
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

        return Common.pairing.getG1().newElementFromBytes(decodedBytes);
    }

    public static Element byteToPk(byte[] pk) {
        return Common.pairing.getG1().newElementFromBytes(pk);
    }

    // 读取 BLS 私钥
    public static Element PemToPk(InputStream inputStream) throws IOException {
        String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        Element keyElement = Common.pairing.getG1().newElementFromBytes(keyBytes);
        return keyElement;
    }

    public static Element PemToSk(String pem) {
        // 去除 PEM 格式的头尾标记
        String base64Content = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

        return Common.pairing.getZr().newElementFromBytes(decodedBytes);
    }

    // 读取 BLS 私钥
    public static Element PemToSk(InputStream inputStream) throws IOException {
        String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        Element keyElement = Common.pairing.getZr().newElementFromBytes(keyBytes);
        return keyElement;
    }

    // 将PEM格式的签名转换为Element
    public static Element PemToSig(String pem) {
        String base64Content = pem.replace("-----BEGIN SIGNATURE-----\n", "")
                .replace("-----END SIGNATURE-----\n", "")
                .replaceAll("\\s", "");
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        return Common.pairing.getG2().newElementFromBytes(decodedBytes);
    }

    // 新增方法：支持直接解析 byte[] 类型的签名数据
    public static Element PemToSig(byte[] pem) {
        return Common.pairing.getG2().newElementFromBytes(pem);
    }

    // 将签名转换为PEM格式
    public static String SigToPem(Element sig) {
        ByteArrayOutputStream pemStream = new ByteArrayOutputStream();
        try {
            pemStream.write(("-----BEGIN SIGNATURE-----\n").getBytes());
            byte[] sigBytes = sig.toBytes();
            String base64Encoded = Base64.getEncoder().encodeToString(sigBytes);
            int lineLength = 64;
            for (int i = 0; i < base64Encoded.length(); i += lineLength) {
                int endIndex = Math.min(i + lineLength, base64Encoded.length());
                pemStream.write((base64Encoded.substring(i, endIndex) + "\n").getBytes());
            }
            pemStream.write(("-----END SIGNATURE-----\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(pemStream.toByteArray());
    }

    public static X509Certificate parseCertificate(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator()); // 保留换行符
            }
            // 移除最后一个多余的换行符（如果有的话）
            if (content.length() > 0) {
                content.deleteCharAt(content.length() - 1);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取证书文件失败", e);
        }

        try (InputStream is = new ByteArrayInputStream(content.toString().getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static X509Certificate parseCertificateStream(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) { // 直接使用文件流
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        } catch (Exception e) {
            throw new RuntimeException("读取证书文件失败: " + filePath, e);
        }
    }

    public static X509Certificate parseCertificateFromString(String cert) {
        try (InputStream is = new ByteArrayInputStream(cert.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readCertificateFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) 
                content.append(line).append(System.lineSeparator()); // 保留换行符
            
            // 移除最后一个多余的换行符（如果有的话）
            if (content.length() > 0) {
                content.deleteCharAt(content.length() - 1);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取证书文件失败", e);
        }
        return content.toString();
    }

    /**
     * 读取密钥
     * 
     */
    public static Element readPrivateKey(String filePath) {
        // 从磁盘加载根证书私钥
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            return PemToSk(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取密钥
     * 
     */
    public static Element readPublicKey(String filePath) {
        // 从磁盘加载根证书私钥
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            return PemToPk(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
