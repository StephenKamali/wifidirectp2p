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

    public EncryptionManager() {
        keyBundles = new HashMap<>();
    }

    //WHEN RYAN MAKES THE MESSAGE STUFF, MAKE WRAPPER FUNCTIONS THAT
    //COMBINE ENCRYPTION ALGORITHMS WITH PACKAGING MESSAGES

    public void destroyBundle(PublicKey peer) {
        KeyBundle theBundle = keyBundles.get(peer);
        theBundle.eraseAllInfo();
        keyBundles.put(peer, null);
    }

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
}
