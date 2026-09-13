package com.digiwave.payments;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class MainActivity extends Activity {
    DbHelper db;
    FrameLayout shell;
    LinearLayout page, clientList, bottomNav;
    EditText search;
    String mode = "home";

    final int INK = Color.rgb(15, 23, 42);
    final int INK_2 = Color.rgb(30, 41, 59);
    final int MUTED = Color.rgb(100, 116, 139);
    final int SOFT = Color.rgb(247, 248, 252);
    final int SURFACE = Color.WHITE;
    final int BORDER = Color.rgb(229, 232, 240);
    final int ACCENT = Color.rgb(99, 91, 255);
    final int ACCENT_2 = Color.rgb(124, 110, 255);
    final int ACCENT_SOFT = Color.rgb(241, 239, 255);
    final int GREEN = Color.rgb(5, 150, 105);
    final int GREEN_SOFT = Color.rgb(236, 253, 245);
    final int RED = Color.rgb(220, 38, 38);
    final int RED_SOFT = Color.rgb(254, 242, 242);
    final int AMBER = Color.rgb(217, 119, 6);
    final int AMBER_SOFT = Color.rgb(255, 251, 235);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DbHelper(this);
        ReminderScheduler.schedule(this);

        getWindow().setStatusBarColor(SOFT);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        buildShell();
    }

    int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable solid(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int) radius));
        return g;
    }

    GradientDrawable outlined(int color, float radius, int strokeColor) {
        GradientDrawable g = solid(color, radius);
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        g.setCornerRadius(dp((int) radius));
        return g;
    }

    TextView text(String value, float sp, int color, int weight) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        String family = weight >= 600 ? "sans-serif-medium" : "sans-serif";
        v.setTypeface(Typeface.create(family, Typeface.NORMAL));
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    TextView overline(String value) {
        TextView v = text(value.toUpperCase(Locale.US), 11, MUTED, 600);
        v.setLetterSpacing(.07f);
        return v;
    }

    Space gap(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return s;
    }

    View hDivider() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(241, 243, 247));
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return v;
    }

    LinearLayout surfaceCard(int radius) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackground(outlined(SURFACE, radius, BORDER));
        c.setElevation(dp(1));
        return c;
    }

    TextView pill(String value, int fg, int bg) {
        TextView p = text(value, 11, fg, 600);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(10), dp(6), dp(10), dp(6));
        p.setBackground(solid(bg, 999));
        return p;
    }

    TextView button(String value, boolean primary) {
        TextView b = text(value, 14, primary ? Color.WHITE : INK, 600);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(15), dp(12), dp(15), dp(12));
        b.setBackground(primary ? solid(ACCENT, 14) : outlined(Color.WHITE, 14, BORDER));
        b.setClickable(true);
        b.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) b.setElevation(primary ? dp(2) : 0);
        return b;
    }

    ImageView icon(int drawable, int tint, int size) {
        ImageView v = new ImageView(this);
        v.setImageResource(drawable);
        v.setColorFilter(tint);
        v.setPadding(dp(2), dp(2), dp(2), dp(2));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
        return v;
    }

    LinearLayout iconButton(int drawable, int tint, int bgColor, int size) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        b.setBackground(solid(bgColor, 15));
        b.setClickable(true);
        b.setFocusable(true);
        b.addView(icon(drawable, tint, 21), new LinearLayout.LayoutParams(dp(22), dp(22)));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
        return b;
    }

    void buildShell() {
        shell = new FrameLayout(this);
        shell.setBackgroundColor(SOFT);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(8), dp(18), dp(116));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        bottomNav = makeBottomNav();
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(76), Gravity.BOTTOM);
        navLp.setMargins(dp(14), 0, dp(14), dp(12));
        shell.addView(bottomNav, navLp);

        setContentView(shell);
        render();
    }

    LinearLayout makeBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        nav.setBackground(outlined(Color.WHITE, 25, BORDER));
        nav.setElevation(dp(12));
        addNav(nav, R.drawable.ic_home, "Home", "home");
        addNav(nav, R.drawable.ic_clients, "Clients", "clients");
        addNav(nav, R.drawable.ic_clock, "Due", "due");
        addNav(nav, R.drawable.ic_settings, "Settings", "settings");
        return nav;
    }

    void addNav(LinearLayout nav, int drawable, String label, String navMode) {
        boolean selected = mode.equals(navMode);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(6), dp(6), dp(5));
        if (selected) item.setBackground(solid(ACCENT_SOFT, 18));

        ImageView i = icon(drawable, selected ? ACCENT : MUTED, 20);
        TextView t = text(label, 10.5f, selected ? ACCENT : MUTED, selected ? 600 : 400);
        t.setGravity(Gravity.CENTER);
        item.addView(i, new LinearLayout.LayoutParams(dp(22), dp(23)));
        item.addView(t, new LinearLayout.LayoutParams(-1, dp(22)));
        item.setOnClickListener(v -> {
            mode = navMode;
            rebuildNav();
            render();
        });
        nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    void rebuildNav() {
        int idx = shell.indexOfChild(bottomNav);
        shell.removeView(bottomNav);
        bottomNav = makeBottomNav();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, dp(76), Gravity.BOTTOM);
        lp.setMargins(dp(14), 0, dp(14), dp(12));
        shell.addView(bottomNav, idx, lp);
    }

    void render() {
        page.removeAllViews();
        search = null;
        if (mode.equals("settings")) {
            renderSettings();
            return;
        }
        renderHeader();
        if (mode.equals("home")) renderHome();
        else renderClients(mode.equals("due"));
    }

    void renderHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(18));

        LinearLayout mark = new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(ACCENT, Color.rgb(132, 88, 255), 15));
        TextView d = text("D", 18, Color.WHITE, 600);
        d.setGravity(Gravity.CENTER);
        mark.addView(d, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        String heading = mode.equals("home") ? "DigiWave Solutions" : (mode.equals("due") ? "Payment Due" : "Clients");
        String sub = mode.equals("home") ? greeting() + "  •  " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM", Locale.US)) :
                (mode.equals("due") ? "Follow up before payments slip" : "Manage clients and monthly fees");
        brand.addView(text(heading, 20, INK, 600));
        brand.addView(gap(3));
        brand.addView(text(sub, 12.5f, MUTED, 400));

        LinearLayout add = iconButton(R.drawable.ic_plus, Color.WHITE, ACCENT, 46);
        add.setElevation(dp(3));
        add.setOnClickListener(v -> clientFormDialog(-1));

        row.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));
        row.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(add, new LinearLayout.LayoutParams(dp(46), dp(46)));
        page.addView(row);
    }

    String greeting() {
        int h = LocalTime.now().getHour();
        if (h < 12) return "Good morning";
        if (h < 17) return "Good afternoon";
        return "Good evening";
    }

    static class Stats {
        double aed, pkr;
        int active, due, overdue, soon;
    }

    Stats stats() {
        Stats s = new Stats();
        Cursor c = db.clients();
        LocalDate now = LocalDate.now();
        while (c.moveToNext()) {
            s.active++;
            long id = c.getLong(c.getColumnIndexOrThrow("id"));
            double fee = c.getDouble(c.getColumnIndexOrThrow("fee"));
            String curr = c.getString(c.getColumnIndexOrThrow("currency"));
            String due = c.getString(c.getColumnIndexOrThrow("next_due"));
            double left = Math.max(0, fee - db.paidSinceDue(id, due));
            long diff = ChronoUnit.DAYS.between(now, LocalDate.parse(due));
            if (left > 0) {
                if (curr.equals("AED")) s.aed += left; else s.pkr += left;
                if (diff < 0) s.overdue++;
                if (diff == 0) s.due++;
                if (diff > 0 && diff <= 3) s.soon++;
            }
        }
        c.close();
        return s;
    }

    void renderHome() {
        Stats s = stats();

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(21), dp(21), dp(21), dp(20));
        hero.setBackground(gradient(Color.rgb(16, 23, 45), Color.rgb(55, 46, 122), 28));
        hero.setElevation(dp(3));

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("OUTSTANDING", 11, Color.rgb(195, 199, 219), 600);
        label.setLetterSpacing(.09f);
        TextView active = pill(s.active + " ACTIVE", Color.WHITE, Color.argb(35, 255, 255, 255));
        heroTop.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        heroTop.addView(active);
        hero.addView(heroTop);
        hero.addView(gap(17));

        LinearLayout balances = new LinearLayout(this);
        balances.setOrientation(LinearLayout.HORIZONTAL);
        balances.addView(balanceBlock("AED", money(s.aed)), new LinearLayout.LayoutParams(0, -2, 1));
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(45, 255, 255, 255));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(1), dp(55));
        divLp.setMargins(dp(15), 0, dp(15), 0);
        balances.addView(divider, divLp);
        balances.addView(balanceBlock("PKR", money(s.pkr)), new LinearLayout.LayoutParams(0, -2, 1));
        hero.addView(balances);
        hero.addView(gap(18));

        LinearLayout insight = new LinearLayout(this);
        insight.setGravity(Gravity.CENTER_VERTICAL);
        insight.setPadding(dp(12), dp(10), dp(12), dp(10));
        insight.setBackground(solid(Color.argb(28, 255, 255, 255), 15));
        ImageView bell = icon(R.drawable.ic_bell, Color.WHITE, 18);
        String insightText = attentionText(s);
        TextView it = text(insightText, 12, Color.rgb(236, 238, 248), 400);
        it.setPadding(dp(9), 0, 0, 0);
        insight.addView(bell, new LinearLayout.LayoutParams(dp(18), dp(18)));
        insight.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
        hero.addView(insight);

        page.addView(hero);
        page.addView(gap(18));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("Payment pulse", 18, INK, 600), new LinearLayout.LayoutParams(0, -2, 1));
        TextView small = text("LIVE", 10, GREEN, 600);
        small.setLetterSpacing(.06f);
        small.setPadding(dp(9), dp(5), dp(9), dp(5));
        small.setBackground(solid(GREEN_SOFT, 999));
        titleRow.addView(small);
        page.addView(titleRow);
        page.addView(gap(10));

        LinearLayout metricsRow1 = new LinearLayout(this);
        metricsRow1.setOrientation(LinearLayout.HORIZONTAL);
        metricsRow1.addView(metricCard(R.drawable.ic_calendar, "Due today", String.valueOf(s.due), AMBER, AMBER_SOFT), new LinearLayout.LayoutParams(0, dp(114), 1));
        metricsRow1.addView(horizontalSpace(10));
        metricsRow1.addView(metricCard(R.drawable.ic_alert, "Overdue", String.valueOf(s.overdue), RED, RED_SOFT), new LinearLayout.LayoutParams(0, dp(114), 1));
        page.addView(metricsRow1);
        page.addView(gap(10));

        LinearLayout metricsRow2 = new LinearLayout(this);
        metricsRow2.setOrientation(LinearLayout.HORIZONTAL);
        metricsRow2.addView(metricCard(R.drawable.ic_clock, "Next 3 days", String.valueOf(s.soon), ACCENT, ACCENT_SOFT), new LinearLayout.LayoutParams(0, dp(114), 1));
        metricsRow2.addView(horizontalSpace(10));
        metricsRow2.addView(metricCard(R.drawable.ic_clients, "Active clients", String.valueOf(s.active), INK_2, Color.rgb(241, 245, 249)), new LinearLayout.LayoutParams(0, dp(114), 1));
        page.addView(metricsRow2);
        page.addView(gap(22));

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.addView(text("Needs attention", 18, INK, 600), new LinearLayout.LayoutParams(0, -2, 1));
        TextView viewAll = text("View all", 13, ACCENT, 600);
        viewAll.setPadding(dp(12), dp(8), 0, dp(8));
        viewAll.setOnClickListener(v -> { mode = "due"; rebuildNav(); render(); });
        section.addView(viewAll);
        page.addView(section);
        page.addView(gap(10));

        clientList = new LinearLayout(this);
        clientList.setOrientation(LinearLayout.VERTICAL);
        page.addView(clientList);
        renderClientRows(true, 4);

        page.addView(gap(14));
        TextView add = button("Add New Client", true);
        add.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_plus, 0, 0, 0);
        add.setCompoundDrawablePadding(dp(8));
        add.setOnClickListener(v -> clientFormDialog(-1));
        page.addView(add, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    String attentionText(Stats s) {
        int total = s.due + s.overdue + s.soon;
        if (total == 0) return "Everything is on track. No follow-up needed today.";
        if (s.overdue > 0) return s.overdue + " overdue payment" + (s.overdue == 1 ? " needs" : "s need") + " your attention.";
        if (s.due > 0) return s.due + " payment" + (s.due == 1 ? " is" : "s are") + " due today.";
        return s.soon + " payment" + (s.soon == 1 ? " is" : "s are") + " coming up in the next 3 days.";
    }

    LinearLayout balanceBlock(String code, String amount) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(text(code, 11, Color.rgb(190, 195, 216), 600));
        l.addView(gap(5));
        TextView a = text(amount, 27, Color.WHITE, 600);
        a.setMaxLines(1);
        l.addView(a);
        return l;
    }

    Space horizontalSpace(int w) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1));
        return s;
    }

    LinearLayout metricCard(int drawable, String labelText, String value, int accent, int tint) {
        LinearLayout c = surfaceCard(22);
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout iconWrap = iconButton(drawable, accent, tint, 34);
        top.addView(iconWrap, new LinearLayout.LayoutParams(dp(34), dp(34)));
        top.addView(text(value, 25, INK, 600), new LinearLayout.LayoutParams(0, dp(34), 1));
        ((TextView) top.getChildAt(1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        c.addView(top);
        c.addView(gap(10));
        c.addView(text(labelText, 12.5f, MUTED, 600));
        return c;
    }

    void renderClients(boolean dueOnly) {
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(14), 0, dp(10), 0);
        searchBox.setBackground(outlined(Color.WHITE, 17, BORDER));
        searchBox.setElevation(dp(1));
        ImageView mag = icon(R.drawable.ic_search, MUTED, 19);
        search = new EditText(this);
        search.setHint("Search name or WhatsApp");
        search.setTextSize(14.5f);
        search.setTextColor(INK);
        search.setHintTextColor(Color.rgb(148, 163, 184));
        search.setSingleLine(true);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(10), 0, 0, 0);
        searchBox.addView(mag, new LinearLayout.LayoutParams(dp(20), dp(20)));
        searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(54), 1));
        page.addView(searchBox);
        page.addView(gap(17));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text(dueOnly ? "Follow-up queue" : "All clients", 18, INK, 600), new LinearLayout.LayoutParams(0, -2, 1));
        TextView former = pill("Former", MUTED, Color.rgb(238, 241, 245));
        former.setPadding(dp(12), dp(7), dp(12), dp(7));
        former.setOnClickListener(v -> formerDialog());
        heading.addView(former);
        page.addView(heading);
        page.addView(gap(10));

        clientList = new LinearLayout(this);
        clientList.setOrientation(LinearLayout.VERTICAL);
        page.addView(clientList);
        renderClientRows(dueOnly, 0);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { renderClientRows(dueOnly, 0); }
            public void afterTextChanged(Editable e) {}
        });
    }

    void renderClientRows(boolean dueOnly, int limit) {
        if (clientList == null) return;
        clientList.removeAllViews();
        String q = search == null ? "" : search.getText().toString().trim();
        Cursor c;
        if (!q.isEmpty()) c = db.searchClients(q, false);
        else if (dueOnly) c = db.overdueOrDue(3);
        else c = db.clients();

        int shown = 0;
        LocalDate now = LocalDate.now();
        while (c.moveToNext()) {
            if (limit > 0 && shown >= limit) break;
            long id = c.getLong(c.getColumnIndexOrThrow("id"));
            String name = c.getString(c.getColumnIndexOrThrow("name"));
            String phone = c.getString(c.getColumnIndexOrThrow("phone"));
            double fee = c.getDouble(c.getColumnIndexOrThrow("fee"));
            String curr = c.getString(c.getColumnIndexOrThrow("currency"));
            String due = c.getString(c.getColumnIndexOrThrow("next_due"));
            String notes = c.getString(c.getColumnIndexOrThrow("notes"));
            double left = Math.max(0, fee - db.paidSinceDue(id, due));
            long diff = ChronoUnit.DAYS.between(now, LocalDate.parse(due));
            if (dueOnly && left <= 0) continue;
            addClientCard(id, name, phone, curr, due, notes, left, diff);
            shown++;
        }
        c.close();

        if (shown == 0) {
            LinearLayout empty = surfaceCard(24);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(31), dp(24), dp(31));
            LinearLayout iconWrap = iconButton(dueOnly ? R.drawable.ic_check : R.drawable.ic_clients,
                    dueOnly ? GREEN : ACCENT, dueOnly ? GREEN_SOFT : ACCENT_SOFT, 48);
            empty.addView(iconWrap, new LinearLayout.LayoutParams(dp(48), dp(48)));
            empty.addView(gap(13));
            TextView a = text(dueOnly ? "You're all caught up" : "No clients yet", 17, INK, 600);
            a.setGravity(Gravity.CENTER);
            empty.addView(a);
            empty.addView(gap(5));
            TextView b = text(dueOnly ? "No payments need attention right now." : "Add your first client to start tracking payments.", 13, MUTED, 400);
            b.setGravity(Gravity.CENTER);
            empty.addView(b);
            clientList.addView(empty);
        }
    }

    void addClientCard(long id, String name, String phone, String curr, String due, String notes, double left, long diff) {
        LinearLayout c = surfaceCard(23);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(11));
        clientList.addView(c, cp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = text(initials(name), 14, ACCENT, 600);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(solid(ACCENT_SOFT, 15));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, 0, 0);
        names.addView(text(name, 16, INK, 600));
        names.addView(gap(3));
        names.addView(text(phone == null || phone.isEmpty() ? "WhatsApp not added" : phone, 12, MUTED, 400));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));

        String status;
        int statusFg, statusBg;
        if (left <= 0) { status = "Paid"; statusFg = GREEN; statusBg = GREEN_SOFT; }
        else if (diff < 0) { status = "Overdue"; statusFg = RED; statusBg = RED_SOFT; }
        else if (diff == 0) { status = "Due today"; statusFg = AMBER; statusBg = AMBER_SOFT; }
        else if (diff <= 3) { status = "Due soon"; statusFg = ACCENT; statusBg = ACCENT_SOFT; }
        else { status = "Upcoming"; statusFg = MUTED; statusBg = Color.rgb(241, 245, 249); }
        top.addView(pill(status, statusFg, statusBg));
        c.addView(top);
        c.addView(gap(16));
        c.addView(hDivider());
        c.addView(gap(15));

        LinearLayout moneyRow = new LinearLayout(this);
        moneyRow.setGravity(Gravity.BOTTOM);
        LinearLayout amt = new LinearLayout(this);
        amt.setOrientation(LinearLayout.VERTICAL);
        amt.addView(overline("Amount due"));
        amt.addView(gap(5));
        amt.addView(text(curr + " " + money(left), 22, INK, 600));
        moneyRow.addView(amt, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout date = new LinearLayout(this);
        date.setOrientation(LinearLayout.VERTICAL);
        TextView dl = overline("Next due");
        dl.setGravity(Gravity.RIGHT);
        TextView dv = text(friendlyDate(due), 13, INK_2, 600);
        dv.setGravity(Gravity.RIGHT);
        date.addView(dl);
        date.addView(gap(5));
        date.addView(dv);
        moneyRow.addView(date);
        c.addView(moneyRow);

        if (notes != null && !notes.trim().isEmpty()) {
            c.addView(gap(11));
            TextView note = text(notes, 12.5f, MUTED, 400);
            note.setMaxLines(2);
            note.setEllipsize(TextUtils.TruncateAt.END);
            c.addView(note);
        }

        c.addView(gap(16));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView wa = miniAction(R.drawable.ic_whatsapp, "WhatsApp", ACCENT, ACCENT_SOFT);
        TextView pay = miniAction(R.drawable.ic_wallet, "Payment", INK, Color.rgb(244, 246, 249));
        LinearLayout more = iconButton(R.drawable.ic_more, MUTED, Color.rgb(244, 246, 249), 44);
        wa.setOnClickListener(v -> openWhatsApp(phone, name, curr, left));
        pay.setOnClickListener(v -> paymentDialog(id, name, curr, left, due));
        more.setOnClickListener(v -> clientMenu(id, name, true));

        actions.addView(wa, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(horizontalSpace(8));
        actions.addView(pay, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(horizontalSpace(8));
        actions.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        c.addView(actions);
    }

    TextView miniAction(int drawable, String label, int fg, int bg) {
        TextView b = text(label, 12.5f, fg, 600);
        b.setGravity(Gravity.CENTER);
        b.setBackground(solid(bg, 13));
        b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        b.setCompoundDrawablePadding(dp(7));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    String initials(String name) {
        String[] p = name.trim().split("\\s+");
        String a = p.length > 0 && !p[0].isEmpty() ? p[0].substring(0, 1) : "?";
        if (p.length > 1) a += p[p.length - 1].substring(0, 1);
        return a.toUpperCase(Locale.US);
    }

    String money(double x) {
        if (Math.abs(x - Math.rint(x)) < .001) return String.format(Locale.US, "%,.0f", x);
        return String.format(Locale.US, "%,.2f", x);
    }

    String friendlyDate(String iso) {
        try { return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)); }
        catch (Exception e) { return iso; }
    }

    EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(14.5f);
        e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(148, 163, 184));
        e.setSingleLine(true);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(outlined(Color.rgb(250, 251, 253), 14, BORDER));
        e.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        return e;
    }

    TextView formLabel(String label) {
        TextView l = overline(label);
        l.setTextColor(Color.rgb(100, 116, 139));
        return l;
    }

    void clientFormDialog(long id) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(16), dp(20), dp(24));
        wrap.setBackground(solid(Color.WHITE, 28));

        View grab = new View(this);
        grab.setBackground(solid(Color.rgb(216, 220, 230), 999));
        LinearLayout grabRow = new LinearLayout(this);
        grabRow.setGravity(Gravity.CENTER);
        grabRow.addView(grab, new LinearLayout.LayoutParams(dp(42), dp(4)));
        wrap.addView(grabRow);
        wrap.addView(gap(17));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text(id > 0 ? "Edit client" : "Add new client", 22, INK, 600));
        titleBox.addView(gap(4));
        titleBox.addView(text("Monthly billing and WhatsApp follow-up", 12.5f, MUTED, 400));
        head.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout close = iconButton(R.drawable.ic_close, MUTED, Color.rgb(244, 246, 249), 40);
        close.setOnClickListener(v -> d.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        wrap.addView(head);
        wrap.addView(gap(22));

        EditText name = field("e.g. Ahmed Khan");
        EditText phone = field("e.g. 971501234567");
        EditText fee = field("e.g. 1500");
        fee.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText next = field("Select date");
        next.setFocusable(false);
        next.setClickable(true);
        next.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0);
        next.setCompoundDrawablePadding(dp(8));
        EditText notes = field("Optional note");

        addFormField(wrap, "Client name", name);
        addFormField(wrap, "WhatsApp", phone);
        addFormField(wrap, "Monthly fee", fee);

        final String[] cur = {"AED"};
        LinearLayout currency = new LinearLayout(this);
        currency.setOrientation(LinearLayout.HORIZONTAL);
        currency.setPadding(dp(4), dp(4), dp(4), dp(4));
        currency.setBackground(solid(Color.rgb(244, 246, 249), 16));
        TextView aed = segment("AED", true);
        TextView pkr = segment("PKR", false);
        currency.addView(aed, new LinearLayout.LayoutParams(0, dp(42), 1));
        currency.addView(pkr, new LinearLayout.LayoutParams(0, dp(42), 1));
        wrap.addView(formLabel("Currency"));
        wrap.addView(gap(7));
        wrap.addView(currency);
        wrap.addView(gap(16));

        wrap.addView(formLabel("Next due date"));
        wrap.addView(gap(7));
        wrap.addView(next);
        wrap.addView(gap(16));
        wrap.addView(formLabel("Notes"));
        wrap.addView(gap(7));
        wrap.addView(notes);
        wrap.addView(gap(22));

        Runnable refreshCurrency = () -> {
            boolean isAed = cur[0].equals("AED");
            aed.setBackground(isAed ? solid(Color.WHITE, 12) : ColorDrawableCompat.transparent());
            aed.setTextColor(isAed ? INK : MUTED);
            aed.setElevation(isAed ? dp(1) : 0);
            pkr.setBackground(!isAed ? solid(Color.WHITE, 12) : ColorDrawableCompat.transparent());
            pkr.setTextColor(!isAed ? INK : MUTED);
            pkr.setElevation(!isAed ? dp(1) : 0);
        };
        aed.setOnClickListener(v -> { cur[0] = "AED"; refreshCurrency.run(); });
        pkr.setOnClickListener(v -> { cur[0] = "PKR"; refreshCurrency.run(); });

        final int[] billingDay = {Math.min(LocalDate.now().getDayOfMonth(), 28)};
        if (id > 0) {
            Cursor c = db.client(id);
            if (c.moveToFirst()) {
                name.setText(c.getString(c.getColumnIndexOrThrow("name")));
                phone.setText(c.getString(c.getColumnIndexOrThrow("phone")));
                fee.setText(String.valueOf(c.getDouble(c.getColumnIndexOrThrow("fee"))));
                cur[0] = c.getString(c.getColumnIndexOrThrow("currency"));
                billingDay[0] = c.getInt(c.getColumnIndexOrThrow("billing_day"));
                next.setText(c.getString(c.getColumnIndexOrThrow("next_due")));
                notes.setText(c.getString(c.getColumnIndexOrThrow("notes")));
            }
            c.close();
        } else {
            next.setText(LocalDate.now().toString());
        }
        refreshCurrency.run();

        next.setOnClickListener(v -> {
            LocalDate base;
            try { base = LocalDate.parse(next.getText().toString()); }
            catch (Exception e) { base = LocalDate.now(); }
            DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
                LocalDate selected = LocalDate.of(year, month + 1, day);
                next.setText(selected.toString());
            }, base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth());
            picker.show();
        });

        TextView save = button(id > 0 ? "Save Changes" : "Add Client", true);
        wrap.addView(save, new LinearLayout.LayoutParams(-1, dp(54)));
        save.setOnClickListener(v -> {
            try {
                String n = name.getText().toString().trim();
                double f = Double.parseDouble(fee.getText().toString().trim());
                LocalDate nd = LocalDate.parse(next.getText().toString().trim());
                if (n.isEmpty() || f <= 0) throw new Exception();
                billingDay[0] = Math.min(nd.getDayOfMonth(), 28);
                if (id > 0) db.updateClient(id, n, phone.getText().toString().trim(), f, cur[0], billingDay[0], nd.toString(), notes.getText().toString().trim());
                else db.addClient(n, phone.getText().toString().trim(), f, cur[0], billingDay[0], nd.toString(), notes.getText().toString().trim());
                d.dismiss();
                render();
            } catch (Exception ex) {
                toast("Please check client name, fee and due date");
            }
        });

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(wrap);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams a = w.getAttributes();
            a.dimAmount = .38f;
            w.setAttributes(a);
        }
        d.show();
        if (w != null) {
            w.setLayout(-1, -2);
            w.setGravity(Gravity.BOTTOM);
        }
    }

    void addFormField(LinearLayout wrap, String label, View field) {
        wrap.addView(formLabel(label));
        wrap.addView(gap(7));
        wrap.addView(field);
        wrap.addView(gap(16));
    }

    TextView segment(String value, boolean selected) {
        TextView v = text(value, 13.5f, selected ? INK : MUTED, 600);
        v.setGravity(Gravity.CENTER);
        v.setBackground(selected ? solid(Color.WHITE, 12) : ColorDrawableCompat.transparent());
        if (selected) v.setElevation(dp(1));
        return v;
    }

    void paymentDialog(long id, String name, String curr, double left, String due) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(24));
        box.setBackground(solid(Color.WHITE, 28));

        View grab = new View(this);
        grab.setBackground(solid(Color.rgb(216, 220, 230), 999));
        LinearLayout grabRow = new LinearLayout(this);
        grabRow.setGravity(Gravity.CENTER);
        grabRow.addView(grab, new LinearLayout.LayoutParams(dp(42), dp(4)));
        box.addView(grabRow);
        box.addView(gap(17));

        box.addView(text("Record payment", 22, INK, 600));
        box.addView(gap(5));
        box.addView(text(name, 13, MUTED, 400));
        box.addView(gap(18));

        LinearLayout dueCard = new LinearLayout(this);
        dueCard.setOrientation(LinearLayout.HORIZONTAL);
        dueCard.setGravity(Gravity.CENTER_VERTICAL);
        dueCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        dueCard.setBackground(solid(ACCENT_SOFT, 16));
        LinearLayout dueText = new LinearLayout(this);
        dueText.setOrientation(LinearLayout.VERTICAL);
        dueText.addView(overline("Remaining"));
        dueText.addView(gap(4));
        dueText.addView(text(curr + " " + money(left), 20, INK, 600));
        dueCard.addView(dueText, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView wallet = icon(R.drawable.ic_wallet, ACCENT, 22);
        dueCard.addView(wallet, new LinearLayout.LayoutParams(dp(24), dp(24)));
        box.addView(dueCard);
        box.addView(gap(17));

        EditText amount = field("Amount received");
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText note = field("Note, optional");
        addFormField(box, "Amount", amount);
        box.addView(formLabel("Note"));
        box.addView(gap(7));
        box.addView(note);
        box.addView(gap(20));

        LinearLayout row = new LinearLayout(this);
        TextView full = button("Paid in Full", false);
        TextView save = button("Save Payment", true);
        row.addView(full, new LinearLayout.LayoutParams(0, dp(52), 1));
        row.addView(horizontalSpace(9));
        row.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1));
        box.addView(row);

        full.setOnClickListener(v -> {
            if (left > 0) {
                savePayment(id, left, curr, "Paid in full", left, due);
                d.dismiss();
            }
        });
        save.setOnClickListener(v -> {
            try {
                double a = Double.parseDouble(amount.getText().toString());
                if (a <= 0) throw new Exception();
                savePayment(id, a, curr, note.getText().toString(), left, due);
                d.dismiss();
            } catch (Exception e) {
                toast("Enter a valid amount");
            }
        });

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams a = w.getAttributes();
            a.dimAmount = .38f;
            w.setAttributes(a);
        }
        d.show();
        if (w != null) {
            w.setLayout(-1, -2);
            w.setGravity(Gravity.BOTTOM);
        }
    }

    void savePayment(long id, double a, String curr, String note, double left, String due) {
        db.addPayment(id, a, curr, note);
        if (a >= left - 0.01) db.rollNextDue(id, due);
        render();
        toast("Payment saved");
    }

    void clientMenu(long id, String name, boolean active) {
        String[] items = {"Edit client", "Payment history", active ? "Mark as former client" : "Reactivate client", "Delete client"};
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(items, (d, which) -> {
                    if (which == 0) clientFormDialog(id);
                    else if (which == 1) historyDialog(id, name);
                    else if (which == 2) { db.setActive(id, !active); render(); }
                    else confirmDelete(id);
                }).show();
    }

    void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete client?")
                .setMessage("This permanently deletes the client and payment history.")
                .setPositiveButton("Delete", (x, y) -> { db.deleteClient(id); render(); })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void historyDialog(long id, String name) {
        Cursor c = db.paymentsForClient(id);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), dp(8), dp(18), dp(8));
        int count = 0;
        while (c.moveToNext()) {
            double a = c.getDouble(c.getColumnIndexOrThrow("amount"));
            String curr = c.getString(c.getColumnIndexOrThrow("currency"));
            String at = c.getString(c.getColumnIndexOrThrow("paid_at"));
            String note = c.getString(c.getColumnIndexOrThrow("note"));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(friendlyDate(at.substring(0, 10)), 13, INK, 600));
            if (note != null && !note.isEmpty()) left.addView(text(note, 11.5f, MUTED, 400));
            TextView amount = text(curr + " " + money(a), 14, GREEN, 600);
            amount.setGravity(Gravity.RIGHT);
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(amount);
            if (count > 0) list.addView(hDivider());
            list.addView(row);
            count++;
        }
        c.close();
        if (count == 0) {
            TextView empty = text("No payments recorded yet.", 13, MUTED, 400);
            empty.setPadding(0, dp(20), 0, dp(20));
            empty.setGravity(Gravity.CENTER);
            list.addView(empty);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        new AlertDialog.Builder(this).setTitle(name + "  •  History").setView(sv).setPositiveButton("Close", null).show();
    }

    void formerDialog() {
        Cursor c = db.inactiveClients();
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), dp(6), dp(18), dp(6));
        int count = 0;
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndexOrThrow("id"));
            String name = c.getString(c.getColumnIndexOrThrow("name"));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            row.addView(text(name, 14, INK, 600), new LinearLayout.LayoutParams(0, -2, 1));
            TextView restore = pill("Reactivate", ACCENT, ACCENT_SOFT);
            restore.setOnClickListener(v -> { db.setActive(id, true); render(); toast("Client reactivated"); });
            row.addView(restore);
            if (count > 0) list.addView(hDivider());
            list.addView(row);
            count++;
        }
        c.close();
        if (count == 0) {
            TextView empty = text("No former clients.", 13, MUTED, 400);
            empty.setPadding(0, dp(20), 0, dp(20));
            empty.setGravity(Gravity.CENTER);
            list.addView(empty);
        }
        new AlertDialog.Builder(this).setTitle("Former clients").setView(list).setPositiveButton("Close", null).show();
    }

    void renderSettings() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(20));
        LinearLayout mark = new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(ACCENT, Color.rgb(132, 88, 255), 15));
        TextView d = text("D", 18, Color.WHITE, 600);
        d.setGravity(Gravity.CENTER);
        mark.addView(d, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(text("Settings", 21, INK, 600));
        title.addView(gap(3));
        title.addView(text("DigiWave Payment Manager", 12.5f, MUTED, 400));
        row.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));
        row.addView(title);
        page.addView(row);

        LinearLayout reminder = surfaceCard(24);
        LinearLayout rTop = new LinearLayout(this);
        rTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout bell = iconButton(R.drawable.ic_bell, ACCENT, ACCENT_SOFT, 42);
        LinearLayout rText = new LinearLayout(this);
        rText.setOrientation(LinearLayout.VERTICAL);
        rText.setPadding(dp(12), 0, 0, 0);
        rText.addView(text("Payment reminders", 16, INK, 600));
        rText.addView(gap(3));
        rText.addView(text("One smart reminder every day", 12, MUTED, 400));
        rTop.addView(bell, new LinearLayout.LayoutParams(dp(42), dp(42)));
        rTop.addView(rText, new LinearLayout.LayoutParams(0, -2, 1));
        reminder.addView(rTop);
        reminder.addView(gap(16));
        reminder.addView(hDivider());
        reminder.addView(gap(15));

        int saved = getSharedPreferences("settings", MODE_PRIVATE).getInt("reminder_hour", 9);
        TextView time = button(formatHour(saved), false);
        time.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, R.drawable.ic_chevron, 0);
        time.setCompoundDrawablePadding(dp(9));
        time.setGravity(Gravity.CENTER_VERTICAL);
        time.setPadding(dp(14), 0, dp(14), 0);
        final int[] chosen = {saved};
        time.setOnClickListener(v -> timePicker(chosen, time));
        reminder.addView(time, new LinearLayout.LayoutParams(-1, dp(52)));
        reminder.addView(gap(10));
        TextView save = button("Save Reminder Time", true);
        reminder.addView(save, new LinearLayout.LayoutParams(-1, dp(52)));
        save.setOnClickListener(v -> {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("reminder_hour", chosen[0]).apply();
            ReminderScheduler.schedule(this);
            toast("Reminder time updated");
        });
        page.addView(reminder);
        page.addView(gap(12));

        LinearLayout about = surfaceCard(24);
        about.addView(overline("Workspace"));
        about.addView(gap(12));
        settingsRow(about, R.drawable.ic_clients, "Business", "DigiWave Solutions");
        about.addView(hDivider());
        settingsRow(about, R.drawable.ic_wallet, "Currencies", "AED & PKR");
        about.addView(hDivider());
        settingsRow(about, R.drawable.ic_lock, "Data", "Stored privately on this device");
        page.addView(about);
        page.addView(gap(14));

        TextView version = text("DigiWave Payment Manager  •  Premium v3", 11.5f, Color.rgb(148, 163, 184), 400);
        version.setGravity(Gravity.CENTER);
        page.addView(version);
    }

    void settingsRow(LinearLayout parent, int drawable, String left, String right) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(13), 0, dp(13));
        LinearLayout i = iconButton(drawable, MUTED, Color.rgb(244, 246, 249), 36);
        TextView l = text(left, 13.5f, INK, 600);
        l.setPadding(dp(11), 0, 0, 0);
        TextView rr = text(right, 12.5f, MUTED, 400);
        rr.setGravity(Gravity.RIGHT);
        r.addView(i, new LinearLayout.LayoutParams(dp(36), dp(36)));
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(rr);
        parent.addView(r);
    }

    void timePicker(final int[] chosen, TextView target) {
        final int[] hours = {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        String[] labels = new String[hours.length];
        int selected = 2;
        for (int i = 0; i < hours.length; i++) {
            labels[i] = formatHour(hours[i]);
            if (hours[i] == chosen[0]) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Reminder time")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    chosen[0] = hours[which];
                    target.setText(labels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    String formatHour(int h) {
        int display = h % 12;
        if (display == 0) display = 12;
        return display + ":00 " + (h < 12 ? "AM" : "PM");
    }

    void openWhatsApp(String phone, String name, String curr, double left) {
        if (phone == null || phone.trim().isEmpty()) {
            toast("Add WhatsApp number first");
            return;
        }
        String clean = phone.replaceAll("[^0-9]", "");
        String msg = "Hi " + name + ", this is a friendly reminder regarding your pending payment of " + curr + " " + money(left) + " for DigiWave Solutions. Please let me know once paid. Thank you.";
        Uri u = Uri.parse("https://wa.me/" + clean + "?text=" + Uri.encode(msg));
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
        catch (Exception ex) { toast("WhatsApp or browser not available"); }
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    static class ColorDrawableCompat {
        static GradientDrawable transparent() {
            GradientDrawable g = new GradientDrawable();
            g.setColor(Color.TRANSPARENT);
            g.setCornerRadius(0);
            return g;
        }
    }
}
