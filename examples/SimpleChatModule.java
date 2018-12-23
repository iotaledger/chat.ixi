package org.iota.ict.ixi;

import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.util.Scanner;

public class SimpleChatModule extends IxiModule implements Runnable {

    public SimpleChatModule() {
        super("chat.ixi");
     }

    @Override
    public void onIctConnect(String name) {
        setGossipFilter(new GossipFilter().watchAddress("IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999"));
        System.out.println("Connected!");
        new Thread(this).start();
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        System.out.println(event.getTransaction().decodedSignatureFragments);
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) { ; }

    @Override
    public void run() {

        Scanner in = new Scanner(System.in);

        System.out.print("Your name: ");
        String name = in.nextLine();
        System.out.println();

        do {

            String message = in.nextLine();
            TransactionBuilder t = new TransactionBuilder();
            t.address = "IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999";
            t.asciiMessage(name + ": " + message);
            submit(t.build());

        } while(true);

    }

    public static void main(String[] args) {
        new SimpleChatModule();
    }

}
