package hopscotch.messages;

import java.io.Serializable;

public class Payload implements Serializable {
    public PayloadEncryption encryption;
    public PayloadType type;
    public byte[] content;
}
