package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.get;

public class IxiREST extends IxiModule {

    private Set<String> channels = new HashSet<>();
    private BlockingQueue<Transaction> toPrint = new LinkedBlockingQueue<>();

    public static final String ADDRESS = "IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999";
    public static final String NAME = "chat.ixi";

    public IxiREST() {
        super(NAME);
    }

    @Override
    public void onIctConnect(String name) {
        setGossipFilter(new GossipFilter().watchAddress(ADDRESS));
        System.out.println("Connected!");

        get("/addChannel/:channel", (request, response) -> {
            channels.add(request.params(":channel"));
            return "";
        });

        get("/getMessage/", (request, response) -> {
            Transaction t = toPrint.take();
            return "[" + t.hash + "] " + t.decodedSignatureFragments;
        });

        get("/submitMessage/:channel/:message", (request, response) -> {

            String message = request.params(":message");
            String channel = request.params(":channel");

            TransactionBuilder b = new TransactionBuilder();
            b.address = channel;
            b.asciiMessage(message);
            submit(b.build());

            return "";

        });

    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        toPrint.add(event.getTransaction());
        System.out.println("RECEIVED");
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) { ; }


    public static void main(String[] args) {
        new IxiREST();
    }

}
