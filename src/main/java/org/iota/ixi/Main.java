package org.iota.ixi;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;

/**
 * This class is just for testing your IXI, so you don't have to run Ict manually.
 * */
public class Main {

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.ixiEnabled = true;
        new Ict(properties);

        new ChatIxi(properties.name, "anonymous", "");
    }
}
