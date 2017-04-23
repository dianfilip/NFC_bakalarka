package com.filip.nfcbak1;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by Filip on 23.10.2016.
 */
public class AsymmetricEncryption {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static final String TAG = "AsymmetricAlgorithmRSA";

    private PrivateKey privateKey = null;

    public byte[] encodeWithPrivate(String toEncode) {
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            c.init(Cipher.ENCRYPT_MODE, privateKey);
            encodedBytes = c.doFinal(toEncode.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "RSA encryption error");
        }

        //Log.i(TAG, "[ENCODED]:\n" + Base64.encodeToString(encodedBytes, Base64.DEFAULT) + "\n");

        return encodedBytes;
    }

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

        //String decodedString = Base64.encodeToString(decodedBytes, Base64.DEFAULT);
        //Log.i(TAG, "[DECODED]:\n" + decodedString + "\n");

        return decodedString;
    }

    public void setPrivateKey(String keyStringPrivate) {

        keyStringPrivate = keyStringPrivate.replace("\\r", "").replace("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

        try {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec((Base64.decode(keyStringPrivate, Base64.DEFAULT))));

            System.out.println(privateKey);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

}
