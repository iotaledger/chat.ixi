package org.iota.ixi.chat.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.chat.ChatIxi;
import org.iota.ixi.chat.utils.AES;
import org.iota.ixi.chat.utils.KeyPair;
import org.iota.ixi.chat.utils.RSA;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;

public class Message {

    public final long timestamp;
    public final String username;
    public final String userid;
    public final String message;
    public final String channel;
    public final String signature;
    public final String publicKey;
    public final boolean isTrusted;
    public final boolean isOwn;
    public static SecureRandom random;
    private static final int IV_PARAMETER_SPEC_BYTE_LENGTH = 16;
    private static final int SEED_LENGTH = IV_PARAMETER_SPEC_BYTE_LENGTH / 2 * 3;

    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Message() {
        timestamp = 0;
        username = "";
        userid = "";
        message = "";
        channel = "";
        publicKey = "";
        signature = "";
        isTrusted = false;
        isOwn = false;
    }

    public Message(Transaction transaction, Set<String> contacts, String ownUserid) throws JSONException, AES.AESException, RSA.RSAException {

        channel = ChatIxi.getChannelOfAddress(transaction.address());
        String decodedSignatureFragments = transaction.decodedSignatureFragments();

        String seed = decodedSignatureFragments.substring(0, SEED_LENGTH);
        byte[] bytes = Trytes.toBytes(seed);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(bytes);
        String encrypted =  decodedSignatureFragments.substring(SEED_LENGTH);
        String jsonString = AES.decrypt(encrypted, channel, ivParameterSpec);
        final JSONObject jsonObject = new JSONObject(jsonString);

        timestamp = transaction.issuanceTimestamp;
        username = jsonObject.getString(Fields.username.name());
        ChatIxi.validateUsername(username);
        message = jsonObject.getString(Fields.message.name());
        publicKey = jsonObject.getString(Fields.public_key.name());
        userid = generateUserid(publicKey);
        signature = jsonObject.getString(Fields.signature.name());
        RSA.verify(getSignedData(), signature, KeyPair.publicKeyFromString(publicKey));
        isTrusted = contacts.contains(userid);
        isOwn = userid.equals(ownUserid);
    }

    Message(MessageBuilder builder) {
        timestamp = System.currentTimeMillis();
        username = builder.username;
        message = builder.message;
        channel = builder.channel;
        publicKey = builder.keyPair.getPublicKeyAsString();
        userid = generateUserid(publicKey);
        try {
            signature = RSA.sign(getSignedData(), builder.keyPair.privateKey);
        } catch (RSA.RSAException e) {
            throw new RuntimeException(e);
        }
        isOwn = true;
        isTrusted = true;
    }

    public static String generateUserid(String publicKey) {
        String publicKeyTrytes = Trytes.fromAscii(publicKey);
        String publicKeyHash = IotaCurlHash.iotaCurlHash(publicKeyTrytes, publicKeyTrytes.length(), 123);
        return publicKeyHash.substring(0, 8);
    }

    private String getSignedData() {
        return username+message+channel;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Fields.timestamp.name(), timestamp);
        jsonObject.put(Fields.username.name(), username);
        jsonObject.put(Fields.user_id.name(), userid);
        jsonObject.put(Fields.message.name(), message);
        jsonObject.put(Fields.channel.name(), channel);
        jsonObject.put(Fields.is_trusted.name(), isTrusted);
        jsonObject.put(Fields.is_own.name(), isOwn);
        return jsonObject;
    }

    public Transaction toTransaction() {
        JSONObject unencrypted = new JSONObject();
        unencrypted.put(Fields.username.name(), username);
        unencrypted.put(Fields.message.name(), message);
        unencrypted.put(Fields.signature.name(), signature);
        unencrypted.put(Fields.public_key.name(), publicKey);

        if(unencrypted.toString().length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength / 3 * 2) {
            System.err.println("Message to long, doesn't fit into transaction.");
            return null;
        }

        try {
            return encryptIntoTransaction(unencrypted, channel);
        } catch (AES.AESException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Transaction encryptIntoTransaction(JSONObject unencrypted, String channel) throws AES.AESException {

        TransactionBuilder builder = new TransactionBuilder();
        builder.address = ChatIxi.getAddressOfChannel(channel);

        String seed = Trytes.randomSequenceOfLength(SEED_LENGTH);
        byte[] bytes = Trytes.toBytes(seed);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(bytes);
        String encrypted = AES.encrypt(unencrypted.toString(), channel, ivParameterSpec);
        builder.asciiMessage(seed+encrypted);

        builder.tag = ChatIxi.calcLifeSignTag(System.currentTimeMillis());
        return builder.buildWhileUpdatingTimestamp();
    }

    private static String secureTryteSeed() {
        char[] seed = new char[SEED_LENGTH];
        for(int i = 0; i < SEED_LENGTH; i++)
            seed[i] = Trytes.TRYTES.charAt(random.nextInt());
        return new String(seed);
    }

    private enum Fields {
        username, user_id, message, timestamp, channel, public_key, signature, is_trusted, is_own
    }
}
