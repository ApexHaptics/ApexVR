package com.example.chris.apexvr;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

/**
 * Created by Chris on 3/19/2017.
 */

public class DataSaver {

    public DataSaver(Context context){

        Notification.Builder notificationBuilder = new Notification.Builder(context);

        Intent result = new Intent(context,VRActivity.class);
        PendingIntent intent = PendingIntent.getActivity(context,0,result,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(Icon.createWithResource(context,R.mipmap.file_icon),"Save file",intent);

        notificationBuilder.addAction(actionBuilder.build());

    }
}
