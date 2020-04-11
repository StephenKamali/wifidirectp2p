package hopscotch.encryption;

/*Think of this class as a bundle of all AES and RSA keys
associated with a single peer-to-peer transaction. This bundling
will allow each client to perform numerous transactions simultaneously
with numerous other peers.*/

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class KeyBundle {

    private PrivateKey myPrivateRSAKey;
    private PublicKey myPublicRSAKey;
    private PublicKey theirPublicRSAKey;
    private SecretKey ourSharedAESKey;
    private IvParameterSpec ourSharedAESIV;

    public KeyBundle() {
        super();
    }

    void createRSAKeys() {
        KeyPair myKeys = DataEncryptor.genNewRSAKeySet();
        myPrivateRSAKey = myKeys.getPrivate();
        myPublicRSAKey = myKeys.getPublic();
    }

    void eraseAllInfo() {
        myPrivateRSAKey = null;
        myPublicRSAKey = null;
        theirPublicRSAKey = null;
        ourSharedAESKey = null;
        ourSharedAESIV = null;
    }

    //GETTERS
    PrivateKey getMyPrivateRSAKey() {
        return myPrivateRSAKey;
    }

    PublicKey getMyPublicRSAKey() {
        return myPublicRSAKey;
    }

    PublicKey getTheirPublicRSAKey() {
        return theirPublicRSAKey;
    }

    SecretKey getOurSharedAESKey() {
        return ourSharedAESKey;
    }

    IvParameterSpec getOurSharedAESIV() {
        return ourSharedAESIV;
    }

    //SETTERS
    void setMyPrivateRSAKey(PrivateKey newKey) {
        myPrivateRSAKey = newKey;
    }

    void setMyPublicRSAKey(PublicKey newKey) {
        myPublicRSAKey = newKey;
    }

    void setTheirPublicRSAKey(PublicKey newKey) {
        theirPublicRSAKey = newKey;
    }

    void setOurSharedAESKey(SecretKey newKey) {
        ourSharedAESKey = newKey;
    }

    void setOurSharedAESIV(IvParameterSpec newIV) {
        ourSharedAESIV = newIV;
    }
}
