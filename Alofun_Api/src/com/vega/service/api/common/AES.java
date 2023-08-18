package com.vega.service.api.common;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class AES {

    private static String algorithm = "AES";

    // Performs Encryption
    public static String encrypt(String plainText, String strKey)
            throws Exception {
        Key key = generateKey(strKey);
        Cipher chiper = Cipher.getInstance(algorithm);
        chiper.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = chiper.doFinal(plainText.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encVal);
        return encryptedValue;
    }

    // Performs decryption
    public static String decrypt(String encryptedText, String strKey) throws Exception {
        // generate key
        Key key = generateKey(strKey);
        Cipher chiper = Cipher.getInstance(algorithm);
        chiper.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedText);
        byte[] decValue = chiper.doFinal(decordedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    // generateKey() is used to generate a secret key for AES algorithm
    private static Key generateKey(String strKey) throws Exception {
        Key key = new SecretKeySpec(strKey.getBytes(), algorithm);
        return key;
    }

    public static String encodeAESKeyToBase64(final SecretKey aesKey)
            throws IllegalArgumentException {
        if (!aesKey.getAlgorithm().equalsIgnoreCase("AES")) {
            throw new IllegalArgumentException("Not an AES key");
        }

        final byte[] keyData = aesKey.getEncoded();
        final String encodedKey = DatatypeConverter.printBase64Binary(keyData);
        return encodedKey;
    }
}
