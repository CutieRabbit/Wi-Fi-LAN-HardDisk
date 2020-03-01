package com.example.mobileftpserver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class NotificationUtils {

    public final String channelID = "uriah.channelID.01";
    public final String channelDescription = "阿就只是個頻道";
    public final String channelName = "只是頻道";

    public String createNotificationChannel(Context context){
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            return channelID;
        }else{
            return null;
        }
    }

}
