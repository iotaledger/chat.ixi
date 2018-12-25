package org.iota.ixi.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.RSA;
import org.iota.ixi.utils.KeyPair;
import org.json.JSONException;
import org.json.JSONObject;

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

    public Message() {
        timestamp = 0;
        username = "";
        userid = "";
        message = "";
        channel = "";
        publicKey = "";
        signature = "";
        isTrusted = false;
    }

    public Message(Transaction transaction, Set<String> contacts) throws JSONException, RSA.RSAException {
        final JSONObject jsonObject = new JSONObject(transaction.decodedSignatureFragments);
        timestamp = transaction.issuanceTimestamp;
        username = jsonObject.getString(Fields.username.name());
        message = jsonObject.getString(Fields.message.name());
        channel = jsonObject.getString(Fields.channel.name());
        publicKey = jsonObject.getString(Fields.public_key.name());
        userid = generateUserid(publicKey);
        signature = jsonObject.getString(Fields.signature.name());
        RSA.verify(getSignedData(), signature, KeyPair.publicKeyFromString(publicKey));
        isTrusted = contacts.contains(publicKey);
    }

    Message(MessageBuilder builder) {
        timestamp = System.currentTimeMillis();
        username = builder.username;
        message = builder.message;
        channel = builder.channel;
        publicKey = builder.keyPair.getPublicAsString();
        userid = generateUserid(publicKey);
        try {
            signature = RSA.sign(getSignedData(), builder.keyPair.privateKey);
        } catch (RSA.RSAException e) {
            throw new RuntimeException(e);
        }
        isTrusted = true;
    }

    private String generateUserid(String publicKey) {
        String publicKeyTrytes = Trytes.fromAscii(publicKey);
        return IotaCurlHash.iotaCurlHash(publicKeyTrytes, publicKeyTrytes.length(), 123);
    }

    private String getSignedData() {
        return username+message+channel;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Fields.timestamp.name(), timestamp);
        jsonObject.put(Fields.username.name(), username);
        jsonObject.put(Fields.user_id.name(), userid);
        jsonObject.put(Fields.message.name(), message);
        jsonObject.put(Fields.channel.name(), channel);
        jsonObject.put(Fields.signature.name(), signature);
        jsonObject.put(Fields.public_key.name(), publicKey);
        jsonObject.put(Fields.is_trusted.name(), isTrusted);
        return jsonObject.toString();
    }

    private enum Fields {
        username, user_id, message, timestamp, channel, public_key, signature, is_trusted
    }
}
