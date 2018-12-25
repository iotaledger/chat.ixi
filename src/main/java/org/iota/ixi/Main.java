package org.iota.ixi;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.Ict;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Properties;

/**
 * This class is just for testing your IXI, so you don't have to run Ict manually.
 * */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        new ChatIxi("Your_Name");

        Properties properties = new Properties();
        properties.ixiEnabled = true;
        properties.ixis.add("chat.ixi");
        new Ict(properties);
    }
}
