package org.iota.ixi;

import org.iota.ict.ixi.IxiModule;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.model.Message;
import org.iota.ixi.model.MessageBuilder;
import org.iota.ixi.utils.FileOperations;
import org.iota.ixi.utils.KeyManager;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Filter;

import java.io.IOException;
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
    private Set<String> contacts;
    private GossipFilter gossipFilter = new GossipFilter();

    private static final java.io.File CONTACTS_FILE = new java.io.File("contacts.txt");

    public static void main(String[] args) {
        if(args.length == 0) {
            System.err.println("WARNING: No arguments were passed to IXI module.");
            System.out.println("You can start chat.ixi like this:    java -jar chat.ixi-{VERSION}.jar {ICT_NAME} {USERNAME}");
        }
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nPlease enter the name of your ICT (Default = "+ (new Properties()).name +"):\n> ");
        String ictName = scanner.nextLine();

        System.out.println("Please enter your username:\n> ");
        String username = scanner.nextLine();

        new ChatIxi(ictName, username);
    }

    public ChatIxi(String ictName, String username) {
        super("chat.ixi", ictName);
        this.username = username;
        this.keyPair = KeyManager.loadKeyPair();
        this.userid = Message.generateUserid(keyPair.getPublicAsString());
        this.contacts = loadContacts();
        contacts.add(userid);
        init();
        System.out.println("CHAT.ixi is now running on port "+spark.Spark.port()+". Open web/index.html in your web browse to access the chat.");
    }

    public void init() {

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
            storeContacts();
            return "";
        });

        get("/removeContact/:userid", (request, response) -> {
            contacts.remove(request.params(":userid"));
            storeContacts();
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
                onlineUser.put("is_trusted", contacts.contains(message.userid));
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
        }
    }

    private Set<String> loadContacts() {
        Set<String> contacts = new HashSet<>();
        try {
            if(CONTACTS_FILE.exists()) {
                String contactsFileContent = FileOperations.readFromFile(CONTACTS_FILE);
                contacts.addAll(Arrays.asList(contactsFileContent.split(",")));
            }
        } catch (IOException e) {
            System.err.println("Could not read contacts from file " + CONTACTS_FILE.getAbsolutePath() + ": " + e.getMessage());
        }
        return contacts;
    }

    private void storeContacts() {
        StringJoiner sj = new StringJoiner(",");
        for(String contact : contacts)
            sj.add(contact);
        FileOperations.writeToFile(CONTACTS_FILE, sj.toString());
    }

    public static void validateUsername(String username) {
        if(username.length() < 3 || username.length() > 20)
            throw new RuntimeException("Username length must be 3-20.");
        if(!username.matches("^[A-Za-z0-9\\-._]*$"))
            throw new RuntimeException("Username contains illegal characters.");
    }
}
