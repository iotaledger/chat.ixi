package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

/**
 * This is an example IXI module. Use it as template to implement your own module.
 * Run Main.main() to test it.
 *
 * https://github.com/iotaledger/ixi
 * */
public class Ixi extends IxiModule {

    // TODO rename your IXI
    public static final String NAME = "example.ixi";

    public static void main(String[] args) {
        new Ixi();
    }

    public Ixi() {
        super(NAME);
        System.out.println(NAME + " started, waiting for Ict to connect ...");
        System.out.println("Just add '"+NAME+"' to 'ixis' in your ict.cfg file and restart your Ict.\n");

        // important: do not call any API functions such as 'findTransactionByHash()' before onIctConnect() is called!
    }

    @Override
    public void onIctConnect(String name) {
        System.out.println("Ict '" + name + "' connected, submitting message ...");
        setGossipFilter(new GossipFilter().setWatchingAll(true)); // subscribe to all transactions
        submit("Hello World!");
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {
        System.out.println("Received message:  " + event.getTransaction().decodedSignatureFragments);
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        System.out.println("Submitted message: " + event.getTransaction().decodedSignatureFragments);
    }
}
