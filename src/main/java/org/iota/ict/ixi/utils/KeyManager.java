package org.iota.ict.ixi.utils;

import java.io.File;
import java.io.IOException;

public class KeyManager {

    static File PUBLIC_KEY_FILE = new File("public.key");
    static File PRIVATE_KEY_FILE = new File("private.key");

    public static KeyPair loadKeyPair() {
        try {
            return tryToLoadKeyPair();
        } catch (RSA.RSAException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair tryToLoadKeyPair() throws RSA.RSAException {

        if(!PUBLIC_KEY_FILE.exists() || !PRIVATE_KEY_FILE.exists()) {
            KeyPair keyPair = new KeyPair();
            storeKeyPairInFiles(keyPair);
            return keyPair;
        }

        try {
            String publicKeyString = FileOperations.readFromFile(PUBLIC_KEY_FILE);
            String privateKeyString = FileOperations.readFromFile(PRIVATE_KEY_FILE);
            return new KeyPair(publicKeyString, privateKeyString);
        } catch (IOException e) {
            e.printStackTrace();
            return new KeyPair();
        }
    }

    static void storeKeyPairInFiles(KeyPair keyPair) {
        deleteKeyFiles();
        FileOperations.writeToFile(PUBLIC_KEY_FILE, keyPair.getPublicKeyAsString());
        FileOperations.writeToFile(PRIVATE_KEY_FILE, keyPair.getPrivateKeyAsString());
    }

    static void deleteKeyFiles() {
        PUBLIC_KEY_FILE.delete();
        PRIVATE_KEY_FILE.delete();
    }
}
