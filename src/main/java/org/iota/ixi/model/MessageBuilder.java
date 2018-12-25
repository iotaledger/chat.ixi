package org.iota.ixi.model;

import org.iota.ixi.utils.KeyPair;

public class MessageBuilder {

    public String username;
    public String message;
    public String channel;
    public KeyPair keyPair;

    public Message build() {
        return new Message(this);
    }
}
