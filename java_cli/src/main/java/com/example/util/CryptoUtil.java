package com.example.util;

import org.springframework.util.DigestUtils;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CryptoUtil {

    /**
     * SHA256散列函数
     *
     * @param str
     * @return
     */
    public static String SHA256(String str) {
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (Exception e) {
            System.out.println("getSHA256 is error" + e.getMessage());
        }
        return encodeStr;
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        String temp;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                builder.append("0");
            }
            builder.append(temp);
        }
        return builder.toString();
    }

    public static String MD5(String str) {
        String resultStr = DigestUtils.md5DigestAsHex(str.getBytes());
        return resultStr.substring(4, resultStr.length());
    }

    public static String UUID() {
        return java.util.UUID.randomUUID().toString().replaceAll("\\-", "");
    }

        /**
     * 获取当前时间（精确到毫秒）的字符串
     * @return 返回时间字符串yyyy-MM-dd HH:mm:ss.SSS
     */
    public static String getTheTimeInMilliseconds() {
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        return time.format(date);
    }
}