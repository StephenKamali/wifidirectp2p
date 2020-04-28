package cs3220.project.wifidirectp2p;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;

import hopscotch.communication.CommunicationManager;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    static AlertDialog alertDialog;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Status update")
                .create();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);
            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });
        return mContentView;
    }

    private void sendPacket(byte[] packet) {
        // We have a packet to send. Transfer it to group owner i.e peer using
        // PacketTransferService.
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + packet.length + " bytes");
        //Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), PacketTransferService.class);
        serviceIntent.setAction(PacketTransferService.ACTION_SEND_PACKET);
        serviceIntent.putExtra(PacketTransferService.EXTRAS_PACKET_DATA, packet);
        serviceIntent.putExtra(PacketTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(PacketTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));
        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
             //       .execute();
            new PacketAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                   .execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.

            //sendPacket("HOWDY WORLD. THIS IS A TEST PACKET.");
            sendPacket(((WiFiDirectActivity)getActivity()).outPacket);

            //mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            //((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
            //        .getString(R.string.client_text));
        }
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }
    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private TextView statusText;
        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(context.getExternalFilesDir("received"),
                        "wifip2pshared-" + System.currentTimeMillis()
                                + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                File recvFile = new File(result);
                Uri fileUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.wifidirect.fileprovider",
                        recvFile);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "image/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
        }
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class PacketAsyncTask extends AsyncTask<Void, Void, byte[]> {
        private Context context;
        private TextView statusText;
        private Activity mainActivity;
        /**
         * @param context
         * @param statusText
         */
        public PacketAsyncTask(Activity mainActivity, View statusText) {
            this.context = mainActivity;
            this.statusText = (TextView) statusText;
            this.mainActivity = mainActivity;
        }
        @Override
        protected byte[] doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                Log.d("HSCOTCH", "inet address = " + client.getInetAddress().getAddress());
                Log.d("HSCOTCH", "local address = " + client.getLocalAddress().getAddress());
                InputStream inputstream = client.getInputStream();

                byte[] packet = receivePacket(inputstream);
                Log.d("HSCOTCH", "Packet contents: " + new String(packet));

                serverSocket.close();

                return packet;
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(byte[] result) {
            if (result != null) {
                statusText.setText("Packet copied - " + new String(result));
                Log.d("HSCOTCH", "packet copied");
                ((WiFiDirectActivity)mainActivity).commManager.decodeStream(result, new CommunicationManager.SearchReqCallback() {
                    @Override
                    public void onReadyToBroadcast(byte[] outMessage) {
                        ((WiFiDirectActivity) mainActivity).outPacket = outMessage;
                        new AlertDialog.Builder(mainActivity)
                                .setTitle("Status update")
                                .setMessage("Received a matching search request. \n" +
                                        "Click BROADCAST to send matching info back to network.")
                                .create().show();
                        Log.d("HSCOTCH", "Ready to broadcast ");
                    }
                }, new CommunicationManager.SearchRespCallback() {
                    @Override
                    public void onResponseReceived(PublicKey key, String fileName, int fileSize) {
                        ((WiFiDirectActivity) mainActivity).outPacket =
                                ((WiFiDirectActivity) mainActivity).commManager.genConfirmDownloadStream(key, fileName, fileSize);
                        new AlertDialog.Builder(mainActivity)
                                .setTitle("Status update")
                                .setMessage("Received response: " + fileName + "\n" +
                                        "Click BROADCAST to send download request.")
                                .create().show();
                        Log.d("HSCOTCH", "Received response " + fileName);
                    }
                }, new CommunicationManager.NegotiationCallback() {
                    @Override
                    public void onDownloadRequest(byte[] outMessage) {
                        ((WiFiDirectActivity) mainActivity).outPacket = outMessage;
                        new AlertDialog.Builder(mainActivity)
                                .setTitle("Status update")
                                .setMessage("Received a download request.\n" +
                                        "Press BROADCAST to continue.")
                                .create().show();
                        Log.d("HSCOTCH", "Ready to broadcast ");
                    }
                }, new CommunicationManager.DownloadCallback() {
                    @Override
                    public void onReceiveFile(String theFile) {
                        ((WiFiDirectActivity) mainActivity).outPacket = null;
                        new AlertDialog.Builder(mainActivity)
                                .setTitle("Status update")
                                .setMessage("Received file: " + theFile + "\n" +
                                        "Transaction finished, key destroyed.")
                                .create().show();
                        Log.d("HSCOTCH", "Ready to broadcast ");
                    }
                });
            }
        }
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    // This is a really simple way to receive the info, and store it in a small buffer
    public static byte[] receivePacket(InputStream inputStream) {
        /*
        byte buf[] = new byte[1024];
        int len;
        try {
            len = inputStream.read(buf);
            Log.d("HSCOTCH", "Received " + len + " bytes");
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
        }*/

        byte buf[] = new byte[1024];
        int len;
        ArrayList<Byte> out = new ArrayList<>();
        try {
            while ((len = inputStream.read(buf)) != -1) {
                //out.write(buf, 0, len);
                for (int i = 0; i < len; i++) {
                    out.add(buf[i]);
                }
            }
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
        }
        byte[] outArray = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) {
            outArray[i] = out.get(i);
        }
        return outArray;
    }
}