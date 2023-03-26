package com.example.wifidirect;


import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_URI = "file_url";
    //public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_MEMBER_ADDRESS = "cl_host";
    public static final String EXTRAS_GROUP_MEMBER_PORT = "go_port";
    public static final String EXTRAS_FILE_TYPE = "file_type";
    public static final String EXTRAS_FILE_PATH = "file_path";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_URI);
            String host = intent.getExtras().getString(EXTRAS_GROUP_MEMBER_ADDRESS);
            host = host.substring(1);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_MEMBER_PORT);

            try {
                Log.d(WifiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                //socket连接
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WifiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                String fileType = intent.getExtras().getString(EXTRAS_FILE_TYPE);
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;

                // Set up the key and the cipher
                Key key = new SecretKeySpec("1234567890abcdef".getBytes(), "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, key);

                String filePath = intent.getExtras().getString(EXTRAS_FILE_PATH);
                String newpath = context.getExternalFilesDir("received")+"/wifip2pshared-" + System.currentTimeMillis()+"."+fileType;
                File encrypt_File = new File(newpath);
                encrypt_File.createNewFile();

                //is = cr.openInputStream(Uri.parse(fileUri));
                FileInputStream in = null;
                try{
                    in = new FileInputStream(filePath);
                }catch (FileNotFoundException e){
                    in = (FileInputStream) cr.openInputStream(Uri.parse(fileUri));
                }
                FileOutputStream out = new FileOutputStream(encrypt_File);

                // Encrypt the file
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(cipher.update(buffer, 0, length));
                }
                out.write(cipher.doFinal());

                // Close the streams
                in.close();
                out.close();
                Uri newUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.wifidirect.fileprovider",
                        encrypt_File);

                try {
                    //is = cr.openInputStream(Uri.parse(fileUri));
                    is = cr.openInputStream(newUri);
                } catch (FileNotFoundException e) {
                    Log.d(WifiDirectActivity.TAG, e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream, fileType);
                //stream.write("end".getBytes());
                Log.d(WifiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WifiDirectActivity.TAG, e.getMessage());
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
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
}
