package com.example.wifidirect;

import android.app.Fragment;
import android.os.Bundle;

import androidx.core.content.FileProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private static WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    static ProgressBar progressBar;
    private Context context;
    public static String fileType;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);
        //进度条
        progressBar = mContentView.findViewById(R.id.progressBarSent);
        //点击“连接”按钮
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                //config.groupOwnerIntent = 15;
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

                //my_ip = ((DeviceListFragment.DeviceActionListener) getActivity()).get_own_ip();
                //设备互联
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);

            }
        });

        //点击“断开”按钮
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });

        //点击“选择文件”按钮（发送方）
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WifiDirectActivity.TAG, "Intent----------- " + uri);
        Map<String,Integer> map = GetMemberIPAsyncTask.map;
        Set<String> set=map.keySet();
        Iterator<String> it=set.iterator();
        while(it.hasNext()){
            //System.out.println(it.next());
            String ip = it.next();
            int Group_Number = map.get(ip);
            if(Group_Number == 0){
                //调用文件传输服务
                Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                //获取文件类型
                String path;
                try {
                    path = FileUtils.getFilePathByUri(context,uri);
                    fileType = path.substring(path.lastIndexOf('.')+1);
                } catch (IOException e) {
                    fileType = "txt";
                }
                //String x = DeviceListFragment.getMAC_info().get(0);
                //my_ip = ((DeviceListFragment.DeviceActionListener) getActivity()).get_own_ip(DeviceListFragment.getMAC_info().get(0));
                //将文件uri、连接设备的信息文件类型传递给服务
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
                //serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_MEMBER_ADDRESS,ip);
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_TYPE, fileType);
                getActivity().startService(serviceIntent);
            }
        }
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
        view.setText("群主 IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && !info.isGroupOwner) {
            //与群主建立socket连接，以便群主获取自己的ip地址
            Intent serviceIntent = new Intent(getActivity(), SocketConnectService.class);
            serviceIntent.setAction(SocketConnectService.ACTION_CONNECT_SOCKET);
            serviceIntent.putExtra(SocketConnectService.EXTRAS_GROUP_OWNER_ADDRESS,info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(SocketConnectService.EXTRAS_GROUP_OWNER_PORT, 8989);
            getActivity().startService(serviceIntent);

            //非群主准备进行文件接收，为避免UI阻塞卡顿，进入后台异步任务，本质为多线程
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();

        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            new GetMemberIPAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
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
        //显示设备详细信息
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
                Log.d(WifiDirectActivity.TAG, "Server: Socket opened");
                //socket成功建立连接前会一直阻塞在此处
                Socket client = serverSocket.accept();
                Log.d(WifiDirectActivity.TAG, "Server: connection done");


                //Log.d(WifiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                //输入流前十个字节为文件类型
                byte buf[] = new byte[10];
                inputstream.read(buf);
                int i;
                for (i = 0; i < 10; i++) {
                    if(buf[i] == 0){
                        break;
                    }
                }
                byte buf2[] = new byte[i];
                for (int j = 0; j < i; j++) {
                    buf2[j] = buf[j];
                }
                String file_type = new String(buf2, StandardCharsets.UTF_8);
                //接收方在本机创建文件
                final File f = new File(context.getExternalFilesDir("received"),
                        "wifip2pshared-" + System.currentTimeMillis()+"."+file_type);

                //int x = file_type.length();

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                //写入刚刚创建的文件
                origincopyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WifiDirectActivity.TAG, e.getMessage());
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
                intent.setDataAndType(fileUri, "*/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //查看接收到的文件
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

    //发送方uri输入流写入输出流（接收方socket)
    public static boolean copyFile(InputStream inputStream, OutputStream out, String filetype) {
        byte buf[] = new byte[1024];
        int len;
        int sentSize=0;
        float perc;
        int times = 0;
        try {
            int totSize = inputStream.available();
            while ((len = inputStream.read(buf)) != -1) {
                //首先发送文件类型
                if(times == 0){
                    byte[] bytes = new byte[10];
                    byte[] bytes2 = filetype.getBytes();
                    for(int i=0;i<10;i++){
                        if(i<filetype.length()){
                            bytes[i] = bytes2[i];
                        }else{
                            bytes[i] = 0;
                        }
                    }
                    out.write(bytes);
                    times++;
                }
                out.write(buf, 0, len);
                sentSize += len;
                perc = (sentSize*100.0f)/totSize;
                Log.d("Sent ",perc+"%");
                //progressDialogSent.setMessage("We have sent "+(sentSize/totSize)*100+"% of the file");
                progressBar.setProgress((int)(perc));
            }
            Log.d("Sent ","100%");
            out.close();
            inputStream.close();
            //progressDialogSent.dismiss();
        } catch (IOException e) {
            Log.d(WifiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    //由接收方socket输出流写入文件
    public static boolean origincopyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        int sentSize=0;
        float perc;
        //progressDialogSent.show();
        //progressDialogSent.setCancelable(false);
        try {
            int totSize = inputStream.available();
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                sentSize += len;
                perc = (sentSize*100.0f)/totSize;
                Log.d("Sent ",perc+"%");
                progressBar.setProgress((int)(perc));
            }
            out.close();
            inputStream.close();
            //progressDialogSent.dismiss();
        } catch (IOException e) {
            Log.d(WifiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    /**
     * A simple server socket that accepts connection from group members and then get their ip
     * address and group number.
     */
    public static class GetMemberIPAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        //public static ArrayList<String> ip_list = new ArrayList<>();
        public static Map<String,Integer> map = new HashMap<>();

        /**
         * @param context
         * @param statusText
         */
        public GetMemberIPAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8989);
                //socket成功建立连接前会一直阻塞在此处
                while(true){
                    Socket client = serverSocket.accept();
                    Log.d(WifiDirectActivity.TAG, "Server: connection done");

                    InputStream inputstream = client.getInputStream();
                    String mem_ip = ""+client.getInetAddress();
                    //ip_list.add(mem_ip);
                    map.put(mem_ip,0);
                    client.close();
                }
            }catch (IOException e) {
                Log.e(WifiDirectActivity.TAG, e.getMessage());
                //return null;
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {}

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Waiting for group member ip");
        }

    }
}