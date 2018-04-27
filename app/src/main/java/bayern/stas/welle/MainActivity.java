package bayern.stas.welle;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private enum STATE {
        STATE_STOPPED,
        STATE_GETREADY,
        STATE_HOLDING,
        STATE_RESTING,
        STATE_PAUSED
    }

    private STATE state = STATE.STATE_STOPPED;

    private TextView mPhaseTxt;
    private TextView mHintTxt;
    private TextView mCounterTxt;

    private final int getReadyValue = 10;
    private Timer timer = null;

    private final int[] scheme = {20, 25, 30, 40, 60, 40, 30, 25, 20};
    private int currentPhaseIndex = 0;
    private int counter = 0;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_start:
                    if (state == STATE.STATE_STOPPED || state == STATE.STATE_PAUSED) {
                        start();
                    } else if (state != STATE.STATE_GETREADY){
                        pause();
                    }
                    return true;
                case R.id.navigation_reset:
                    stop();
                    return true;
            }
            return false;
        }
    };

    private void pause() {
        if (state == STATE.STATE_RESTING) {
            if (++currentPhaseIndex >= scheme.length) {
                stop();
                return;
            }
        }

        mHintTxt.setText(R.string.msg_paused);
        timer.cancel();
        timer = null;
        state = STATE.STATE_PAUSED;
    }

    private void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        state = STATE.STATE_STOPPED;

        mPhaseTxt.setText(R.string.msg_start);
        mHintTxt.setText(hintByState(state));
        mCounterTxt.setText("");
    }

    private void start() {
        if (state == STATE.STATE_STOPPED) {
            currentPhaseIndex = 0;
        }

        state = STATE.STATE_GETREADY;
        counter = getReadyValue;
        mPhaseTxt.setText(String.format(getResources().getString(R.string.title_phase_frmt), scheme[currentPhaseIndex]));
        mHintTxt.setText(hintByState(state));

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mCounterTxt.setText(String.format("%d", --counter));
                        if (counter == 0) {
                            triggerPhase();
                        }
                    }

                });
            }

        }, 0, 1000);
    }

    private void triggerPhase() {
        generateTone(1000, 800).play();
        if (state == STATE.STATE_GETREADY) {
            counter = scheme[currentPhaseIndex];
            state = STATE.STATE_HOLDING;
            mHintTxt.setText(hintByState(state));
            mCounterTxt.setText(String.format("%d", counter));
        } else if (state == STATE.STATE_HOLDING) {
            if (currentPhaseIndex + 1 >= scheme.length) {
                stop();
            } else {
                counter = scheme[currentPhaseIndex];
                state = STATE.STATE_RESTING;
                mHintTxt.setText(hintByState(state));
                mCounterTxt.setText(String.format("%d", counter));
            }
        } else if (state == STATE.STATE_RESTING) {
            counter = scheme[++currentPhaseIndex];
            state = STATE.STATE_HOLDING;
            mHintTxt.setText(hintByState(state));
            mCounterTxt.setText(String.format("%d", counter));
            mPhaseTxt.setText(String.format(getResources().getString(R.string.title_phase_frmt), scheme[currentPhaseIndex]));
        }
    }

    private String hintByState(STATE s) {
        String result = "";
        if (s == STATE.STATE_GETREADY) {
            result = getResources().getString(R.string.msg_getready);
        } else if (s == STATE.STATE_HOLDING) {
            result = getResources().getString(R.string.msg_hold);
        } else if (s == STATE.STATE_RESTING) {
            result = getResources().getString(R.string.msg_rest);
        } else if (s == STATE.STATE_PAUSED) {
            result = getResources().getString(R.string.msg_paused);
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPhaseTxt = (TextView) findViewById(R.id.phase);
        mHintTxt = (TextView) findViewById(R.id.textView);
        mCounterTxt = (TextView) findViewById(R.id.textView2);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        stop();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        pause();
    }

    private AudioTrack generateTone(double freqHz, int durationMs)
    {
        int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
        short[] samples = new short[count];
        for(int i = 0; i < count; i += 2){
            short sample = (short)(Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF);
            samples[i + 0] = sample;
            samples[i + 1] = sample;
        }
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
        track.write(samples, 0, count);
        return track;
    }
}

