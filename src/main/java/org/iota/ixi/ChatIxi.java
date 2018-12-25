package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ixi.model.Message;
import org.iota.ixi.model.MessageBuilder;
import org.iota.ixi.utils.KeyManager;
import spark.Filter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.after;
import static spark.Spark.get;

public class ChatIxi extends IxiModule {

    private GossipFilter gossipFilter = new GossipFilter();
    private BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private String username;
    private final org.iota.ixi.utils.KeyPair keyPair;
    private Set<String> contacts = new HashSet<>();

    public static void main(String[] args) {
        new ChatIxi(args.length > 0 ? args[0] : "anonymous");
    }

    public ChatIxi(String username) {
        super("chat.ixi");
        this.username = username;
        this.keyPair = KeyManager.loadKeyPair();
    }

    @Override
    public void onIctConnect(String name) {

        setGossipFilter(gossipFilter);

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        get("/getMyPublicKey", (request, response) -> keyPair.getPublicAsString());

        get("/addPublicKey/:public", (request, response) -> {
            contacts.add(request.params(":public"));
            return "";
        });

        get("/addChannel/:channel", (request, response) -> {
            String address = request.params(":channel");
            gossipFilter.watchAddress(address);
            setGossipFilter(gossipFilter);
            return "";
        });

        get("/getMessage/", (request, response) ->  messages.take().toString());

        get("/submitMessage/:channel/", (request, response) -> {
            String channel = request.params(":channel");
            String message = request.queryParams("message");
            submitMessage(channel, message);
            return "";
        });

        System.out.println("Connected!");

    }

    private void submitMessage(String channel, String message) {
        Message toSend = createMessage(channel, message);
        submitMessage(channel, toSend);
    }

    private Message createMessage(String channel, String message) {
        MessageBuilder builder = new MessageBuilder();
        builder.keyPair = keyPair;
        builder.username = username;
        builder.message = message;
        builder.channel = channel;
        return builder.build();
    }

    private void submitMessage(String channel, Message message) {
        TransactionBuilder b = new TransactionBuilder();
        b.address = channel;
        b.asciiMessage(message.toString());
        submit(b.build());
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        messages.add(new Message(event.getTransaction(), contacts));
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        messages.add(new Message(event.getTransaction(), contacts));
    }
}
