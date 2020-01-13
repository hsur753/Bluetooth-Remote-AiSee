package org.ahlab.aisee.shared.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class BTMessageBody implements Serializable {

    static final long serialVersionUID = 123456789123456789L;

    private String messageID;
    private String data;

    public BTMessageBody(String id){
        messageID = id;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setData(String d) {
        data = d;
    }

    public String getData() {
        return data;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(this);
        return b.toByteArray();
    }

    public static BTMessageBody deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return (BTMessageBody) o.readObject();
    }
}
