package org.iota.ict.ixi;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.ixi.model.Credentials;
import org.iota.ict.ixi.model.Message;
import org.iota.ict.ixi.model.MessageBuilder;
import org.iota.ict.ixi.utils.FileOperations;
import org.iota.ict.ixi.utils.KeyManager;
import org.iota.ict.ixi.utils.KeyPair;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.utils.Trytes;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.*;

public class ChatIxi extends IxiModule {

    private static final HashMap<String, String> channelByAddress = new HashMap<>();
    private static final HashMap<String, String> addressByChannel = new HashMap<>();
    final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

    private final String userid;
    private final Credentials credentials;
    private final KeyPair keyPair;
    private final Set<String> channelNames;
    private final Set<String> contacts;
    private GossipFilter gossipFilter = new GossipFilter();

    private static final java.io.File CHANNELS_FILE = new java.io.File("channels.txt");
    private static final java.io.File CONTACTS_FILE = new java.io.File("contacts.txt");
    private static final java.io.File CONFIG_FILE = new java.io.File("chat.cfg");

    private int historySize = 100;

    public ChatIxi(Ixi ixi) {

        super(ixi);

        this.keyPair = KeyManager.loadKeyPair();
        this.userid = Message.generateUserid(keyPair.getPublicKeyAsString());
        this.contacts = loadContacts(CONTACTS_FILE);
        this.channelNames = loadChannels(CHANNELS_FILE);
        this.credentials = loadCredentials();

        storeChannels();
        contacts.add(userid);

        ixi.addGossipListener(new GossipListener() {
            @Override
            public void onGossipEvent(GossipEvent event) {
                if(gossipFilter.passes(event.getTransaction()))
                    addTransactionToQueue(event.getTransaction());
            }
        });

    }

    @Override
    public void run() {

        staticFiles.externalLocation("web/");

        before((Filter) (request, response) -> {
            String queryPassword = request.queryParams("password");

            if (queryPassword == null || !queryPassword.equals(credentials.getPassword())) {
                halt(401, "Access denied: password incorrect.");
            }
        });

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        post("/init", (request, response) -> {
            synchronized(this) {
                try { historySize = Integer.parseInt(request.queryParams("history_size")); } catch (Throwable t) { ; }
                messages.add(new Message());
                for(String channel : channelNames) {
                    String channelAddress = deriveChannelAddressFromName(channel);
                    gossipFilter.watchAddress(channelAddress);
                    pullChannelHistory(channelAddress);
                }
            }
            // delay web app so that multiple messages are already queued and can be submitted bundled once web app requests messages
            Thread.sleep(100);
            return new JSONArray(channelNames).toString();
        });

        post("/addContact/:userid", (request, response) -> {
            contacts.add(request.params(":userid"));
            storeContacts();
            return "";
        });

        post("/removeContact/:userid", (request, response) -> {
            contacts.remove(request.params(":userid"));
            storeContacts();
            return "";
        });

        post("/removeChannel/", (request, response) -> {

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channelName = request.queryParams("name");
                String channelAddress = deriveChannelAddressFromName(channelName);

                Set<String> channelNamesToRemove = new HashSet<>();
                for(String c : channelNames)
                    if(deriveChannelAddressFromName(c).equals(channelAddress))
                        channelNamesToRemove.add(c);
                channelNames.removeAll(channelNamesToRemove);
                storeChannels();

                gossipFilter.unwatchAddress(channelAddress);
                return "";
            }
        });

        post("/addChannel/", (request, response) -> {
            // Due to the delay of setGossipFilter(), it is important to ensure that setGossipFilter() is called in the correct order.
            // Otherwise the newest GossipFilter with N channels might be replaced by an older GossipFilter with L channels (L<N).
            // This would result in missing channels. The synchronized block ensures the correct order.

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channelName = request.queryParams("name");
                addChannel(channelName);
                return "";
            }
        });

        post("/getMessage/", (request, response) -> {
            JSONArray array = new JSONArray();
            synchronized (messages) {
                do {
                    array.put(messages.take().toJSON());
                }
                while (!messages.isEmpty() && array.length() < 100);
            }
            return array.toString();
        });

        post("/getOnlineUsers", (request, response) -> {
            return getOnlineUsers().toString();
        });

        post("/submitMessage/:channel/", (request, response) -> {
            String channel = request.params(":channel");
            String message = request.queryParams("message");
            submitMessage(channel, message);
            return "";
        });

        System.out.println("CHAT.ixi is now running on port "+spark.Spark.port()+". Open web/index.html in your web browser to access the chat.");

    }

    protected void addChannel(String channelName) {
        channelNames.add(channelName);
        storeChannels();

        String channelAddress = deriveChannelAddressFromName(channelName);
        gossipFilter.watchAddress(channelAddress);
        pullChannelHistory(channelAddress);
    }

    private void pullChannelHistory(String address) {
        Set<Transaction> transactions = ixi.findTransactionsByAddress(address);
        List<Transaction> orderedTransactions = new LinkedList<>(transactions);
        Collections.sort(orderedTransactions, (tx1, tx2) -> Long.compare(tx1.issuanceTimestamp, tx2.issuanceTimestamp));
        List<Transaction> elements = orderedTransactions.subList(Math.max(0, orderedTransactions.size() - historySize), orderedTransactions.size());
        for(Transaction transaction : elements)
            addTransactionToQueue(transaction);
    }

    public JSONObject getOnlineUsers() {
        Set<Transaction> recentLifeSigns = new HashSet<>();
        for(int i = 0; i < 30; i++) {
            String lifeSignTag = calcLifeSignTag(System.currentTimeMillis()-120000*i);
            recentLifeSigns.addAll(ixi.findTransactionsByTag(lifeSignTag));
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

    private String deriveChannelAddressFromName(String channelName) {
        String trytes = channelName.trim().toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        assert Trytes.isTrytes(trytes);
        String padded = Trytes.padRight(trytes.substring(0, Math.min(81, trytes.length())), 81);
        return getAddressOfChannel(padded);
    }

    public static String calcLifeSignTag(long unixMs) {
        long minuteIndex = unixMs/1000/160;
        String prefix = "LIFESIGN9";
        return prefix + Trytes.fromNumber(BigInteger.valueOf(minuteIndex), Transaction.Field.TAG.tryteLength - prefix.length());
    }

    void submitMessage(String channel, String message) {
        Message toSend = createMessage(channel, message);
        Transaction transaction = toSend.toTransaction();
        if(transaction != null)
            ixi.submit(transaction);
    }

    private Message createMessage(String channel, String message) {
        MessageBuilder builder = new MessageBuilder();
        builder.keyPair = keyPair;
        builder.username = credentials.getUsername();
        builder.message = message;
        builder.channel = channel;
        return builder.build();
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

    private Set<String> loadContacts(File file) {
        try {
            return readStringsFromFile(file);
        } catch (IOException e) {
            System.err.println("Could not read contacts from file " + file.getAbsolutePath() + ": " + e.getMessage());
            return  new HashSet<>();
        }
    }

    private Set<String> loadChannels(File file) {
        final Set<String> defaultChannels = new HashSet<>(Arrays.asList("speculation", "announcements", "ict", "casual"));
        if(!file.exists())
            return defaultChannels;
        try {
            Set<String> userDefinedChannels = readStringsFromFile(file);
            if(userDefinedChannels.isEmpty())
                userDefinedChannels.add("speculation");
            return userDefinedChannels;
        } catch (IOException e) {
            System.err.println("Could not read channels from file " + file.getAbsolutePath() + ": " + e.getMessage());
            return defaultChannels;
        }
    }

    private Set<String> readStringsFromFile(File file) throws IOException {
        Set<String> strings = new HashSet<>();
        if(file.exists()) {
            String channelsFileContent = FileOperations.readFromFile(file);
            strings.addAll(Arrays.asList(channelsFileContent.split(",")));
        }
        return strings;
    }

    private void storeContacts() {
        StringJoiner sj = new StringJoiner(",");
        for(String contact : contacts)
            sj.add(contact);
        FileOperations.writeToFile(CONTACTS_FILE, sj.toString());
    }

    private void storeChannels() {
        StringJoiner sj = new StringJoiner(",");
        for(String channelname : channelNames)
            sj.add(channelname);
        FileOperations.writeToFile(CHANNELS_FILE, sj.toString());
    }

    public static void validateUsername(String username) {
        if(username.length() < 3 || username.length() > 20)
            throw new RuntimeException("Username length must be 3-20.");
        if(!username.matches("^[A-Za-z0-9\\-._]*$"))
            throw new RuntimeException("Username contains illegal characters.");
    }

    public static String getChannelOfAddress(String address) {
        return channelByAddress.containsKey(address) ? channelByAddress.get(address) : Trytes.padRight("", 81);
    }

    public static String getAddressOfChannel(String channel) {
        if(addressByChannel.containsKey(channel))
            return addressByChannel.get(channel);
        String address = IotaCurlHash.iotaCurlHash(channel, channel.length(), 123).substring(0, 81);
        addressByChannel.put(channel, address);
        channelByAddress.put(address, channel);
        return address;
    }

    private Credentials loadCredentials() {
        java.util.Properties p = new java.util.Properties();
        InputStream input = null;
        try {
            if(!CONFIG_FILE.exists())
                throw new RuntimeException(CONFIG_FILE.getAbsolutePath() + ": " +"config file not found");
            input = new FileInputStream(CONFIG_FILE.getName());
            p.load(input);
            String username = p.getProperty("username");
            String password = p.getProperty("password");
            if(username == null || username.length() == 0)
                throw new RuntimeException(CONFIG_FILE.getAbsolutePath() + ": " +"username not set");
            if(password == null || password.length() == 0)
                throw new RuntimeException(CONFIG_FILE.getAbsolutePath() + ": " +"password not set");
            return new Credentials(username, password);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        } finally {
            try { input.close(); } catch(Throwable t) { ; }
        }
    }

}
