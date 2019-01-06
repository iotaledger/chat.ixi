package org.iota.ict.ixi.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyPairTest {

    @Test
    public void testKeyToStringConversion() throws RSA.RSAException {
        KeyPair keyPair = new KeyPair();
        PublicKey decodedPublic = KeyPair.publicKeyFromString(keyPair.getPublicKeyAsString());
        PrivateKey decodedPrivate = KeyPair.privateKeyFromString(keyPair.getPrivateKeyAsString());
        Assert.assertEquals("Key decoding failed: resulted in different public key.", decodedPublic, keyPair.publicKey);
        Assert.assertEquals("Key decoding failed: resulted in different private key.", decodedPrivate, keyPair.privateKey);
    }
}