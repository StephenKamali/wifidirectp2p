package hopscotch.messages;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;

public class Packet implements Serializable {
    private PublicKey sender;
    private PublicKey receiver;
    private ArrayList<Payload> payloads;

    public Packet(PublicKey sender, PublicKey receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.payloads = new ArrayList<>();
    }

    public PublicKey getSender() { return this.sender; };
    public PublicKey getReceiver() { return this.receiver; };
    public ArrayList<Payload> getPayloads() { return this.payloads; }

    public void appendPayload(PayloadEncryption encryption, PayloadType type, byte[] content) {
        Payload payload = new Payload();
        payload.encryption = encryption;
        payload.type = type;
        payload.content = content;
        this.payloads.add(payload);
    }

    public void appendPayload(Payload payload) {
        this.payloads.add(payload);
    }

    public void reset() {
        this.payloads.clear();
    }
}
