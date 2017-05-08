package com.filip.nfcbak1;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Autor: Filip Dian
 *
 * Trieda obsluhujuca vytvaranie kluca a sifrovanie.
 */
public class AsymmetricEncryption {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static final String TAG = "AsymmetricAlgorithmRSA";

    private PrivateKey privateKey = null;

    /**
     * Zasifrovanie spravy.
     *
     * @param toEncode - sprava na zasifrovanie
     * @return encodedBytes - bajty zasifrovanej spravy
     */
    public byte[] encodeWithPrivate(String toEncode) {
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            c.init(Cipher.ENCRYPT_MODE, privateKey);
            encodedBytes = c.doFinal(toEncode.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "RSA encryption error");
        }

        return encodedBytes;
    }

    /**
     * Odsifrovanie zasifrovanej spravy
     *
     * @param encodedBytes - bajty zasifrovanej spravy
     * @return decodedString - odisfrovana sprava
     */
    public String decodeWithPrivate(byte[] encodedBytes) {
        byte[] decodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            c.init(Cipher.DECRYPT_MODE, privateKey);
            decodedBytes = c.doFinal(encodedBytes);
        } catch (Exception e) {
            Log.e(TAG, "RSA decryption error");
        }

        String decodedString = null;

        try {
            decodedString = new String(decodedBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return decodedString;
    }

    /**
     * Nastavenie kluca pre pouzitie v sifrovanie.
     *
     * @param keyStringPrivate
     */
    public void setPrivateKey(String keyStringPrivate) {

        keyStringPrivate = keyStringPrivate.replace("\\r", "").replace("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

        try {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec((Base64.decode(keyStringPrivate, Base64.DEFAULT))));

            Log.i(TAG, "" + privateKey);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}
