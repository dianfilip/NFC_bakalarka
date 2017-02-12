package com.filip.nfcbak1;

import android.util.Base64;
import android.util.Log;


import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.Cipher;

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
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, privateKey);
            encodedBytes = c.doFinal(Base64.decode(toEncode, Base64.DEFAULT));
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
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.DECRYPT_MODE, privateKey);
            decodedBytes = c.doFinal(encodedBytes);
        } catch (Exception e) {
            Log.e(TAG, "RSA decryption error");
        }

        String decodedString = Base64.encodeToString(decodedBytes, Base64.DEFAULT);
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

    public void setPrivateKey(byte[] keyBytes) throws Exception {


        try {
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
        }
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public void testMethod() {

        String keyStringPublic = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCh0+deNV2127KGdvxF8YKnMVzB\n" +
                "NNm0R4BUeYNBJwMSiYRN9yDlDJS3MXYrQ91zSDxQcRBd7pteutNXdo8dtn3Q+NdL\n" +
                "v48gVX6JwxZ0e+KyOJz0JMQSotjLIIZSsfVNoRBSsxEsZuuy/ccWPsleeDS1gfMS\n" +
                "xxU7YxH8IgI1yHx5UQIDAQAB";

        String keyStringPrivate = "MIICXQIBAAKBgQCh0+deNV2127KGdvxF8YKnMVzBNNm0R4BUeYNBJwMSiYRN9yDl\n" +
                "DJS3MXYrQ91zSDxQcRBd7pteutNXdo8dtn3Q+NdLv48gVX6JwxZ0e+KyOJz0JMQS\n" +
                "otjLIIZSsfVNoRBSsxEsZuuy/ccWPsleeDS1gfMSxxU7YxH8IgI1yHx5UQIDAQAB\n" +
                "AoGAOh8TcCCWoaRggC7n+G7/T/FIsRO8RSWRD8X8wD+0uMmvPRlPNTTJjOo02OEs\n" +
                "/iSplPKmwDXck69iDH3GdROAKCWTfeUzdFk3yY4ohYSPgLM51u/AmwIITaAms+r/\n" +
                "KhpFXXl97txosZKwF8Vyy9PV7c2yZuRx+Kexv8kFS+lTXAECQQDRcbBBr4HML5LI\n" +
                "W0aPNEAxaU6hwwKxxEF7perJGWTOONb/66hyQGK5q1jvykIahoTjB+1U1Lpqe/eu\n" +
                "mFD+30OxAkEAxcyhgm1AeR+aJGhDmX2OCW+zw4aXA9wYRHI1jo3wKQHnCXGDaID2\n" +
                "ZKzBZygqMYEwV5HN3QBumBx499821F4XoQJAO04Cx4anrSZnXJ4jy5bS+mrEHh+2\n" +
                "2pkkpZtkcM7k8VO85ThYOQmsKsCu7S8LKrGeXR64gAXARziU+HYesRyM8QJBAIx7\n" +
                "Y5JQqePc0AtfifNvuvt0vEX4RzVUkl+6hdMzeAiH82E/n8cPIPArykjLu/vg90aa\n" +
                "pY17CxE516ikfjqigUECQQC0zx98bPTR2vY0s0A8TVhQZCi02baHhLbTcFSt4/Vh\n" +
                "4hfqSA0ubCMoOTZ/H4TFzmNzfIkRFKh7S9SX9K00zXSB";

        //String keyStringPublic = Base64.encodeToString(encryption.getPublicKey().getEncoded(), Base64.DEFAULT);

        keyStringPrivate = keyStringPrivate.replace("-----BEGIN PRIVATE KEY-----\n", "");
        keyStringPrivate = keyStringPrivate.replace("-----END PRIVATE KEY-----", "");

        Log.i(TAG, "string public: " + keyStringPublic);
        Log.i(TAG, "string private: " + keyStringPrivate);

        //keyStringPublic = keyStringPublic.substring(0,31);



        byte[] encodedKey = Base64.decode(keyStringPublic, Base64.DEFAULT);
        //*Key privateKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "RSA");
        this.setPublicKey(encodedKey);
        Log.i("HCEDEMO", "public key: " + this.getPublicKey().toString() + " format: " + this.getPublicKey().getFormat());

        try {
            encodedKey = Base64.decode(keyStringPrivate, Base64.DEFAULT);
            this.setPrivateKey(encodedKey);
            Log.i("HCEDEMO", "private key: " + this.getPrivateKey().toString() + " format: " + this.getPrivateKey().getFormat());
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] msg = this.encodeWithPrivate("ahojdasdas");
        String msgString = Base64.encodeToString(msg, Base64.DEFAULT);

        Log.i(TAG, "encoded with private: " + msgString + "size: " + msgString.length());

        byte[] decodedMsg = Base64.decode(msgString, Base64.DEFAULT);
        String decodedMsgString = this.decodeWithPublic(decodedMsg);

        Log.i(TAG, "decoded with public: " + decodedMsgString + "size: " + decodedMsgString.length());
    }

}
