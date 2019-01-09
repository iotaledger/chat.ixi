package org.iota.ixi.utils;

import com.google.common.hash.Hashing;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

public class AES {

    static {
        Security.setProperty("crypto.policy", "unlimited");
    }

	public static String encrypt(String plainText, String password, IvParameterSpec iv) throws AESException {
		try {
			byte[] p = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).asBytes();
			SecretKeySpec secretKey = new SecretKeySpec(p, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
		} catch (Exception e) {
			throw new AESException(e);
		}
	}

	public static String decrypt(String cipherText, String password, IvParameterSpec iv) throws AESException {
		try {
			byte[] p = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).asBytes();
			SecretKeySpec secretKey = new SecretKeySpec(p, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText.getBytes())));
		} catch (Exception e) {
			throw new AESException(e);
		}
	}

	public static IvParameterSpec generateRandomIv() {
		SecureRandom randomSecureRandom = new SecureRandom();
		byte[] iv = new byte[16];
		randomSecureRandom.nextBytes(iv);
		return new IvParameterSpec(iv);
	}

	public static class AESException extends Exception {
		public AESException(Throwable e) {
			super(e);
		}
	}

}