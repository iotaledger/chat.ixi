package org.iota.ixi.model;

import org.iota.ixi.utils.KeyPair;
import org.json.JSONException;

public class MessageBuilder {

    public String username;
    public String message;
    public String channel;
    public KeyPair keyPair;

    public Message build() throws JSONException {
        return new Message(this);
    }
}
