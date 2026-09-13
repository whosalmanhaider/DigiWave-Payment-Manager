package com.digiwave.payments;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.Build;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CH="payments_due";
    @Override public void onReceive(Context c,Intent intent){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){ nm.createNotificationChannel(new NotificationChannel(CH,"Payment Reminders",NotificationManager.IMPORTANCE_HIGH)); }
        DbHelper db=new DbHelper(c); Cursor x=db.overdueOrDue(3); int id=1000;
        while(x.moveToNext()){
            long cid=x.getLong(x.getColumnIndexOrThrow("id")); String name=x.getString(x.getColumnIndexOrThrow("name"));
            double fee=x.getDouble(x.getColumnIndexOrThrow("fee")); String curr=x.getString(x.getColumnIndexOrThrow("currency")); String due=x.getString(x.getColumnIndexOrThrow("next_due"));
            double paid=db.paidSinceDue(cid,due); double left=Math.max(0,fee-paid); if(left<=0) continue;
            LocalDate d=LocalDate.parse(due); long diff=ChronoUnit.DAYS.between(LocalDate.now(),d);
            String state=diff<0?"Overdue":(diff==0?"Due Today":"Due in "+diff+" day"+(diff==1?"":"s"));
            Intent open=new Intent(c,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(c,(int)cid,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=new Notification.Builder(c,CH).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(name+" • "+state).setContentText(curr+" "+String.format("%.0f",left)+" remaining. Message client for payment.").setAutoCancel(true).setContentIntent(pi);
            nm.notify(id++,b.build());
        }
        x.close();
    }
}
