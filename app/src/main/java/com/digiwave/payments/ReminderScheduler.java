package com.digiwave.payments;

import android.app.*;
import android.content.*;
import java.util.*;

public class ReminderScheduler {
    public static void schedule(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(c,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        int hour=c.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("reminder_hour",9);
        Calendar cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,hour);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR,1);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }
}
