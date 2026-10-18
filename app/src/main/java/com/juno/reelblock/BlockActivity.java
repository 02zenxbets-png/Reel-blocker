package com.juno.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Friction screen shown when reels/shorts are blocked.
 * The "Continue" button unlocks after the delay countdown.
 */
public class BlockActivity extends Activity {

    public static final String EXTRA_KIND = "kind";

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        String kind = getIntent().getStringExtra(EXTRA_KIND);
        boolean isReels = "reels".equals(kind);

        TextView title = findViewById(R.id.block_title);
        TextView subtitle = findViewById(R.id.block_subtitle);
        TextView countdown = findViewById(R.id.block_countdown);
        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnClose = findViewById(R.id.btn_close_apps);

        title.setText(isReels ? "Reels Blocked!" : "Shorts Blocked!");
        subtitle.setText("Thoda break le bhai\nPadhai pe focus kar!");

        boolean strict = Prefs.strictMode(this);
        if (strict) {
            btnContinue.setVisibility(View.GONE);
            countdown.setText("NO MERCY!\nWapas mat jao!");
        }

        int delaySecs = Math.max(3, Prefs.delaySecs(this));
        if (!strict) {
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.4f);
        }
        timer = new CountDownTimer(strict ? 3000 : delaySecs * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long s = (millisUntilFinished + 999) / 1000;
                countdown.setText("Wait " + s + "s...");
            }

            @Override
            public void onFinish() {
                if (strict) {
                    // Auto-send to home, no mercy.
                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(home);
                    finish();
                } else {
                    countdown.setText("Ab ja sakte ho!");
                    btnContinue.setEnabled(true);
                    btnContinue.setAlpha(1f);
                }
            }
        }.start();

        btnContinue.setOnClickListener(v -> finish());

        btnClose.setOnClickListener(v -> {
            // Send user to home screen.
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Don't allow skipping via back button.
    }
}
