package cs3220.project.wifidirectp2p;

import java.util.HashMap;

public class PeerCache {

    class Peer {
        //TODO - add info here that needs to be stored about peers
    }

    private static final PeerCache ourInstance = new PeerCache();

    public static PeerCache getInstance() {
        return ourInstance;
    }

    // <MAC Address, Peer info>
    private HashMap<String, Peer> peerCache;

    private PeerCache() {
        peerCache = new HashMap<>();
    }

    public HashMap<String, Peer> getPeerCache() {
        return peerCache;
    }
}
