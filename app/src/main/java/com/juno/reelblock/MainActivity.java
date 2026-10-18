package com.juno.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Main screen with 3 bottom tabs: Blocking, Study timer, Reel counter.
 * Fintech-dark theme with DP-blue accent.
 */
public class MainActivity extends Activity {

    private CountDownTimer studyTimer;
    private long studyTotalMillis = 25 * 60 * 1000L;
    private long studyRemaining = studyTotalMillis;
    private boolean studyRunning = false;

    private LinearLayout pageBlocking, pageStudy, pageCounter;
    private Button tabBlocking, tabStudy, tabCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pageBlocking = findViewById(R.id.page_blocking);
        pageStudy = findViewById(R.id.page_study);
        pageCounter = findViewById(R.id.page_counter);
        tabBlocking = findViewById(R.id.btn_tab_blocking);
        tabStudy = findViewById(R.id.btn_tab_study);
        tabCounter = findViewById(R.id.btn_tab_counter);

        tabBlocking.setOnClickListener(v -> selectTab(0));
        tabStudy.setOnClickListener(v -> selectTab(1));
        tabCounter.setOnClickListener(v -> selectTab(2));
        selectTab(0);

        setupBlockingTab();
        setupStudyTimer();
        setupCounterTab();
    }

    private void selectTab(int tab) {
        pageBlocking.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        pageStudy.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        pageCounter.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        styleTab(tabBlocking, tab == 0);
        styleTab(tabStudy, tab == 1);
        styleTab(tabCounter, tab == 2);

        if (tab == 2) refreshCounters();
    }

    private void styleTab(Button btn, boolean active) {
        if (active) {
            btn.setBackgroundResource(R.drawable.btn_blue);
            btn.setTextColor(getColor(R.color.bg_dark));
        } else {
            btn.setBackgroundResource(R.drawable.btn_ghost);
            btn.setTextColor(getColor(R.color.text_soft));
        }
    }

    // ---------- Blocking tab ----------

    private void setupBlockingTab() {
        Switch swReels = findViewById(R.id.sw_reels);
        Switch swShorts = findViewById(R.id.sw_shorts);
        Switch swStrict = findViewById(R.id.sw_strict);
        SeekBar sbDelay = findViewById(R.id.sb_delay);
        TextView tvDelay = findViewById(R.id.tv_delay);
        Button btnEnable = findViewById(R.id.btn_enable);

        swReels.setChecked(Prefs.blockReels(this));
        swShorts.setChecked(Prefs.blockShorts(this));
        swStrict.setChecked(Prefs.strictMode(this));

        int delay = Prefs.delaySecs(this);
        sbDelay.setMax(57); // 3..60 seconds
        sbDelay.setProgress(delay - 3);
        tvDelay.setText("Rokne ka time: " + delay + " second");

        swReels.setOnCheckedChangeListener((v, on) -> Prefs.set(this, Prefs.KEY_BLOCK_REELS, on));
        swShorts.setOnCheckedChangeListener((v, on) -> Prefs.set(this, Prefs.KEY_BLOCK_SHORTS, on));
        swStrict.setOnCheckedChangeListener((v, on) -> Prefs.set(this, Prefs.KEY_STRICT, on));
        sbDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int secs = p + 3;
                Prefs.setDelay(MainActivity.this, secs);
                tvDelay.setText("Rokne ka time: " + secs + " second");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        btnEnable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    // ---------- Study tab ----------

    private void setupStudyTimer() {
        TextView tvTimer = findViewById(R.id.tv_timer);
        TextView tvMsg = findViewById(R.id.tv_timer_msg);
        Button btnStart = findViewById(R.id.btn_timer_start);
        Button btnReset = findViewById(R.id.btn_timer_reset);

        updateTimerText(tvTimer);

        int[] presetIds = {R.id.btn_t15, R.id.btn_t25, R.id.btn_t45, R.id.btn_t60};
        int[] presetMins = {15, 25, 45, 60};
        for (int i = 0; i < presetIds.length; i++) {
            final int mins = presetMins[i];
            findViewById(presetIds[i]).setOnClickListener(v -> {
                stopStudyTimer();
                studyTotalMillis = mins * 60 * 1000L;
                studyRemaining = studyTotalMillis;
                updateTimerText(tvTimer);
                tvMsg.setText(mins + " minute ka session set! Start dabao.");
                btnStart.setText("Start");
            });
        }

        btnStart.setOnClickListener(v -> {
            if (studyRunning) {
                stopStudyTimer();
                btnStart.setText("Start");
                tvMsg.setText("Ruka hua hai — Start dabake continue karo.");
            } else {
                if (studyRemaining <= 0) {
                    studyRemaining = studyTotalMillis;
                }
                studyRunning = true;
                btnStart.setText("Pause");
                tvMsg.setText("Padhai chal rahi hai... lage raho!");
                studyTimer = new CountDownTimer(studyRemaining, 1000) {
                    @Override public void onTick(long millisUntilFinished) {
                        studyRemaining = millisUntilFinished;
                        updateTimerText(tvTimer);
                    }
                    @Override public void onFinish() {
                        studyRunning = false;
                        studyRemaining = studyTotalMillis;
                        updateTimerText(tvTimer);
                        btnStart.setText("Start");
                        tvMsg.setText("Khatam! Thoda break le lo, bhai.");
                    }
                }.start();
            }
        });

        btnReset.setOnClickListener(v -> {
            stopStudyTimer();
            studyRemaining = studyTotalMillis;
            updateTimerText(tvTimer);
            btnStart.setText("Start");
            tvMsg.setText("Reset ho gaya. Padhai shuru karo, bhai!");
        });
    }

    private void stopStudyTimer() {
        if (studyTimer != null) {
            studyTimer.cancel();
            studyTimer = null;
        }
        studyRunning = false;
    }

    private void updateTimerText(TextView tvTimer) {
        long totalSecs = studyRemaining / 1000;
        long m = totalSecs / 60;
        long s = totalSecs % 60;
        tvTimer.setText(String.format("%02d:%02d", m, s));
    }

    // ---------- Counter tab ----------

    private void setupCounterTab() {
        Button btnReset = findViewById(R.id.btn_reset_counters);
        btnReset.setOnClickListener(v -> {
            Prefs.resetCounters(this);
            refreshCounters();
        });
        refreshCounters();
    }

    private void refreshCounters() {
        TextView tvBlocked = findViewById(R.id.tv_blocked_count);
        TextView tvWatched = findViewById(R.id.tv_watched_count);
        if (tvBlocked != null) tvBlocked.setText(String.valueOf(Prefs.blockedCount(this)));
        if (tvWatched != null) tvWatched.setText(String.valueOf(Prefs.watchedCount(this)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText(isServiceEnabled()
                ? "Service ON — blocking active"
                : "Service OFF — Blocking tab me ON karo");
        refreshCounters();
    }

    @Override
    protected void onDestroy() {
        stopStudyTimer();
        super.onDestroy();
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        String me = getPackageName() + "/" + ReelBlockService.class.getName();
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(me)
                    || s.equalsIgnoreCase(getPackageName() + "/.ReelBlockService")) {
                return true;
            }
        }
        return false;
    }
}
