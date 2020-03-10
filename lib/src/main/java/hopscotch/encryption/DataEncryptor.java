package hopscotch.encryption;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class DataEncryptor {

    public static void main(String[] args) {
        //ATTEMPT AT RSA
        try {
            KeyPair keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keys.getPublic());
            String message = "My name is Kyle!";
            byte[] rawData = message.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.doFinal(rawData);
            cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
            byte[] decrypted = cipher.doFinal(encrypted);
            System.out.println("message: " + message);
            System.out.println("encrypted: " + new String(encrypted));
            System.out.println("decrypted: " + new String(decrypted, StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        } catch (NoSuchPaddingException b) {
            System.out.println("No such padding exists!");
        } catch (InvalidKeyException i) {
            System.out.println("Invalid key provided!");
        } catch (BadPaddingException k) {
            System.out.println("Bad padding provided");
        } catch (IllegalBlockSizeException d) {
            System.out.println("Illegal block size provided!");
        }

        //ATTEMPT AT AES-128

    }
}
