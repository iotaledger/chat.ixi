package org.iota.ixi.chat.utils;

import org.iota.ixi.chat.ChatIxi;

import java.io.File;
import java.io.IOException;

public class KeyManager {

    static final File PUBLIC_KEY_FILE = new File(ChatIxi.DIRECTORY, "public.key");
    static final File PRIVATE_KEY_FILE = new File(ChatIxi.DIRECTORY, "private.key");

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
        try {
            PUBLIC_KEY_FILE.getParentFile().mkdirs();
            PUBLIC_KEY_FILE.createNewFile();
            PRIVATE_KEY_FILE.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileOperations.writeToFile(PUBLIC_KEY_FILE, keyPair.getPublicKeyAsString());
        FileOperations.writeToFile(PRIVATE_KEY_FILE, keyPair.getPrivateKeyAsString());
    }

    static void deleteKeyFiles() {
        PUBLIC_KEY_FILE.delete();
        PRIVATE_KEY_FILE.delete();
    }
}
