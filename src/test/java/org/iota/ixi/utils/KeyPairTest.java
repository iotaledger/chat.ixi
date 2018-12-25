package org.iota.ixi.utils;

import org.iota.ixi.RSA;
import org.junit.Assert;
import org.junit.Test;

import java.security.PublicKey;

public class KeyPairTest {

    @Test
    public void testKeyToStringConversion() throws RSA.RSAException {
        KeyPair keyPair = new KeyPair();
        PublicKey decoded = KeyPair.publicKeyFromString(keyPair.getPublicAsString());
        Assert.assertEquals(decoded, keyPair.publicKey);
    }
}