package org.iota.ixi;

import org.iota.ict.Ict;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Properties;

/**
 * This class is just for testing your IXI, so you don't have to run Ict manually.
 * */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        new IxiREST("Your_Name");

        Properties p1 = new Properties();
        p1.port = 1341;
        p1.ixiEnabled = true;
        p1.ixis.add("chat.ixi");

        Properties p2 = new Properties();
        p2.port = 1342;

        Ict ict1 = new Ict(p1);
        Ict ict2 = new Ict(p2);

        ict1.neighbor(ict2.getAddress());
        ict2.neighbor(ict1.getAddress());

        String name = "Samuel";
        String message = "hallo test";
        TransactionBuilder t = new TransactionBuilder();
        t.address = "IXI9CHAT9999999999999999999999999999999999999999999999999999999999999999999999999";
        t.asciiMessage(name + ": " + message);
        ict2.submit(t.build());

    }
}
