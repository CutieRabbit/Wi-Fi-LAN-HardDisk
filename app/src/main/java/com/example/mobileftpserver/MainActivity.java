package com.example.mobileftpserver;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    FtpServerFactory serverFactory;
    FtpServer server;
    ListenerFactory listenerFactory;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.WAKE_LOCK",
            "android.permission.CHANGE_WIFI_STATE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void openServer(View view) {

        EditText editText = (EditText) findViewById(R.id.editText2);
        String stringPort = editText.getText().toString();

        int port = Integer.parseInt(stringPort);
        String deviceIP = getDeviceIP();
        String wifiIP = getWifiIP();

        if(wifiIP.equals("0.0.0.0")){
            wifiIP = "不可用";
        }

        if(port >= 65535 || port <= 0) {
            wrongPort();
            return;
        }

        serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(port);
        setConfigurationFile();
        serverFactory.addListener("default", factory.createListener());
        server = serverFactory.createServer();

        try {
            server.start();
        }catch(Exception e){
            e.printStackTrace();
            openServerFailed(e.getMessage());
            return;
        }

        TextView serverPortView = (TextView) findViewById(R.id.ServerPort);
        TextView serverStatus = (TextView) findViewById(R.id.ServerStatus);
        TextView deviceIPView = (TextView) findViewById(R.id.DeviceIP);
        TextView wifiIPView = (TextView) findViewById(R.id.WiFi_IP);
        serverPortView.setText(String.format("伺服器Port: %d", port));
        serverStatus.setText(String.format("伺服器狀態: %s", "開啟"));
        deviceIPView.setText(String.format("設備IP: %s:%d",deviceIP, port));

        if(!wifiIP.equals("不可用")) {
            wifiIPView.setText(String.format("Wi-Fi IP: %s:%d", wifiIP, port));
        }else{
            wifiIPView.setText(String.format("Wi-Fi IP: %s", wifiIP));
        }

        Button startServerButton = (Button) findViewById(R.id.button);
        startServerButton.setEnabled(false);

        sendNotification(port);

    }

    public void wrongPort(){
        AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
        alertBox.setTitle("Port設置錯誤");
        alertBox.setMessage("Port必須介於0至65535");
        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        };
        alertBox.setPositiveButton("OK", OKListener);
        alertBox.show();
    }

    public void openServerFailed(String message){
        AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
        alertBox.setTitle("Server開啟失敗");
        alertBox.setMessage(message);
        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        };
        alertBox.setPositiveButton("OK", OKListener);
        alertBox.show();
    }

    public String getWifiIP(){
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(WIFI_SERVICE);
        int intIP = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d", (intIP & 0xff), (intIP >> 8 & 0xff), (intIP >> 16 & 0xff), (intIP >> 24 & 0xff));
        return ipAddress;
    }

    public String getDeviceIP(){
        try{
            List<NetworkInterface> list = Collections.list(NetworkInterface.getNetworkInterfaces());
            for(NetworkInterface intf : list){
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for(InetAddress address : addrs){
                    if(!address.isLoopbackAddress() && !address.getHostAddress().contains(":")){
                        return address.getHostAddress();
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return "Unknown Error";
    }

    public void setConfigurationFile(){
        String path = Environment.getExternalStorageDirectory() + File.separator + "FTPProject" + File.separator;
        try {
            File dictionary = new File(path);
            if (!dictionary.exists()) {
                System.out.println("Create Folder: " + path);
                boolean success = dictionary.mkdirs();
            }
            File file = new File(path + "ftpserver.properties");
            if (!file.exists()) {
                boolean success = file.createNewFile();
            }
            String data = "" +
                    "ftpserver.user.anonymous.userpassword=\n" +
                    "ftpserver.user.anonymous.homedirectory=/mnt/sdcard\n" +
                    "ftpserver.user.anonymous.enableflag=true\n" +
                    "ftpserver.user.anonymous.writepermission=true\n" +
                    "ftpserver.user.anonymous.maxloginnumber=250\n" +
                    "ftpserver.user.anonymous.maxloginperip=250\n" +
                    "ftpserver.user.anonymous.idletime=300\n" +
                    "ftpserver.user.anonymous.uploadrate=10000\n" +
                    "ftpserver.user.anonymous.downloadrate=10000\n";

            PrintWriter writer = new PrintWriter(file);
            writer.println(data);
            writer.close();
            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            userManagerFactory.setFile(file);
            serverFactory.setUserManager(userManagerFactory.createUserManager());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void closeServer(){
        server.stop();
    }

    public void sendNotification(int port){
        NotificationUtils utils = new NotificationUtils();
        String title = "伺服器運作中";
        String text = "運行Port: " + port;
        String id = utils.createNotificationChannel(this);
        NotificationCompat.Builder compat = new NotificationCompat.Builder(this, id);
        compat.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setOngoing(true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(0, compat.build());
    }

}
