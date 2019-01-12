package org.iota.ixi;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.model.Message;
import org.junit.Assert;
import org.junit.Test;

public class ChatIxiTest {

    @Test
    public void testMessage() {

        Properties properties = new Properties();
        properties.ixiEnabled = true;
        new Ict(properties);

        ChatIxi chatIxi = new ChatIxi(properties.name, "anonymous", "");

        String original = "Hello World";
        String channel = Trytes.padRight("SPECULATION", 81);

        chatIxi.submitMessage(channel, original);
        chatIxi.addChannel(channel);
        Message message = chatIxi.messages.poll();
        Assert.assertEquals("Failed reading self-submitted message.", original, message.message);
    }
}
