package org.iota.ixi.utils;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class KeyManagerTest {

    @BeforeClass
    public static void setUp() {
        KeyManager.PUBLIC_KEY_FILE = new File("test_public.key");
        KeyManager.PRIVATE_KEY_FILE = new File("test_private.key");
    }

    @Test
    public void testStore() throws RSA.RSAException, IOException {
        KeyPair original = new KeyPair();
        KeyManager.storeKeyPairInFiles(original);
        String readPublicKeyString = FileOperations.readFromFile(KeyManager.PUBLIC_KEY_FILE);
        Assert.assertEquals("Key storing failed: stored incorrect string.", original.getPublicAsString(), readPublicKeyString);
    }

    @Test
    public void testPersistence() throws RSA.RSAException {
        KeyPair original = new KeyPair();
        KeyManager.storeKeyPairInFiles(original);
        KeyPair loaded = KeyManager.loadKeyPair();
        Assert.assertEquals("Key persistence failed: resulted in different public keys.", original.getPublicAsString(), loaded.getPublicAsString());
        Assert.assertEquals("Key persistence failed: resulted in different private keys.", original.getPrivateAsString(), loaded.getPrivateAsString());
    }

    @AfterClass
    public static void tearDown() {
        KeyManager.deleteKeyFiles();;
    }
}