package hopscotch.encryption;

import java.util.HashMap;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

/*This is the front-end object everyone else should be using. It will manage
encrypting files as well as keeping track of all keys associated with all
peer-to-peer transactions. */

public class EncryptionManager {

    //Each peer should be identified by their public key,
    // and their key maps to an associated KeyBundle for transactions
    //exclusively with that peer.
    private HashMap<PublicKey, KeyBundle> keyBundles;
    private PublicKey requestRSAPublicKey;
    private PrivateKey requestRSAPrivateKey;

    public EncryptionManager() {
        requestRSAPublicKey = null;
        keyBundles = new HashMap<>();
    }

    //Before I've made contact with a peer, use my own public key for our
    //transaction as the key to the keyBundle.
    public PublicKey requestSetup() {
        KeyPair myKeys = DataEncryptor.genNewRSAKeySet();
        requestRSAPrivateKey = myKeys.getPrivate();
        requestRSAPublicKey = myKeys.getPublic();
        return myKeys.getPublic();
    }

    //When the Responder gives me their public key, update our keyBundle. Call
    //this for every person that responds.
    public void requestFinishSetup(PublicKey newPeer) {
        KeyBundle newBundle = new KeyBundle();
        newBundle.setMyPublicRSAKey(requestRSAPublicKey);
        newBundle.setMyPrivateRSAKey(requestRSAPrivateKey);
        newBundle.setTheirPublicRSAKey(newPeer);
        keyBundles.put(newPeer, newBundle);
    }

    //Before I respond to someone's request, setup a keyBundle for the exchange.
    public void responseSetup(PublicKey newPeer) {
        KeyBundle newBundle = new KeyBundle();
        newBundle.createRSAKeys();
        newBundle.setTheirPublicRSAKey(newPeer);
        keyBundles.put(newPeer, newBundle);
    }

    //Create an AES key for our transaction and add it to our keyBundle.
    public SecretKey genAESTransaction(PublicKey peer) {
        SecretKey newKey =  DataEncryptor.genNewAESKey();
        setOurSharedAESKey(peer, newKey);
        return newKey;
    }

    //Checks to see if I've been previously communicating with a peer for the sake
    //of a transaction between us.
    public boolean currentlyTalkingTo(PublicKey peer) {
        return keyBundles.containsKey(peer);
    }

    //-------------------------------------------------------------------------------

    //GETTERS
    public PrivateKey getMyPrivateRSAKey(PublicKey peer) {
        return keyBundles.get(peer).getMyPrivateRSAKey();
    }
    public PublicKey getMyPublicRSAKey(PublicKey peer) {
        return keyBundles.get(peer).getMyPublicRSAKey();
    }
    public PublicKey getTheirPublicRSAKey(PublicKey peer) {
        return keyBundles.get(peer).getTheirPublicRSAKey();
    }
    public SecretKey getOurSharedAESKey(PublicKey peer) {
        return keyBundles.get(peer).getOurSharedAESKey();
    }
    public IvParameterSpec getOurSharedAESIV(PublicKey peer) {
        return keyBundles.get(peer).getOurSharedAESIV();
    }

    //SETTERS
    public void setMyPrivateRSAKey(PublicKey peer, PrivateKey newKey) {
        keyBundles.get(peer).setMyPrivateRSAKey(newKey);
    }
    public void setMyPublicRSAKey(PublicKey peer, PublicKey newKey) {
        keyBundles.get(peer).setMyPublicRSAKey(newKey);
    }
    public void setTheirPublicRSAKey(PublicKey peer, PublicKey newKey) {
        keyBundles.get(peer).setTheirPublicRSAKey(newKey);
    }
    public void setOurSharedAESKey(PublicKey peer, SecretKey newKey) {
        keyBundles.get(peer).setOurSharedAESKey(newKey);
    }
    public void setOurSharedAESIV(PublicKey peer, IvParameterSpec newIV) {
        keyBundles.get(peer).setOurSharedAESIV(newIV);
    }

    public void destroyKeyBundle(PublicKey peer) {
        KeyBundle theBundle = keyBundles.get(peer);
        theBundle.eraseAllInfo();
        keyBundles.remove(peer);
    }
}
