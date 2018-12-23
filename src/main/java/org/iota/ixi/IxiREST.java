package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.get;

public class IxiREST extends IxiModule {

    private GossipFilter gossipFilter = new GossipFilter();
    private BlockingQueue<Transaction> messages = new LinkedBlockingQueue<>();

    public static final String ADDRESS = "IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999";
    public static final String NAME = "chat.ixi";
    public static String USERNAME;

    public IxiREST(String username) {
        super(NAME);
        USERNAME = username;
    }

    @Override
    public void onIctConnect(String name) {

        setGossipFilter(gossipFilter);

        get("/addChannel/:channel", (request, response) -> {
            gossipFilter.getWatchedAddresses().add(request.params(":channel"));
            return "";
        });

        get("/getMessage/", (request, response) -> {
            Transaction t = messages.take();
            JSONObject o = new JSONObject(t.decodedSignatureFragments);
            return o.toString();
        });

        get("/submitMessage/:channel/:message", (request, response) -> {

            String message = request.params(":message");
            String channel = request.params(":channel");

            TransactionBuilder b = new TransactionBuilder();
            b.address = channel;

            JSONObject o = new JSONObject();

            o.accumulate("username", USERNAME);
            o.accumulate("message",message);
            o.accumulate("timestamp", LocalDateTime.now());
            b.asciiMessage(message);

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
    public void onTransactionSubmitted(GossipSubmitEvent event) { ; }

}
