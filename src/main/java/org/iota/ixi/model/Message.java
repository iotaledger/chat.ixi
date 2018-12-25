package org.iota.ixi.model;

import org.iota.ict.model.Transaction;
import org.iota.ixi.RSA;
import org.iota.ixi.utils.KeyPair;
import org.json.JSONObject;

import java.util.Set;

public class Message {

    public final long timestamp;
    public final String username;
    public final String message;
    public final String channel;
    public final String signature;
    public final String publicKey;
    public final boolean isTrusted;

    public Message(Transaction transaction, Set<String> contacts) {
        final JSONObject jsonObject = new JSONObject(transaction.decodedSignatureFragments);
        timestamp = transaction.issuanceTimestamp;
        username = jsonObject.getString(Fields.username.name());
        message = jsonObject.getString(Fields.message.name());
        channel = jsonObject.getString(Fields.channel.name());
        publicKey = jsonObject.getString(Fields.public_key.name());
        signature = jsonObject.getString(Fields.signature.name());
        isTrusted = isTrusted(contacts);
    }

    Message(MessageBuilder builder) {
        timestamp = System.currentTimeMillis();
        username = builder.username;
        message = builder.message;
        channel = builder.channel;
        publicKey = builder.keyPair.getPublicAsString();
        try {
            signature = RSA.sign(getSignedData(), builder.keyPair.privateKey);
        } catch (RSA.RSAException e) {
            throw new RuntimeException(e);
        }
        isTrusted = true;
    }

    private boolean isTrusted(Set<String> contacts) {
        try {
            return contacts.contains(publicKey) && RSA.verify(getSignedData(), signature, KeyPair.publicKeyFromString(publicKey));
        } catch (RSA.RSAException e) {
            return false;
        }
    }

    private String getSignedData() {
        return username+message+channel;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Fields.timestamp.name(), timestamp);
        jsonObject.put(Fields.username.name(), username);
        jsonObject.put(Fields.message.name(), message);
        jsonObject.put(Fields.channel.name(), channel);
        jsonObject.put(Fields.signature.name(), signature);
        jsonObject.put(Fields.public_key.name(), publicKey);
        jsonObject.put(Fields.is_trusted.name(), isTrusted);
        return jsonObject.toString();
    }

    private enum Fields {
        username, message, timestamp, channel, public_key, signature, is_trusted
    }
}
