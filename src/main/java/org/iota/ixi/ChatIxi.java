package org.iota.ixi;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.json.JSONObject;
import spark.Filter;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.after;
import static spark.Spark.get;

public class ChatIxi extends IxiModule {

    private GossipFilter gossipFilter = new GossipFilter();
    private BlockingQueue<Transaction> messages = new LinkedBlockingQueue<>();
    private String username;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Set<PublicKey> contacts = new HashSet<>();

    public static void main(String[] args) throws RSA.RSAException, IOException {
        new ChatIxi(args[0]);
    }

    public ChatIxi(String username) throws RSA.RSAException, IOException {
        super("chat.ixi");
        this.username = username;

        File pk = new File("public.key");
        File sk = new File("private.key");

        if(!pk.exists() || !sk.exists()) {
            pk.delete();
            sk.delete();

            KeyPair k = RSA.generateKeyPair();

            String publicKeyString = Keys.publicKeyToString(k.getPublic());
            String privateKeyString = Keys.privateKeyToString(k.getPrivate());

            Keys.writeToFile(publicKeyString, "public.key");
            Keys.writeToFile(privateKeyString, "private.key");
        }

        publicKey = Keys.readPublicKeyFromFile();
        privateKey = Keys.readPrivateKeyFromFile();

    }

    @Override
    public void onIctConnect(String name) {

        setGossipFilter(gossipFilter);

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        get("/getMyPublicKey", (request, response) -> {
            return Keys.publicKeyToString(publicKey);
        });

        get("/addPublicKey/:pk", (request, response) -> {
            String pk = request.params(":pk");
            contacts.add(Keys.loadPublicKey(pk));
            return "";
        });

        get("/addChannel/:channel", (request, response) -> {
            String address = request.params(":channel");
            gossipFilter.watchAddress(address);
            setGossipFilter(gossipFilter);
            return "";
        });

        get("/getMessage/", (request, response) -> {
            Transaction t = messages.take();
            JSONObject o = new JSONObject(t.decodedSignatureFragments).put("timestamp", t.issuanceTimestamp);

            String username = (String) o.get("username");
            String message = (String) o.get("message");
            String channel = (String) o.get("channel");
            PublicKey pk = Keys.loadPublicKey((String) o.get("publicKey"));
            String signature = (String) o.get("signature");

            boolean trusted = false;
            if(contacts.contains(pk))
                if(RSA.verify(username+message+channel,signature,pk))
                    trusted = true;

            o.put("trusted", trusted);

            return o.toString();
        });

        get("/submitMessage/:channel/", (request, response) -> {

            String channel = request.params(":channel");
            String message = request.queryParams("message");

            TransactionBuilder b = new TransactionBuilder();
            b.address = channel;

            JSONObject o = new JSONObject();
            o.accumulate("username", username);
            o.accumulate("message",message);
            o.accumulate("channel",channel);
            o.accumulate("publicKey",Keys.publicKeyToString(publicKey));
            o.accumulate("signature",RSA.sign(username+message+channel,privateKey));

            b.asciiMessage(o.toString());

            submit(b.build());

            return "";

        });

        System.out.println("Connected!");

    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        messages.add(event.getTransaction());
        System.out.println("RECEIVED");
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        messages.add(event.getTransaction());
        System.out.println("SUBMITTED");
    }

    private String hash(String s) {
        String trytes = Trytes.fromAscii(s);
        return IotaCurlHash.iotaCurlHash(trytes, trytes.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }

}
