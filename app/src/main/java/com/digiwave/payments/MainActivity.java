package com.digiwave.payments;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class MainActivity extends Activity {
    DbHelper db; LinearLayout root,list; TextView stats, sectionTitle; EditText search;
    int navy=Color.rgb(15,23,42), muted=Color.rgb(100,116,139), bg=Color.rgb(248,250,252), green=Color.rgb(22,163,74), red=Color.rgb(220,38,38), amber=Color.rgb(245,158,11);
    String mode="all";

    @Override public void onCreate(Bundle b){
        super.onCreate(b); db=new DbHelper(this); ReminderScheduler.schedule(this);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);
        build();
    }

    TextView t(String s,int size,int color){ TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,6,0,6);return v; }
    Button btn(String s){ Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b; }
    EditText e(String hint){ EditText e=new EditText(this);e.setHint(hint);e.setPadding(8,18,8,18);return e; }

    void build(){
        ScrollView sv=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,24,28,50);root.setBackgroundColor(bg);sv.addView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.addView(t("DigiWave Solutions",24,navy));brand.addView(t("Client Payment Manager",14,muted));
        Button settings=btn("Settings");top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));top.addView(settings);root.addView(top);settings.setOnClickListener(v->settingsDialog());

        stats=t("",17,navy);stats.setPadding(0,20,0,14);root.addView(stats);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL); Button add=btn("+ Add Client");Button due=btn("Due");Button former=btn("Former");actions.addView(add,new LinearLayout.LayoutParams(0,-2,2));actions.addView(due,new LinearLayout.LayoutParams(0,-2,1));actions.addView(former,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
        add.setOnClickListener(v->addClientDialog()); due.setOnClickListener(v->{mode="due";refresh();}); former.setOnClickListener(v->{mode="former";refresh();});

        search=e("Search client or WhatsApp number");root.addView(search);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){refresh();} public void afterTextChanged(Editable s){}});
        sectionTitle=t("Active Clients",20,navy);sectionTitle.setPadding(0,14,0,6);root.addView(sectionTitle);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list); setContentView(sv); refresh();
    }

    void refresh(){
        list.removeAllViews(); String q=search==null?"":search.getText().toString().trim(); Cursor c;
        if(!q.isEmpty()) c=db.searchClients(q,mode.equals("former")); else if(mode.equals("due")) c=db.overdueOrDue(3); else if(mode.equals("former")) c=db.inactiveClients(); else c=db.clients();
        int total=0,dueCount=0,over=0; double aed=0,pkr=0;
        while(c.moveToNext()){
            total++; long id=c.getLong(c.getColumnIndexOrThrow("id"));String name=c.getString(c.getColumnIndexOrThrow("name"));String phone=c.getString(c.getColumnIndexOrThrow("phone"));double fee=c.getDouble(c.getColumnIndexOrThrow("fee"));String curr=c.getString(c.getColumnIndexOrThrow("currency"));String next=c.getString(c.getColumnIndexOrThrow("next_due"));String notes=c.getString(c.getColumnIndexOrThrow("notes"));int active=c.getInt(c.getColumnIndexOrThrow("active"));
            double paid=db.paidSinceDue(id,next);double left=Math.max(0,fee-paid); LocalDate d=LocalDate.parse(next);long diff=ChronoUnit.DAYS.between(LocalDate.now(),d);
            if(active==1 && left>0&&diff<=0) dueCount++; if(active==1&&left>0&&diff<0) over++; if(active==1){if(curr.equals("AED")) aed+=left; else pkr+=left;}
            addCard(id,name,phone,fee,curr,next,notes,left,diff,active==1);
        }
        c.close();
        sectionTitle.setText(mode.equals("former")?"Former Clients":mode.equals("due")?"Due / Upcoming Payments":"Active Clients");
        stats.setText("Pending  AED "+String.format("%.0f",aed)+"   |   PKR "+String.format("%.0f",pkr)+"\nDue/Overdue: "+dueCount+"   |   Overdue: "+over);
        if(total==0) list.addView(t("No clients found.",15,muted));
    }

    void addCard(long id,String name,String phone,double fee,String curr,String next,String notes,double left,long diff,boolean active){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(24,18,24,18);GradientDrawable gd=new GradientDrawable();gd.setColor(Color.WHITE);gd.setCornerRadius(22);card.setBackground(gd);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,10,0,10);list.addView(card,cp);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);TextView nm=t(name,19,navy);Button menu=btn("⋮");head.addView(nm,new LinearLayout.LayoutParams(0,-2,1));head.addView(menu);card.addView(head);
        String state=!active?"Former Client":left<=0?"Paid":diff<0?"Overdue by "+(-diff)+" days":diff==0?"Due Today":diff<=3?"Due in "+diff+" days":"Next payment in "+diff+" days";
        int stateColor=!active?muted:left<=0?green:(diff<0?red:(diff<=3?amber:muted));card.addView(t(state,14,stateColor));
        card.addView(t(curr+" "+String.format("%.0f",fee)+" monthly  •  Remaining "+curr+" "+String.format("%.0f",left),15,navy));
        card.addView(t("Due: "+next+(phone==null||phone.isEmpty()?"":"  •  "+phone)+(notes==null||notes.isEmpty()?"":"\n"+notes),13,muted));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button wa=btn("WhatsApp");Button pay=btn("Payment");Button history=btn("History");row.addView(wa,new LinearLayout.LayoutParams(0,-2,1));row.addView(pay,new LinearLayout.LayoutParams(0,-2,1));row.addView(history,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);
        wa.setEnabled(active);pay.setEnabled(active);wa.setOnClickListener(v->openWhatsApp(phone,name,curr,left)); pay.setOnClickListener(v->paymentDialog(id,name,curr,left,next)); history.setOnClickListener(v->historyDialog(id,name));
        menu.setOnClickListener(v->clientMenu(id,name,active));
    }

    void addClientDialog(){ clientFormDialog(-1); }
    void editClientDialog(long id){ clientFormDialog(id); }
    void clientFormDialog(long id){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(24,0,24,0);
        EditText name=e("Client Name");EditText phone=e("WhatsApp Number with country code");EditText fee=e("Monthly Fee");fee.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);Spinner cur=new Spinner(this);cur.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"AED","PKR"}));EditText day=e("Billing Day (1-28)");day.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);EditText next=e("Next Due Date (YYYY-MM-DD)");EditText notes=e("Notes (optional)");
        f.addView(name);f.addView(phone);f.addView(fee);f.addView(cur);f.addView(day);f.addView(next);f.addView(notes);
        if(id>0){Cursor c=db.client(id);if(c.moveToFirst()){name.setText(c.getString(c.getColumnIndexOrThrow("name")));phone.setText(c.getString(c.getColumnIndexOrThrow("phone")));fee.setText(String.valueOf(c.getDouble(c.getColumnIndexOrThrow("fee"))));String currency=c.getString(c.getColumnIndexOrThrow("currency"));cur.setSelection(currency.equals("PKR")?1:0);day.setText(String.valueOf(c.getInt(c.getColumnIndexOrThrow("billing_day"))));next.setText(c.getString(c.getColumnIndexOrThrow("next_due")));notes.setText(c.getString(c.getColumnIndexOrThrow("notes")));}c.close();}
        else { LocalDate now=LocalDate.now();day.setText(String.valueOf(Math.min(now.getDayOfMonth(),28)));next.setText(now.toString()); }
        new AlertDialog.Builder(this).setTitle(id>0?"Edit Client":"Add Client").setView(f).setPositiveButton("Save",(d,w)->{
            try{String n=name.getText().toString().trim();if(n.isEmpty())throw new Exception();int bd=Integer.parseInt(day.getText().toString());if(bd<1||bd>28)throw new Exception();double ff=Double.parseDouble(fee.getText().toString());LocalDate nd=LocalDate.parse(next.getText().toString().trim());if(id>0)db.updateClient(id,n,phone.getText().toString().trim(),ff,cur.getSelectedItem().toString(),bd,nd.toString(),notes.getText().toString().trim());else db.addClient(n,phone.getText().toString().trim(),ff,cur.getSelectedItem().toString(),bd,nd.toString(),notes.getText().toString().trim());refresh();}catch(Exception ex){Toast.makeText(this,"Check name, fee, billing day and date",Toast.LENGTH_LONG).show();}
        }).setNegativeButton("Cancel",null).show();
    }

    void paymentDialog(long id,String name,String curr,double left,String due){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,0,24,0);EditText amount=e("Amount received");amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);EditText note=e("Payment note (optional)");box.addView(amount);box.addView(note);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(name+" • "+curr).setMessage("Remaining: "+curr+" "+String.format("%.0f",left)).setView(box).setPositiveButton("Save",null).setNeutralButton("Paid in Full",null).setNegativeButton("Cancel",null).create();
        dlg.setOnShowListener(x->{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{double a=Double.parseDouble(amount.getText().toString());if(a<=0)throw new Exception();savePayment(id,a,curr,note.getText().toString(),left,due);dlg.dismiss();}catch(Exception ex){Toast.makeText(this,"Enter a valid amount",Toast.LENGTH_SHORT).show();}});dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{if(left>0){savePayment(id,left,curr,note.getText().toString(),left,due);dlg.dismiss();}});});dlg.show();
    }
    void savePayment(long id,double a,String curr,String note,double left,String due){db.addPayment(id,a,curr,note);if(a>=left-0.01)db.rollNextDue(id,due);refresh();Toast.makeText(this,"Payment saved",Toast.LENGTH_SHORT).show();}

    void historyDialog(long id,String name){
        Cursor c=db.paymentsForClient(id);StringBuilder sb=new StringBuilder();double total=0;while(c.moveToNext()){double a=c.getDouble(c.getColumnIndexOrThrow("amount"));String curr=c.getString(c.getColumnIndexOrThrow("currency"));String at=c.getString(c.getColumnIndexOrThrow("paid_at"));String note=c.getString(c.getColumnIndexOrThrow("note"));total+=a;sb.append(at.substring(0,10)).append("   ").append(curr).append(" ").append(String.format("%.0f",a));if(note!=null&&!note.isEmpty())sb.append("   • ").append(note);sb.append("\n\n");}c.close();if(sb.length()==0)sb.append("No payments recorded yet.");TextView view=t(sb.toString(),15,navy);view.setPadding(24,12,24,12);view.setMovementMethod(new ScrollingMovementMethod());new AlertDialog.Builder(this).setTitle(name+" • Payment History").setView(view).setPositiveButton("Close",null).show();
    }

    void clientMenu(long id,String name,boolean active){
        String[] items=active?new String[]{"Edit Client","Mark as Former Client","Delete Client"}:new String[]{"Edit Client","Reactivate Client","Delete Client"};
        new AlertDialog.Builder(this).setTitle(name).setItems(items,(d,which)->{
            if(which==0)editClientDialog(id);else if(which==1){db.setActive(id,!active);refresh();}else new AlertDialog.Builder(this).setTitle("Delete Client").setMessage("Delete this client and all payment history permanently?").setPositiveButton("Delete",(x,y)->{db.deleteClient(id);refresh();}).setNegativeButton("Cancel",null).show();
        }).show();
    }

    void settingsDialog(){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(24,0,24,0);TextView label=t("Daily reminder time",15,navy);Spinner hour=new Spinner(this);String[] labels={"7:00 AM","8:00 AM","9:00 AM","10:00 AM","11:00 AM","12:00 PM","1:00 PM","2:00 PM","3:00 PM","4:00 PM","5:00 PM","6:00 PM","7:00 PM","8:00 PM"};int[] hours={7,8,9,10,11,12,13,14,15,16,17,18,19,20};hour.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));int saved=getSharedPreferences("settings",MODE_PRIVATE).getInt("reminder_hour",9);int pos=2;for(int i=0;i<hours.length;i++)if(hours[i]==saved)pos=i;hour.setSelection(pos);f.addView(label);f.addView(hour);f.addView(t("The app checks daily for payments due within 3 days, due today, and overdue payments.",13,muted));
        new AlertDialog.Builder(this).setTitle("Reminder Settings").setView(f).setPositiveButton("Save",(d,w)->{int h=hours[hour.getSelectedItemPosition()];getSharedPreferences("settings",MODE_PRIVATE).edit().putInt("reminder_hour",h).apply();ReminderScheduler.schedule(this);Toast.makeText(this,"Reminder time updated",Toast.LENGTH_SHORT).show();}).setNegativeButton("Cancel",null).show();
    }

    void openWhatsApp(String phone,String name,String curr,double left){
        if(phone==null||phone.trim().isEmpty()){Toast.makeText(this,"Add WhatsApp number first",Toast.LENGTH_SHORT).show();return;}
        String clean=phone.replaceAll("[^0-9]","");String msg="Hi "+name+", this is a friendly reminder regarding your pending payment of "+curr+" "+String.format("%.0f",left)+" for DigiWave Solutions. Please let me know once paid. Thank you.";Uri u=Uri.parse("https://wa.me/"+clean+"?text="+Uri.encode(msg));try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){Toast.makeText(this,"WhatsApp/browser not available",Toast.LENGTH_SHORT).show();}
    }
}
