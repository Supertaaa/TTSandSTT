package com.vega.service.api.common;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Decrypto
{
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_MODE = "AES/ECB/PKCS5Padding";
    
    public static String decrypt(final String encryptedText, final String key) throws Exception {
        final SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(2, secretKey);
        final byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}