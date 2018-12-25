package org.iota.ixi.utils;

import org.iota.ixi.RSA;

import java.io.File;
import java.io.IOException;

public class KeyManager {

    private static final File publicKeyFile = new File("public.key");
    private static final File privateKeyFile = new File("private.key");

    public static KeyPair loadKeyPair() {
        try {
            return tryToLoadKeyPair();
        } catch (RSA.RSAException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair tryToLoadKeyPair() throws RSA.RSAException {

        if(!publicKeyFile.exists() || !privateKeyFile.exists()) {
            publicKeyFile.delete();
            privateKeyFile.delete();
            KeyPair keyPair = new KeyPair();
            storeKeyPairInFiles(keyPair, publicKeyFile, privateKeyFile);
        }

        try {
            String publicKeyString = FileOperations.readFromFile(publicKeyFile);
            String privateKeyString = FileOperations.readFromFile(privateKeyFile);
            return new KeyPair(publicKeyString, privateKeyString);
        } catch (IOException e) {
            e.printStackTrace();
            return new KeyPair();
        }
    }

    private static void storeKeyPairInFiles(KeyPair keyPair, File publicKeyFile, File privateKeyFile) {
        FileOperations.writeToFile(publicKeyFile, keyPair.getPublicAsString());
        FileOperations.writeToFile(privateKeyFile, keyPair.getPrivateAsString());
    }
}
