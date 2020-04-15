package hopscotch.encryption;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

import hopscotch.messages.Packet;
import hopscotch.messages.PayloadEncryption;
import hopscotch.messages.PayloadType;

public class DataEncryptor {
    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;

    public static void main(String[] args) {
        //RSA-2048 example
        String myString = "Hello, my name is Kyle!";
        KeyPair myKeys = genNewRSAKeySet();
        byte[] message = convertStringToBytes(myString);
        byte[] encryptedMessage = encryptRSAMessage(myKeys.getPublic(), message);
        byte[] decryptedMessage = decryptRSAMessage(myKeys.getPrivate(), encryptedMessage);
        String decryptedString = convertBytesToString(decryptedMessage);
        System.out.println("RSA works: " + (decryptedString.equals(myString)));

        //AES-256 GCM example
        SecretKey aesKey = genNewAESKey();
        String myPhrase = "This is great!";
        byte[] phraseBytes = convertStringToBytes(myPhrase);
        byte[] encryptedPhrase = encryptAESMessage(aesKey, phraseBytes);
        byte[] decryptedPhrase = decryptAESMessage(aesKey, encryptedPhrase);
        String newPhrase = convertBytesToString(decryptedPhrase);
        System.out.println("AES Works: " + myPhrase.equals(newPhrase));

        //End-To-End Example---------------------------------------------------------

        //Requester sends out plaintext: [string search, requester public key]
        String search = "CAT MEMES LUL UWU";
        KeyPair requesterKeys = genNewRSAKeySet();
        Packet searchPacket = new Packet(requesterKeys.getPublic(), null);
        searchPacket.appendPayload(PayloadEncryption.CLEAR, PayloadType.SEARCH_REQUEST, convertStringToBytes(search));
        //send [search, requesterPublic] values off

        /*Responder sends RSA encrypted (with requester's public key):
                          [encrypted(matching file metadata), plaintext(responder's public key)]*/
        KeyPair ResponderKeys = genNewRSAKeySet();
        String fileMetaData = "CAT MEMES LUL UWU LOOK AT HIS LIL PAW PAW";
        byte[] encryptedMeta = encryptRSAMessage(searchPacket.getSender(),
                convertStringToBytes(fileMetaData));
        Packet searchResponsePacket = new Packet(ResponderKeys.getPublic(), searchPacket.getSender());
        searchResponsePacket.appendPayload(PayloadEncryption.RSA_ENCRYPTED, PayloadType.SEARCH_RESPONSE, encryptedMeta);
        //send [encryptedMeta, responderPublic] values off

        //At this point, the requester should have numerous incoming responses from various
        //responders. They should decrypt each received metadata to manually select which
        //one they would like to download.
        //for each received response:
        String responseMetaData = convertBytesToString(decryptRSAMessage(requesterKeys.getPrivate(), searchResponsePacket.getPayloads().get(0).content));

        //Requester selects which one they want and:
        /*Requester sends RSA encrypted (with responder's public key):
           [download confirmation, encrypted(metadata for file they want + new AES key + IV]*/
        Boolean download = true;
        SecretKey sharedKey = genNewAESKey();

        Packet downloadRequestPacket = new Packet(requesterKeys.getPublic(), searchResponsePacket.getSender());
        downloadRequestPacket.appendPayload(PayloadEncryption.RSA_ENCRYPTED, PayloadType.AES_NEGOTIATION, encryptRSAMessage(downloadRequestPacket.getReceiver(), sharedKey.getEncoded()));
        downloadRequestPacket.appendPayload(PayloadEncryption.AES_ENCRYPTED, PayloadType.DOWNLOAD_REQUEST, encryptAESMessage(sharedKey, convertStringToBytes(responseMetaData)));
        //send [download, encryptedHandshake] values off

        SecretKey negotiatedSharedKey = deserializeSecretKey(decryptRSAMessage(ResponderKeys.getPrivate(), downloadRequestPacket.getPayloads().get(0).content));
        String fileContent = "Pretend this is a file. Just like this string," +
                "a file can be turned into a byte array. Don't worry.";

        Packet downloadResponsePacket = new Packet(downloadRequestPacket.getReceiver(), downloadRequestPacket.getSender());
        // Response should include file name / metadata and number of expected chunks
        downloadResponsePacket.appendPayload(PayloadEncryption.AES_ENCRYPTED, PayloadType.DOWNLOAD_RESPONSE, encryptAESMessage(negotiatedSharedKey, new byte[] { 1 }));
        downloadResponsePacket.appendPayload(PayloadEncryption.AES_ENCRYPTED, PayloadType.DOWNLOAD_CHUNK, encryptAESMessage(negotiatedSharedKey, convertStringToBytes(fileContent)));
        //send [encryptedFile] values off

        //At this point, the requester receives the AES-encrypted file from the responder.
        //The requester needs to decrypt the file and the transaction is complete.
        byte[] decryptedFile = decryptAESMessage(sharedKey, downloadResponsePacket.getPayloads().get(1).content);
        String rebuiltFile = convertBytesToString(decryptedFile);
        System.out.println("End-To-End works: " + rebuiltFile.equals(fileContent));

        //--------------------------------------------------------
    }

    public static KeyPair genNewRSAKeySet() {
        try {
            KeyPairGenerator newGenerator = KeyPairGenerator.getInstance("RSA");
            newGenerator.initialize(2048, new SecureRandom());
            return newGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        }
        return null;
    }

    public static byte[] encryptRSAMessage(PublicKey myPublicKey, byte[] message) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, myPublicKey, new SecureRandom());
            return cipher.doFinal(message);
        } catch (NoSuchPaddingException b) {
            System.out.println("No such padding exists!");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        } catch (InvalidKeyException i) {
            System.out.println("Invalid key provided!");
        } catch (BadPaddingException k) {
            System.out.println("Bad padding provided");
        } catch (IllegalBlockSizeException d) {
            System.out.println("Illegal block size provided!");
        }
        return null;
    }

    public static byte[] decryptRSAMessage(PrivateKey myPrivateKey, byte[] bytes) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, myPrivateKey, new SecureRandom());
            return cipher.doFinal(bytes);
        } catch (NoSuchPaddingException b) {
            System.out.println("No such padding exists!");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        } catch (InvalidKeyException i) {
            System.out.println("Invalid key provided!");
        } catch (BadPaddingException k) {
            System.out.println("Bad padding provided");
        } catch (IllegalBlockSizeException d) {
            System.out.println("Illegal block size provided!");
        }
        return null;
    }

    public static byte[] convertStringToBytes(String message) {
        return message.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static String convertBytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    //beginning of AES functions

    public static SecretKey genNewAESKey() {
        try {
            KeyGenerator newGenerator = KeyGenerator.getInstance("AES");
            newGenerator.init(AES_KEY_SIZE, new SecureRandom());
            return newGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        }
        return null;
    }

    public static byte[] encryptAESMessage(SecretKey aesKey, byte[] message) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] IV = genNewIV();
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);
            byte[] cipherText = cipher.doFinal(message);

            // Make IV first x bytes of encrypted result
            // IVs can be public
            byte[] result = new byte[IV.length + cipherText.length];
            System.arraycopy(IV, 0, result, 0, IV.length);
            System.arraycopy(cipherText, 0, result, IV.length, cipherText.length);
            return result;
        } catch (NoSuchPaddingException b) {
            System.out.println("No such padding exists!");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        } catch (InvalidKeyException i) {
            System.out.println("Invalid key provided!");
        } catch (BadPaddingException k) {
            System.out.println("Bad padding provided");
        } catch (IllegalBlockSizeException d) {
            System.out.println("Illegal block size provided!");
        } catch (InvalidAlgorithmParameterException l) {
            System.out.println("Invalid IV!");
        }
        return null;
    }

    public static byte[] decryptAESMessage(SecretKey aesKey, byte[] message) {
        try {
            // Extract IV from first x bytes of cipher text
            byte[] IV = Arrays.copyOfRange(message, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(message, GCM_IV_LENGTH, message.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec);
            return cipher.doFinal(cipherText);
        } catch (NoSuchPaddingException b) {
            System.out.println("No such padding exists!");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such algorithm exists!");
        } catch (InvalidKeyException i) {
            System.out.println("Invalid key provided!");
        } catch (BadPaddingException k) {
            System.out.println("Bad padding provided");
        } catch (IllegalBlockSizeException d) {
            System.out.println("Illegal block size provided!");
        } catch (InvalidAlgorithmParameterException l) {
            System.out.println("Invalid IV!");
        }
        return null;
    }

    public static byte[] genNewIV() {
        // IVs should be random and unique for every encryption operation -- even if the message content is the same
        byte[] IV = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);
        return IV;
    }

    public static SecretKey deserializeSecretKey(byte[] key) {
        return new SecretKeySpec(key, 0, key.length, "AES");
    }
}
