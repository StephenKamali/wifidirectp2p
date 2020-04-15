package hopscotch.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serializer {
    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Java error handling sucks");
        }
    }
    public static <T> T deserialize(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (T) is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Java error handling sucks");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Java error handling sucks");
        }
    }
}
