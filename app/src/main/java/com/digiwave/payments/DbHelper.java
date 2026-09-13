package com.digiwave.payments;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB="digiwave_payments.db";
    public DbHelper(Context c){ super(c, DB, null, 2); }

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,fee REAL NOT NULL,currency TEXT NOT NULL,billing_day INTEGER NOT NULL,next_due TEXT NOT NULL,notes TEXT,active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT,client_id INTEGER NOT NULL,amount REAL NOT NULL,currency TEXT NOT NULL,paid_at TEXT NOT NULL,note TEXT,FOREIGN KEY(client_id) REFERENCES clients(id))");
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        // Schema is compatible with v1. Keep data intact.
    }

    public long addClient(String name,String phone,double fee,String currency,int billingDay,String nextDue,String notes){
        ContentValues v=new ContentValues(); v.put("name",name);v.put("phone",phone);v.put("fee",fee);v.put("currency",currency);v.put("billing_day",billingDay);v.put("next_due",nextDue);v.put("notes",notes);v.put("active",1);
        return getWritableDatabase().insert("clients",null,v);
    }

    public void updateClient(long id,String name,String phone,double fee,String currency,int billingDay,String nextDue,String notes){
        ContentValues v=new ContentValues();v.put("name",name);v.put("phone",phone);v.put("fee",fee);v.put("currency",currency);v.put("billing_day",billingDay);v.put("next_due",nextDue);v.put("notes",notes);
        getWritableDatabase().update("clients",v,"id=?",new String[]{String.valueOf(id)});
    }

    public Cursor client(long id){ return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE id=?",new String[]{String.valueOf(id)}); }
    public void setActive(long id,boolean active){ ContentValues v=new ContentValues();v.put("active",active?1:0);getWritableDatabase().update("clients",v,"id=?",new String[]{String.valueOf(id)}); }
    public void deleteClient(long id){ getWritableDatabase().delete("payments","client_id=?",new String[]{String.valueOf(id)});getWritableDatabase().delete("clients","id=?",new String[]{String.valueOf(id)}); }

    public void addPayment(long clientId,double amount,String currency,String note){
        ContentValues v=new ContentValues();v.put("client_id",clientId);v.put("amount",amount);v.put("currency",currency);v.put("paid_at",LocalDateTime.now().toString());v.put("note",note);getWritableDatabase().insert("payments",null,v);
    }

    public double paidSinceDue(long clientId,String due){
        LocalDate d=LocalDate.parse(due); LocalDate periodStart=d.withDayOfMonth(1);
        Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE client_id=? AND paid_at>=?",new String[]{String.valueOf(clientId),periodStart.atStartOfDay().toString()});
        double v=0; if(c.moveToFirst()) v=c.getDouble(0); c.close(); return v;
    }

    public Cursor paymentsForClient(long clientId){
        return getReadableDatabase().rawQuery("SELECT * FROM payments WHERE client_id=? ORDER BY paid_at DESC",new String[]{String.valueOf(clientId)});
    }

    public void rollNextDue(long id,String currentDue){
        LocalDate d=LocalDate.parse(currentDue).plusMonths(1);
        ContentValues v=new ContentValues();v.put("next_due",d.toString());getWritableDatabase().update("clients",v,"id=?",new String[]{String.valueOf(id)});
    }

    public Cursor clients(){ return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE active=1 ORDER BY next_due ASC,name ASC",null); }
    public Cursor inactiveClients(){ return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE active=0 ORDER BY name ASC",null); }
    public Cursor searchClients(String q, boolean includeInactive){
        String like="%"+q+"%";
        if(includeInactive) return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE name LIKE ? OR phone LIKE ? ORDER BY active DESC,next_due ASC,name ASC",new String[]{like,like});
        return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE active=1 AND (name LIKE ? OR phone LIKE ?) ORDER BY next_due ASC,name ASC",new String[]{like,like});
    }
    public Cursor overdueOrDue(int daysAhead){
        String limit=LocalDate.now().plusDays(daysAhead).toString();
        return getReadableDatabase().rawQuery("SELECT * FROM clients WHERE active=1 AND next_due<=? ORDER BY next_due ASC",new String[]{limit});
    }
}
