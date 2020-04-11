package hopscotch.encryption;

import java.util.HashMap;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

/*This is the front-end object everyone else should be using. It will manage
encrypting files as well as keeping track of all keys associated with all
peer-to-peer transactions. */

public class EncryptionManager {

    //Each peer has a corresponding Integer ID, and an associated KeyBundle for transactions
    //exclusively with that peer.
    private HashMap<Integer, KeyBundle> keyBundles;

    public EncryptionManager() {
        keyBundles = new HashMap<>();
    }

    //WHEN RYAN MAKES THE MESSAGE STUFF, MAKE WRAPPER FUNCTIONS THAT
    //COMBINE ENCRYPTION ALGORITHMS WITH PACKAGING MESSAGES

    public void destroyBundle(Integer peer) {
        KeyBundle theBundle = keyBundles.get(peer);
        theBundle.eraseAllInfo();
        keyBundles.put(peer, null);
    }

    //GETTERS
    public PrivateKey getMyPrivateRSAKey(Integer peer) {
        return keyBundles.get(peer).getMyPrivateRSAKey();
    }
    public PublicKey getMyPublicRSAKey(Integer peer) {
        return keyBundles.get(peer).getMyPublicRSAKey();
    }
    public PublicKey getTheirPublicRSAKey(Integer peer) {
        return keyBundles.get(peer).getTheirPublicRSAKey();
    }
    public SecretKey getOurSharedAESKey(Integer peer) {
        return keyBundles.get(peer).getOurSharedAESKey();
    }
    public IvParameterSpec getOurSharedAESIV(Integer peer) {
        return keyBundles.get(peer).getOurSharedAESIV();
    }

    //SETTERS
    public void setMyPrivateRSAKey(Integer peer, PrivateKey newKey) {
        keyBundles.get(peer).setMyPrivateRSAKey(newKey);
    }
    public void setMyPublicRSAKey(Integer peer, PublicKey newKey) {
        keyBundles.get(peer).setMyPublicRSAKey(newKey);
    }
    public void setTheirPublicRSAKey(Integer peer, PublicKey newKey) {
        keyBundles.get(peer).setTheirPublicRSAKey(newKey);
    }
    public void setOurSharedAESKey(Integer peer, SecretKey newKey) {
        keyBundles.get(peer).setOurSharedAESKey(newKey);
    }
    public void setOurSharedAESIV(Integer peer, IvParameterSpec newIV) {
        keyBundles.get(peer).setOurSharedAESIV(newIV);
    }

}
