package org.iota.ixi;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Trytes;
import org.json.JSONObject;
import spark.Filter;

import java.security.KeyPair;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.after;
import static spark.Spark.get;

public class IxiREST extends IxiModule {

    private GossipFilter gossipFilter = new GossipFilter();
    private BlockingQueue<Transaction> messages = new LinkedBlockingQueue<>();

    public static final String ADDRESS = "IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999";
    public static final String NAME = "chat.ixi";
    public static String USERNAME;

    public static void main(String[] args) throws InterruptedException {
        new IxiREST(args[0]);
    }

    public IxiREST(String username) {
        super(NAME);
        USERNAME = username;
        gossipFilter.watchAddress(ADDRESS);
    }

    @Override
    public void onIctConnect(String name) {

        setGossipFilter(gossipFilter);

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        get("/addChannel/:channel", (request, response) -> {
            String address = channelNameToAddress(request.params(":channel"));
            gossipFilter.watchAddress(address);
            setGossipFilter(gossipFilter);
            return "";
        });

        get("/getMessage/", (request, response) -> {
            Transaction t = messages.take();
            JSONObject o = new JSONObject(t.decodedSignatureFragments).put("timestamp", t.issuanceTimestamp);
            return o.toString();
        });

        get("/submitMessage/:channel/", (request, response) -> {

            String channel = request.params(":channel");
            String message = request.queryParams("message");

            TransactionBuilder b = new TransactionBuilder();
            b.address = channel;

            JSONObject o = new JSONObject();
            o.accumulate("username", USERNAME);
            o.accumulate("message",message);
            o.accumulate("channel",channel);

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

    private String channelNameToAddress(String name) {
        String trytes = Trytes.fromAscii(name);
        return IotaCurlHash.iotaCurlHash(trytes, trytes.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }

}
