package org.iota.ixi.utils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public final class KeyPair {

    private static final KeyFactory KEY_FACTORY;
    public final PrivateKey privateKey;
    public final PublicKey publicKey;

    static {
        try {
            KEY_FACTORY = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public KeyPair(String publicKeyString, String privateKeyString) {
        publicKey = publicKeyFromString(publicKeyString);
        privateKey = privateKeyFromString(privateKeyString);
    }


    public KeyPair() throws RSA.RSAException {
        java.security.KeyPair keyPair = RSA.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    public String getPublicAsString() {
        try {
            X509EncodedKeySpec spec = KEY_FACTORY.getKeySpec(publicKey, X509EncodedKeySpec.class);
            return Base64.getEncoder().encodeToString(spec.getEncoded());
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrivateAsString() {
        try {
            PKCS8EncodedKeySpec spec = KEY_FACTORY.getKeySpec(privateKey, PKCS8EncodedKeySpec.class);
            byte[] packed = spec.getEncoded();
            String key64 = Base64.getEncoder().encodeToString(packed);
            Arrays.fill(packed, (byte) 0);
            return key64;
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey publicKeyFromString(String s) {
        try {
            byte[] data = Base64.getDecoder().decode(s);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            return KEY_FACTORY.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    static PrivateKey privateKeyFromString(String s) {
        try {
            byte[] clear = Base64.getDecoder().decode(s);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
            PrivateKey priv = KEY_FACTORY.generatePrivate(keySpec);
            Arrays.fill(clear, (byte) 0);
            return priv;
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}