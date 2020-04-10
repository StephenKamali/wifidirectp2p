package cs3220.project.wifidirectp2p;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceListFragment.DeviceActionListener {
    public static final String TAG = "wifidirectdemo";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;

    private ArrayList<WifiP2pDevice> unbroadcastedPeers;
    private boolean broadcastPeers;
    private boolean waitingForPeers;
    private Button connectToNetworkButton;
    private Button refreshPeersButton;
    private Button broadcastButton;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if  (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);
        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WiFiDirectActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }

        unbroadcastedPeers = new ArrayList<WifiP2pDevice>();
        broadcastPeers = false;
        waitingForPeers = false;

        connectToNetworkButton = (Button) findViewById(R.id.connect_to_network_button);
        connectToNetworkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToNetworkButton.setVisibility(View.GONE);
                refreshPeersButton.setVisibility(View.VISIBLE);
                broadcastButton.setVisibility(View.VISIBLE);

                //Disconnect from any previous groups
                if (manager != null) {
                    final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                            .findFragmentById(R.id.frag_list);
                    if (fragment.getDevice() == null
                            || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                        disconnect();
                    }
                }

                becomeGroupOwner();
            }
        });

        refreshPeersButton = (Button) findViewById(R.id.refresh_peers_button);
        refreshPeersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePeerDiscovery();
            }
        });

        broadcastButton = (Button) findViewById(R.id.broadcast_button);
        broadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastToPeers();
            }
        });
    }
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    public void handleBroadcast() {
        //if (!broadcastPeers || !waitingForPeers)
        if (!broadcastPeers)
            return;

        waitingForPeers = false;

        final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);

        Log.d("HSCOTCH", "Unbroadcasted peers contains " + unbroadcastedPeers.toString());

        if (!unbroadcastedPeers.isEmpty()) {
            final WifiP2pDevice nextPeer = unbroadcastedPeers.remove(0);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = nextPeer.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    new CountDownTimer(10000, 1000) {
                        @Override
                        public void onTick(long l) {
                            Log.d("HSCOTCH", "Getting new status in " + l);
                        }

                        @Override
                        public void onFinish() {
                            /*int ind = fragment.getPeers().indexOf(nextPeer);
                            if (ind != -1) {
                                Log.d("HSCOTCH", "new peer status is " + fragment.getPeers().get(ind).status);
                                if (fragment.getPeers().get(ind).status != WifiP2pDevice.CONNECTED) {
                                    cancelDisconnect();
                                    initiatePeerDiscovery();
                                }
                            }

                             */
                            cancelDisconnect();
                            waitingForPeers = true;
                            initiatePeerDiscovery();

                            // Give peer discovery a chance to discover as much as it can
                            new CountDownTimer(5000, 1000) {

                                @Override
                                public void onTick(long millisUntilFinished) {
                                }

                                @Override
                                public void onFinish() {
                                    handleBroadcast();
                                }
                            }.start();
                        }
                    }.start();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("HSCOTCH", "Unable to start connection to " + nextPeer.deviceName + "(error " + reason + ")");
                }
            });
        } else {
            Log.d("HSCOTCH", "No more peers to broadcast to, becoming group owner");
            broadcastPeers = false;
            cancelDisconnect();
            becomeGroupOwner();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void becomeGroupOwner() {
        Log.d("HSCOTCH", "Becoming group owner");
        manager.createGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("HSCOTCH", "Connected as group owner");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("HSCOTCH", "Failed to become group owner");
                Toast.makeText(WiFiDirectActivity.this, "Failed to become group owner.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initiatePeerDiscovery() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                    Toast.LENGTH_SHORT).show();
        }
        //final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
        //        .findFragmentById(R.id.frag_list);
        //fragment.onInitiateDiscovery();
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void broadcastToPeers() {
        Log.d("HSCOTCH", "Beginning broadcast to peers");

        if (broadcastPeers) {
            handleBroadcast();
        } else {

            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            unbroadcastedPeers.addAll(fragment.getPeers());


            Log.d("HSCOTCH", "Leaving current group to broadcast");
            //cancelDisconnect();
            disconnect();

            broadcastPeers = true;
            waitingForPeers = true;
            initiatePeerDiscovery();

            // Give peer discovery a chance to discover as much as it can
            new CountDownTimer(5000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    handleBroadcast();
                }
            }.start();
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }
    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }
            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }
            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }
        });
    }
    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {
                manager.cancelConnect(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}