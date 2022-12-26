package com.example.wifidirect;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A service that ran by the group owner(server) to accept the connection from group members(client)
 * to get group members' ip address and their group number
 */
public class SocketConnectService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_CONNECT_SOCKET = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_GROUP_NUMBER = "group_number";

    public SocketConnectService(String name) {
        super(name);
    }

    public SocketConnectService() {
        super("SocketConnectService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_CONNECT_SOCKET)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            int GroupNumber = intent.getExtras().getInt(EXTRAS_GROUP_NUMBER);

            try {
                Log.d(WifiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                //MD5加密
                MessageDigest md = MessageDigest.getInstance("MD5");
                String str = "" + GroupNumber;
                // 调用update输入数据:
                md.update(str.getBytes("UTF-8"));
                byte[] result = md.digest();
                //socket连接
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                OutputStream out = socket.getOutputStream();
                out.write(result);

                Log.d(WifiDirectActivity.TAG, "Client socket - " + socket.isConnected());
            } catch (IOException e) {
                Log.e(WifiDirectActivity.TAG, e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}

