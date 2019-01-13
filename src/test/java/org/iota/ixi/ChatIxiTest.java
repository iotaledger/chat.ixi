package org.iota.ixi;

import org.iota.ict.Ict;
import org.iota.ict.ixi.IctProxy;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Trytes;
import org.iota.ixi.chat.ChatIxi;
import org.iota.ixi.chat.model.Message;
import org.junit.Assert;
import org.junit.Test;

public class ChatIxiTest {

    @Test
    public void testMessage() {

        ChatIxi chatIxi = new ChatIxi(new IctProxy(new Ict(new Properties())));

        String original = "Hello World";
        String channel = Trytes.padRight("SPECULATION", 81);

        chatIxi.submitMessage(channel, original);
        chatIxi.addChannel(channel);
        Message message = chatIxi.messages.poll();
        Assert.assertEquals("Failed reading self-submitted message.", original, message.message);


    }
}
