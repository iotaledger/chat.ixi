package org.iota.ixi.utils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.Cipher;

public class RSA {

	private static final String ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"; // RSA/NONE/OAEPWithSHA-256AndMGF1Padding

	public static String encrypt(PublicKey publicKey, String message) throws RSAException {
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
		} catch (Exception e) {
			throw new RSAException(e);
		}
	}

	public static String decrypt(PrivateKey privateKey, String message) throws RSAException {
		try {
			byte[] encrypted = Base64.getDecoder().decode(message);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return new String(cipher.doFinal(encrypted));
		} catch (Exception e) {
			throw new RSAException(e);
		}
	}

	public static String sign(String plainText, PrivateKey privateKey) throws RSAException {
		try {
			Signature privateSignature = Signature.getInstance("SHA256withRSA");
			privateSignature.initSign(privateKey);
			privateSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
			byte[] signature = privateSignature.sign();
			return Base64.getEncoder().encodeToString(signature);
		} catch (Exception e) {
			throw new RSAException(e);
		}
	}

	public static boolean verify(String plainText, String signature, PublicKey publicKey) throws RSAException {
		try {
			Signature publicSignature = Signature.getInstance("SHA256withRSA");
			publicSignature.initVerify(publicKey);
			publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = Base64.getDecoder().decode(signature);
			return publicSignature.verify(signatureBytes);
		} catch (Exception e) {
			throw new RSAException(e);
		}
	}

	public static KeyPair generateKeyPair() throws RSAException {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048, new SecureRandom());
			KeyPair pair = generator.generateKeyPair();
			return pair;
		} catch (Exception e) {
			throw new RSAException(e);
		}
	}

	public static class RSAException extends Exception {
		public RSAException(Throwable e) {
			super(e);
		}
	}

}