package com.filip.nfcbak1;

import android.util.Base64;
import android.util.Log;


import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

/**
 * Created by Filip on 23.10.2016.
 */
public class AsymmetricEncryption {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static final String TAG = "AsymmetricAlgorithmRSA";

    private PublicKey publicKey = null;

    private PrivateKey privateKey = null;

    public void generateKeys() {
        // Generate key pair for 1024-bit RSA encryption and decryption
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(32);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();

            Log.i(TAG, "public key: " + publicKey.toString() + " format: " + publicKey.getFormat());
            Log.i(TAG, "private key: " + privateKey.toString() + " format: " + privateKey.getFormat());
        } catch (Exception e) {
            Log.e(TAG, "RSA key pair error");
        }
    }

    public byte[] encodeWithPublic(String toEncode) {
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, publicKey);
            encodedBytes = c.doFinal(Base64.decode(toEncode, Base64.DEFAULT));
        } catch (Exception e) {
            Log.e(TAG, "RSA encryption error");
        }

        //Log.i(TAG, "[ENCODED]:\n" + Base64.encodeToString(encodedBytes, Base64.DEFAULT) + "\n");

        return encodedBytes;
    }

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

    public String decodeWithPublic(byte[] encodedBytes) {
        byte[] decodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.DECRYPT_MODE, publicKey);
            decodedBytes = c.doFinal(encodedBytes);
        } catch (Exception e) {
            Log.e(TAG, "RSA decryption error");
        }

        String decodedString = Base64.encodeToString(decodedBytes, Base64.DEFAULT);
        //Log.i(TAG, "[DECODED]:\n" + decodedString + "\n");

        return decodedString;
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

    public void setPublicKey(byte[] keyBytes) {
        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setPrivateKey(String keyStringPrivate) {

        keyStringPrivate = keyStringPrivate.replace("\\r", "").replace("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

        try {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec((Base64.decode(keyStringPrivate, Base64.DEFAULT))));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        /*try {
            DerInputStream derReader = new DerInputStream(keyBytes);

            DerValue[] seq = derReader.getSequence(0);

            if (seq.length < 9) {
                throw new GeneralSecurityException("Could not parse a PKCS1 private key.");
            }

            // skip version seq[0];
            BigInteger modulus = seq[1].getBigInteger();
            BigInteger publicExp = seq[2].getBigInteger();
            BigInteger privateExp = seq[3].getBigInteger();
            BigInteger prime1 = seq[4].getBigInteger();
            BigInteger prime2 = seq[5].getBigInteger();
            BigInteger exp1 = seq[6].getBigInteger();
            BigInteger exp2 = seq[7].getBigInteger();
            BigInteger crtCoef = seq[8].getBigInteger();

            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);

            KeyFactory factory = KeyFactory.getInstance("RSA");

            privateKey = factory.generatePrivate(keySpec);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public static void testMethod() {

        String keyStringPrivate = "-----BEGIN RSA PRIVATE KEY-----\\r\\nMIICXAIBAAKBgQCkngS0Iu5rWLtGt\\/EwMzyHwBJip3e6KUflolmqRo6aBVsG0WMI\\r\\nr1AwU69hecpLX1RkQC2XgFypqslkGQ9vcbuNpNgkanaqsbKCKMRzIAtAmFi8RqU1\\r\\nwzA2KcmV5hbOVQtzubVnly\\/KkEvN2vqtMxyrnXDafueZRls5JfuSfcyOewIDAQAB\\r\\nAoGAbB0j9bLjZzkVdjKkgwWDgZyR9p0KMwedoqFnxj8ktN9Dk0y9gByzy6mKi7hT\\r\\nNgFcCaNkzhWNxhjWv5j93DGT\\/TtNBzkxeVD8FFKRwL\\/Ny0VTdgxrW0zKsROgmR+H\\r\\nEuDGBcbe5UYaUhazQPH7nZhJ7GuDy++FxZFlmWsZhmejqDECQQDRU4fkh4VUmuoN\\r\\nd9DkN0dXh0cHGjMCtTg0UXu274f2QdNm8guPx8v5RpwK2GAe9ole5CZf+AriztSE\\r\\nAUNCaLOPAkEAyVJ4KGKKL5uf9zJNLDG9BTTJt8Xe4vD7nROhLxGommDD8c\\/cU4n3\\r\\nbD+dYyU3zGkyEYlK+1i6fWnzgPNdlTAQVQJBAKO6f1dr\\/QjhJuMj7Zsj9cRrxk2y\\r\\n22Vp061wcqDzGFiwwicKeaqbr1qqNRFyjzSIx4gWUkHMZM9k0eryheZiuNcCQBsO\\r\\nomeLFtdfKximAgk2hhj1B0dTqKkHikmKIdeZn\\/dfmfYd4Za4rDA4PIbesakfWkNR\\r\\nGGq\\/ehDw9HEYRDOQyiECQHRX0LlEPw6oVXC1mrZSooVGHBAevvXNTCflEPJc3WEO\\r\\nYxTBhCexv4t9pUSd4WYNIiYUHSBEeI4tvigmjg8T7tM=\\r\\n-----END RSA PRIVATE KEY-----";
        keyStringPrivate = keyStringPrivate.replace("\\r", "").replace("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");
        System.out.println(keyStringPrivate);

        byte[] decoded = null;

        try {
            PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(keyStringPrivate, Base64.DEFAULT)));

            System.out.println(pk);


            //Cipher ci = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*byte[] msg = this.encodeWithPrivate("ahojdasdas");
        String msgString = Base64.encodeToString(msg, Base64.DEFAULT);

        Log.i(TAG, "encoded with private: " + msgString + "size: " + msgString.length());

        byte[] decodedMsg = Base64.decode(msgString, Base64.DEFAULT);
        String decodedMsgString = this.decodeWithPublic(decodedMsg);

        Log.i(TAG, "decoded with public: " + decodedMsgString + "size: " + decodedMsgString.length());*/
    }

}
