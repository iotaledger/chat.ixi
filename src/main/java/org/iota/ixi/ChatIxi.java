package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.model.Message;
import org.iota.ixi.model.MessageBuilder;
import org.iota.ixi.utils.KeyManager;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Filter;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.after;
import static spark.Spark.get;

public class ChatIxi extends IxiModule {

    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private final String username;
    private final String userid;
    private final org.iota.ixi.utils.KeyPair keyPair;
    private Set<String> contacts = new HashSet<>();
    private GossipFilter gossipFilter = new GossipFilter();

    public static void main(String[] args) {
        new ChatIxi(args.length > 0 ? args[0] : "anonymous");
    }

    public ChatIxi(String username) {
        super("chat.ixi");
        this.username = username;
        this.keyPair = KeyManager.loadKeyPair();
        this.userid = Message.generateUserid(keyPair.getPublicAsString());
        contacts.add(userid);
        System.out.println("Waiting for Ict to connect ...");
    }

    @Override
    public void onIctConnect(String name) {

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        get("/init", (request, response) -> {
            synchronized(this) {
                messages.add(new Message());
                setGossipFilter(gossipFilter);
            }
            return "";
        });

        get("/addContact/:userid", (request, response) -> {
            contacts.add(request.params(":userid"));
            return "";
        });

        get("/addChannel/:channel", (request, response) -> {
            // addChannel/ is called N times in a very short interval (one time for each channel). Therefore the gossip filter will be changed N times.
            // Due to the delay of setGossipFilter(), it is important to ensure that setGossipFilter() is called in the correct order.
            // Otherwise the newest GossipFilter with N channels might be replaced by an older GossipFilter with L channels (L<N).
            // This would result in missing channels. The synchronized block ensures the correct order.

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channel = request.params(":channel");
                gossipFilter.watchAddress(channel);
                setGossipFilter(gossipFilter);

                Set<Transaction> transactions = findTransactionsByAddress(channel);
                List<Transaction> orderedTransactions = new LinkedList<>(transactions);
                Collections.sort(orderedTransactions, (tx1, tx2) -> Long.compare(tx1.issuanceTimestamp, tx2.issuanceTimestamp));
                for(Transaction transaction : orderedTransactions)
                    addTransactionToQueue(transaction);
            }
            return "";
        });

        get("/getMessage/", (request, response) -> {
            JSONArray array = new JSONArray();
            synchronized (messages) {
                do {
                    array.put(messages.take().toJSON());
                }
                while (!messages.isEmpty() && array.length() < 100);
            }
            return array.toString();
        });

        get("/getOnlineUsers", (request, response) -> {
            return getOnlineUsers().toString();
        });

        get("/submitMessage/:channel/", (request, response) -> {
            String channel = request.params(":channel");
            String message = request.queryParams("message");
            submitMessage(channel, message);
            return "";
        });

        System.out.println("Connected!");
    }

    public JSONObject getOnlineUsers() {
        Set<Transaction> recentLifeSigns = new HashSet<>();
        for(int i = 0; i < 30; i++) {
            String lifeSignTag = calcLifeSignTag(System.currentTimeMillis()-120000*i);
            recentLifeSigns.addAll(findTransactionsByTag(lifeSignTag));
        }

        JSONObject onlineUsers = new JSONObject();
        for(Transaction transaction : recentLifeSigns)
            try {
                Message message = new Message(transaction, contacts, userid);
                if(onlineUsers.has(message.userid) && onlineUsers.getJSONObject(message.userid).getLong("timestamp") > message.timestamp)
                    continue;

                JSONObject onlineUser = new JSONObject();
                onlineUser.put("is_trusted", contacts.contains(userid));
                onlineUser.put("username", message.username);
                onlineUser.put("timestamp", message.timestamp);
                onlineUsers.put(message.userid, onlineUser);
            } catch (Throwable t) { }

        return onlineUsers;
    }
    private static String calcLifeSignTag(long unixMs) {
        long minuteIndex = unixMs/1000/160;
        String prefix = "LIFESIGN9";
        return prefix + Trytes.fromNumber(BigInteger.valueOf(minuteIndex), Transaction.Field.TAG.tryteLength - prefix.length());
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
        builder.tag = calcLifeSignTag(System.currentTimeMillis());
        Transaction transaction = builder.build();
        submit(transaction);
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        addTransactionToQueue(event.getTransaction());
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        addTransactionToQueue(event.getTransaction());
    }

    public void addTransactionToQueue(Transaction transaction) {
        try {
            Message message = new Message(transaction, contacts, userid);
            if(message.message.length() > 0)
                messages.add(message);
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace();
        }
    }

}
