package org.iota.ixi.chat;

import com.iota.curl.IotaCurlHash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.context.ConfigurableIxiContext;
import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipFilter;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.IOHelper;
import org.iota.ixi.chat.model.Credentials;
import org.iota.ixi.chat.model.Message;
import org.iota.ixi.chat.model.MessageBuilder;
import org.iota.ixi.chat.utils.KeyManager;
import org.iota.ixi.chat.utils.KeyPair;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.chat.utils.PasswordGenerator;
import org.iota.ixi.chat.utils.aes_key_length.AESKeyLengthFix;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Filter;
import spark.Service;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static spark.Spark.*;

public class ChatIxi extends IxiModule {

    private static final HashMap<String, String> channelByAddress = new HashMap<>();
    private static final HashMap<String, String> addressByChannel = new HashMap<>();
    public final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

    private final ChatContext context;
    private final String userid;
    private Credentials credentials;
    private final KeyPair keyPair;
    private GossipFilter gossipFilter = new GossipFilter();
    private Service service = Service.ignite();

    private static final Logger LOGGER = LogManager.getLogger("CHAT_IXI");
    public static final java.io.File DIRECTORY = new java.io.File("modules/chat.ixi/");
    private static final java.io.File WEB_DIRECTORY = new java.io.File("web/dist/modules/CHAT.ixi/");

    private int historySize = 100;

    static {
        AESKeyLengthFix.apply();
    }

    public ChatIxi(Ixi ixi) {

        super(ixi);

        this.keyPair = KeyManager.loadKeyPair();
        this.userid = Message.generateUserid(keyPair.getPublicKeyAsString());

        context = new ChatContext();
        context.applyConfiguration();

        // context.store(); TODO
        context.contacts.add(userid);

        GossipListener listener = new GossipListener.Implementation() {
            @Override
            public void onReceive(GossipEvent event) {
                if (gossipFilter.passes(event.getTransaction()))
                    addTransactionToQueue(event.getTransaction());
            }
        };

        ixi.addListener(listener);
    }

    @Override
    public void install() {
        if(startedFromJar())
            try {
                if(!DIRECTORY.exists())
                    DIRECTORY.mkdirs();
                if (!WEB_DIRECTORY.exists())
                    IOHelper.extractDirectoryFromJarFile(ChatIxi.class, "web/", WEB_DIRECTORY.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    private static boolean startedFromJar() {
        String pathToChatIXI = ChatIxi.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return pathToChatIXI.endsWith(".jar");
    }

    @Override
    public void uninstall() {
        IOHelper.deleteRecursively(DIRECTORY);
        IOHelper.deleteRecursively(WEB_DIRECTORY);
    }

    @Override
    public void run() {

        service.port(2019);

        service.before((Filter) (request, response) -> {
            if(request.requestMethod().toLowerCase().equals("post")) {
                String queryPassword = request.queryParams("password");
                if (queryPassword == null || !queryPassword.equals(credentials.getPassword())) {
                    halt(401, "Access denied: password incorrect.");
                }
            }
        });

        service.after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        service.post("/init/", (request, response) -> {
            try {
                synchronized(this) {
                    try { historySize = Integer.parseInt(request.queryParams("history_size")); } catch (Throwable t) { ; }
                    messages.add(new Message());
                    for(String channel : context.channelNames) {
                        String channelAddress = deriveChannelAddressFromName(channel);
                        gossipFilter.watchAddress(channelAddress);
                        pullChannelHistory(channelAddress);
                    }
                }
                // delay web app so that multiple messages are already queued and can be submitted bundled once web app requests messages
                Thread.sleep(100);
                return new JSONArray(context.channelNames).toString();
            } catch (Throwable t) {
                t.printStackTrace();
                return new JSONObject().put("error", t.getMessage()).toString();
            }
        });

        service.post("/addContact/:userid", (request, response) -> {
            context.contacts.add(request.params(":userid"));
            // context.store(); TODO
            return "";
        });

        service.post("/removeContact/:userid", (request, response) -> {
            context.contacts.remove(request.params(":userid"));
            // context.store(); TODO
            return "";
        });

        service.post("/removeChannel/", (request, response) -> {

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channelName = request.queryParams("name");
                removeChannel(channelName);
                return "";
            }
        });

        service.post("/addChannel/", (request, response) -> {
            // Due to the delay of setGossipFilter(), it is important to ensure that setGossipFilter() is called in the correct order.
            // Otherwise the newest GossipFilter with N channels might be replaced by an older GossipFilter with L channels (L<N).
            // This would result in missing channels. The synchronized block ensures the correct order.

            synchronized (this) { // synchronized necessary for correct order of setGossipFilter()
                String channelName = request.queryParams("name");
                addChannel(channelName);
                return "";
            }
        });

        service.post("/getMessage/", (request, response) -> {
            JSONArray array = new JSONArray();
            synchronized (messages) {
                do {
                    array.put(messages.take().toJSON());
                }
                while (!messages.isEmpty() && array.length() < 100);
            }
            return array.toString();
        });

        service.post("/getOnlineUsers/", (request, response) -> {
            return getOnlineUsers().toString();
        });

        service.post("/submitMessage/:channel/", (request, response) -> {
            String channel = request.params(":channel");
            String message = request.queryParams("message");
            submitMessage(channel, message);
            return "";
        });

        LOGGER.info("CHAT.ixi is now running (port "+service.port()+"). Open it from your Ict web GUI: host:2187/modules/chat.ixi");
    }

    private void removeChannel(String channelName) {
        String channelAddress = deriveChannelAddressFromName(channelName);

        Set<String> channelNamesToRemove = new HashSet<>();
        for(String c : context.channelNames)
            if(deriveChannelAddressFromName(c).equals(channelAddress))
                channelNamesToRemove.add(c);
        context.channelNames.removeAll(channelNamesToRemove);
        //context.store(); TODO

        gossipFilter.unwatchAddress(channelAddress);
    };

    public void addChannel(String channelName) {
        context.channelNames.add(channelName);
        //context.store(); TODO

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
                Message message = new Message(transaction, context.contacts, userid);
                if(onlineUsers.has(message.userid) && onlineUsers.getJSONObject(message.userid).getLong("timestamp") > message.timestamp)
                    continue;

                JSONObject onlineUser = new JSONObject();
                onlineUser.put("is_trusted", context.contacts.contains(message.userid));
                onlineUser.put("username", message.username);
                onlineUser.put("timestamp", message.timestamp);
                onlineUsers.put(message.userid, onlineUser);
            } catch (Throwable t) { }

        return onlineUsers;
    }

    private String deriveChannelAddressFromName(String channelName) {
        String trytes = channelName.trim().toUpperCase().replaceAll("[^a-zA-Z9]", "");
        assert Trytes.isTrytes(trytes);
        String padded = Trytes.padRight(trytes.substring(0, Math.min(81, trytes.length())), 81);
        return getAddressOfChannel(padded);
    }

    public static String calcLifeSignTag(long unixMs) {
        long minuteIndex = unixMs/1000/160;
        String prefix = "LIFESIGN9";
        return prefix + Trytes.fromNumber(BigInteger.valueOf(minuteIndex), Transaction.Field.TAG.tryteLength - prefix.length());
    }

    public void submitMessage(String channel, String message) {
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
            Message message = new Message(transaction, context.contacts, userid);
            if(message.message.length() > 0)
                messages.add(message);
        } catch (Throwable t) {
            LOGGER.warn("Message in transaction "+transaction.hash+" rejected: " + t.getMessage());
        }
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

    @Override
    public void onTerminate() {
        service.stop();
    }

    @Override
    public IxiContext getContext() {
        return context;
    }

    private class ChatContext extends ConfigurableIxiContext {

        private final Set<String> channelNames = new HashSet<>();
        private Set<String> contacts = new HashSet<>();

        private static final String FIELD_PASSWORD = "password";
        private static final String FIELD_USERNAME = "username";
        private static final String FIELD_CHANNELS = "channels";
        private static final String FIELD_CONTACTS = "contacts";

        private ChatContext() {
            super(new JSONObject()
                    .put(FIELD_USERNAME, "Anonymous")
                    .put(FIELD_PASSWORD, PasswordGenerator.random())
                    .put(FIELD_CHANNELS, new JSONArray("casual,ict,announcements,speculation".split(",")))
                    .put(FIELD_CONTACTS, new JSONArray().put(userid)));
        }

        @Override
        protected void validateConfiguration(JSONObject newConfiguration) {
            validatePassword(newConfiguration);
            validateUsername(newConfiguration);
            validateChannels(newConfiguration);
            validateContacts(newConfiguration);
        }

        @Override
        protected void applyConfiguration() {
            credentials = new Credentials(configuration.getString(FIELD_USERNAME), configuration.getString(FIELD_PASSWORD));
            JSONArray channelArray = configuration.getJSONArray(FIELD_CHANNELS);
            JSONArray contactArray = configuration.getJSONArray(FIELD_CONTACTS);
            applyChannels(channelArray);
            contacts = new HashSet<>();
            for(Object contact : contactArray.toList())
                contacts.add((String)contact);
        }

        private void applyChannels(JSONArray channelArray) {
            HashSet<Object> newChannelNames = new HashSet<>(channelArray.toList());
            // remove old channels
            for(String channelName : new HashSet<>(channelNames))
                if(!newChannelNames.contains(channelName))
                    removeChannel(channelName);
            // add new channels
            for(int i = 0; i < channelArray.length(); i++)
                if(!channelNames.contains(channelArray.getString(i)))
                    addChannel(channelArray.getString(i));
        }

        @Override
        public JSONObject getConfiguration() {
            return new JSONObject()
                .put(FIELD_USERNAME, credentials.getUsername())
                .put(FIELD_PASSWORD, credentials.getPassword())
                .put(FIELD_CONTACTS, new JSONArray(contacts))
                .put(FIELD_CHANNELS, new JSONArray(channelNames));
        }

        private void validatePassword(JSONObject newConfiguration) {
            if(!newConfiguration.has(FIELD_PASSWORD))
                throw new IllegalPropertyException(FIELD_PASSWORD, "not defined");
            if(!(newConfiguration.get(FIELD_PASSWORD) instanceof String))
                throw new IllegalPropertyException(FIELD_PASSWORD, "not a string");
            if(newConfiguration.getString(FIELD_PASSWORD).length() < 8)
                throw new IllegalPropertyException(FIELD_PASSWORD, "too short");
        }

        private void validateChannels(JSONObject newConfiguration) {
            if(!newConfiguration.has(FIELD_CHANNELS))
                throw new IllegalPropertyException(FIELD_CHANNELS, "not defined");
            if(!(newConfiguration.get(FIELD_CHANNELS) instanceof JSONArray))
                throw new IllegalPropertyException(FIELD_CHANNELS, "not an array");
            JSONArray array = newConfiguration.getJSONArray(FIELD_CHANNELS);
            for(int i = 0; i < array.length(); i++)
                if(!(array.get(i) instanceof String))
                    throw new IllegalPropertyException(FIELD_CHANNELS, "array element at index "+i+" is not a string");
        }

        private void validateContacts(JSONObject newConfiguration) {
            if(!newConfiguration.has(FIELD_CONTACTS))
                throw new IllegalPropertyException(FIELD_CONTACTS, "not defined");
            if(!(newConfiguration.get(FIELD_CONTACTS) instanceof JSONArray))
                throw new IllegalPropertyException(FIELD_CONTACTS, "not an array");
            JSONArray array = newConfiguration.getJSONArray(FIELD_CONTACTS);
            for(int i = 0; i < array.length(); i++)
                if(!(array.get(i) instanceof String))
                    throw new IllegalPropertyException(FIELD_CONTACTS, "array element at index "+i+" is not a string");
                else if(!array.getString(i).matches("^[A-Z9]{0,8}$"))
                    throw new IllegalPropertyException(FIELD_CONTACTS, "array element at index "+i+" is not a valid user id");
        }

        private void validateUsername(JSONObject newConfiguration) {
            if(!newConfiguration.has(FIELD_USERNAME))
                throw new IllegalPropertyException(FIELD_USERNAME, "not defined");
            if(!(newConfiguration.get(FIELD_USERNAME) instanceof String))
                throw new IllegalPropertyException(FIELD_USERNAME, "not a string");
            ChatIxi.validateUsername(newConfiguration.getString(FIELD_USERNAME));
        }

        private class IllegalPropertyException extends IllegalArgumentException {
            private IllegalPropertyException(String field, String cause) {
                super("Invalid property '"+field+"': " + cause + ".");
            }
        }
    }
}
