package hopscotch.communication;

import java.security.PublicKey;
import java.util.ArrayList;

import javax.crypto.SecretKey;

import hopscotch.encryption.EncryptionManager;
import hopscotch.encryption.DataEncryptor;
import hopscotch.messages.*;

public class CommunicationManager {

    private EncryptionManager encryptionManager;

    public CommunicationManager() {
        encryptionManager = new EncryptionManager();
    }

    //1. Requester sends out plaintext: [requester public key, string search]
    //2. Responder sends RSA encrypted (with requester's public key):
        //[plaintext(responder's public key), encrypted(matching file metadata)]
    //3. Requester sends RSA encrypted (with responder's public key):
        //[plaintext(requester's public key), encrypted(download confirmation, metadata for file they want + new AES key]
    //4. Responder sends AES encrypted (with AES key and IV provided by requester):
        //[encrypted(file)]
    //5. Both parties discard keyBundles

    //create a packet for each out transaction. Load serialized payloads into the packet.
    //Serialize the packet. That byte stream is ready for shipment.

    //outgoing Requester messages ---------------------------------------------------------

    //Requester setups up an initial connection and reaches out into oblivion.
    public byte[] genSearchStream(String search) {
        PublicKey mySearchPublicKey = encryptionManager.requestSetup();
        Packet searchPacket = new Packet(mySearchPublicKey, null);

        //Search payload: clear, search request, string search
        Payload searchPayload = new Payload();
        searchPayload.encryption = PayloadEncryption.CLEAR;
        searchPayload.type = PayloadType.SEARCH_REQUEST;
        searchPayload.content = Serializer.serialize(search);
        searchPacket.appendPayload(searchPayload);

        return Serializer.serialize(searchPacket);
    }

    //Once the Requester has selected which file they want to download from all returned
    //search results, send out a confirmation packet that includes AES negotiation.
    public byte[] genConfirmDownloadStream(PublicKey toWho, String fileName, int fileSize) {
        Packet confirmPacket = new Packet(encryptionManager.getMyPublicRSAKey(toWho), toWho);

        //confirm Payload: RSA encrypted, AES negotiation, encrypted(true)
        Payload confirmPayload = new Payload();
        confirmPayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        confirmPayload.type = PayloadType.AES_NEGOTIATION;
        confirmPayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(true));
        confirmPacket.appendPayload(confirmPayload);

        //filename payload: RSA encrypted, AES negotiation, encrypted(fileName)
        Payload fileNamePayload = new Payload();
        fileNamePayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        fileNamePayload.type = PayloadType.AES_NEGOTIATION;
        fileNamePayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(fileName));
        confirmPacket.appendPayload(fileNamePayload);

        //file size payload: RSA encrypted, AES negotiation, encrypted(fileSize)
        Payload fileSizePayload = new Payload();
        fileSizePayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        fileSizePayload.type = PayloadType.AES_NEGOTIATION;
        fileNamePayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(fileSize));
        confirmPacket.appendPayload(fileSizePayload);

        //generate AES key for the download
        SecretKey ourKey = encryptionManager.genAESTransaction(toWho);

        //AES key payload: RSA encrypted, AES negotiation, encrypted(our shared key)
        Payload AESKeyPayload = new Payload();
        AESKeyPayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        AESKeyPayload.type = PayloadType.AES_NEGOTIATION;
        AESKeyPayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(ourKey));
        confirmPacket.appendPayload(AESKeyPayload);

        return Serializer.serialize(confirmPacket);
    }

    //Outgoing Responder messages ---------------------------------------------------------

    //Once a peer has received an initial search, they become a Responder and
    //return search results.
    private byte[] genSearchResponseStream(PublicKey toWho, String matchedFileName, int fileSize) {
        Packet searchRPacket = new Packet(encryptionManager.getMyPublicRSAKey(toWho), toWho);

        //filename payload: RSA encrypted, search response, encrypted(fileName)
        Payload fileNamePayload = new Payload();
        fileNamePayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        fileNamePayload.type = PayloadType.SEARCH_RESPONSE;
        fileNamePayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(matchedFileName));
        searchRPacket.appendPayload(fileNamePayload);

        //file size payload: RSA encrypted, search response, encrypted(fileSize)
        Payload fileSizePayload = new Payload();
        fileSizePayload.encryption = PayloadEncryption.RSA_ENCRYPTED;
        fileSizePayload.type = PayloadType.SEARCH_RESPONSE;
        fileNamePayload.content = DataEncryptor.encryptRSAMessage(toWho, Serializer.serialize(fileSize));
        searchRPacket.appendPayload(fileSizePayload);

        return Serializer.serialize(searchRPacket);
    }

    //Once a Responder has been given confirmation as well as an AES key from the
    //Requester, they need to send over the AES-encrypted file.
    private byte[] genFileTransferStream(PublicKey toWho, byte[] theFile) {
        Packet filePacket = new Packet(encryptionManager.getMyPublicRSAKey(toWho), toWho);

        //file payload: AES encrypted, download chunk, encrypted(theFile)
        Payload filePayload = new Payload();
        filePayload.encryption = PayloadEncryption.AES_ENCRYPTED;
        filePayload.type = PayloadType.DOWNLOAD_CHUNK;
        filePayload.content = DataEncryptor.encryptAESMessage(encryptionManager.getOurSharedAESKey(toWho), theFile);
        filePacket.appendPayload(filePayload);

        return Serializer.serialize(filePacket);
    }

    //Incoming messages ---------------------------------------------------------

    public void decodeStream(byte[] receivedByteStream) {
        Packet incomingPacket = Serializer.deserialize(receivedByteStream);
        PublicKey receivedFrom = incomingPacket.getSender();
        ArrayList<Payload> data = incomingPacket.getPayloads();
        Payload firstPayload = data.get(0);
        if (firstPayload.type == PayloadType.SEARCH_REQUEST || encryptionManager.currentlyTalkingTo(receivedFrom)) {
            byte[] outMessage;
            switch (firstPayload.type) {
                //Responder gets first touch from requester's search.
                case SEARCH_REQUEST:
                    encryptionManager.responseSetup(receivedFrom);
                    String theirSearch = Serializer.deserialize(firstPayload.content);
                    //INSERT FIND-MATCHING-FILE LOGIC.
                    //FOR EACH MATCHED FILE, CALL:
                    String matchedName = "BLOOPERS FROM THE MOVIE UP";
                    int matchedSize = 50; //byte size of the matched file
                    outMessage = genSearchResponseStream(receivedFrom, matchedName, matchedSize);
                    //INSERT NETWORKING LOGIC TO PROPEL THE OUT MESSAGE TO ALL NEARBY PEERS.
                    break;
                //Requester receives search response from a Responder.
                case SEARCH_RESPONSE:
                    encryptionManager.requestFinishSetup(receivedFrom);
                    String theirResponse = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), firstPayload.content));
                    int responseSize = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), data.get(1).content));
                    //APPEND (receivedFrom, theirResponse, responseSize) INTO A GLOBAL LIST OF FILES TO CHOOSE FROM.
                    break;
                //Responder receives AES negotiation / download confirmation from Requester.
                case AES_NEGOTIATION:
                    boolean download = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), firstPayload.content));
                    String fileTheyWant = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), data.get(1).content));
                    int fileSize = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), data.get(2).content));
                    SecretKey ourSharedKey = Serializer.deserialize(DataEncryptor.decryptRSAMessage(encryptionManager.getMyPrivateRSAKey(receivedFrom), data.get(3).content));
                    encryptionManager.setOurSharedAESKey(receivedFrom, ourSharedKey);
                    if (download) {
                        //FIND THE FILE THEY WANT WITH THE MATCHING FILE NAME AND SIZE
                        String matchedFile = "Pretend this is a file.";
                        outMessage = genFileTransferStream(receivedFrom, Serializer.serialize(matchedFile));
                        //INSERT NETWORKING LOGIC TO PROPEL THE OUT MESSAGE TO ALL NEARBY PEERS.
                    } else {
                        //They don't want to download the file-- kill the whole transaction.
                        encryptionManager.destroyKeyBundle(receivedFrom);
                    }
                    break;
                //Final stage of transaction: this is when the Requester receives the AES-encrypted
                //file from the Responder. Then the transaction is over.
                case DOWNLOAD_CHUNK:
                    String theFile = Serializer.deserialize(DataEncryptor.decryptAESMessage(encryptionManager.getOurSharedAESKey(receivedFrom), data.get(0).content));
                    encryptionManager.destroyKeyBundle(receivedFrom);
                    break;
            }
        }
    }
}
