package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ixi.model.Message;
import org.iota.ixi.model.MessageBuilder;
import org.iota.ixi.utils.KeyManager;
import org.json.JSONException;
import spark.Filter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.after;
import static spark.Spark.get;

public class ChatIxi extends IxiModule {

    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private String username;
    private final org.iota.ixi.utils.KeyPair keyPair;
    private Set<String> contacts = new HashSet<>();
    private Set<String> channels = new HashSet<>();

    public static void main(String[] args) {
        new ChatIxi(args.length > 0 ? args[0] : "anonymous");
    }

    public ChatIxi(String username) {
        super("chat.ixi");
        this.username = username;
        this.keyPair = KeyManager.loadKeyPair();
        System.out.println("Waiting for Ict to connect ...");
    }

    @Override
    public void onIctConnect(String name) {

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
            // addChannel/ is called N times in a very short interval (one time for each channel). Therefore the gossip filter will be changed N times.
            // Due to the delay of setGossipFilter(), it is important to ensure that setGossipFilter() is called in the correct order.
            // Otherwise the newest GossipFilter with N channels might be replaced by an older GossipFilter with L channels (L<N).
            // This would result in missing channels. The synchronized block ensures the correct order.

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channel = request.params(":channel");
                channels.add(channel);
                GossipFilter gossipFilter = new GossipFilter();
                for (String c : channels)
                    gossipFilter.watchAddress(c);
                setGossipFilter(gossipFilter);

                for(Transaction transaction : findTransactionsByAddress(channel))
                    messages.add(new Message(transaction, contacts));
            }
            return "";
        });

        get("/getMessage/", (request, response) -> {
            Message message = messages.take();
            return message.toString();
        });

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
        TransactionBuilder builder = new TransactionBuilder();
        builder.address = channel;
        builder.asciiMessage(message.toString());
        Transaction transaction = builder.build();
        submit(transaction);
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        try {
            Message m = new Message(event.getTransaction(), contacts);
            messages.add(m);
        } catch (JSONException e) { ; }
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        Message m = new Message(event.getTransaction(), contacts);
        messages.add(m);
    }

}
