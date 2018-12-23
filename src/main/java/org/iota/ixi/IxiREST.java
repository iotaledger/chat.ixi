package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.get;

public class IxiREST extends IxiModule implements Runnable {

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
        new Thread(this).start();
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        toPrint.add(event.getTransaction());
        System.out.println("RECEIVED");
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) { ; }

    @Override
    public void run() {

        get("/getMessage/", (request, response) -> {
            Transaction t = toPrint.take();
            return "[" + t.hash + "] " + t.decodedSignatureFragments;
        });

        get("/submitMessage/:message", (request, response) -> {

            String message = request.params(":message");

            TransactionBuilder b = new TransactionBuilder();
            b.address = ADDRESS;
            b.asciiMessage(message);
            submit(b.build());

            return "";

        });

    }

    public static void main(String[] args) {
        new IxiREST();
    }

}
