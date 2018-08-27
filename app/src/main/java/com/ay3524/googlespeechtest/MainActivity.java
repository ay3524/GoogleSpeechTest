package com.ay3524.googlespeechtest;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ay3524.googlespeechtest.speech.SpeechService;
import com.ay3524.googlespeechtest.speech.VoiceRecorder;
import com.ay3524.googlespeechtest.utils.NetworkUtils;
import com.ay3524.googlespeechtest.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonStart, buttonStop;
    TextView textViewStreamedText, textViewRecognizedText, textViewStatus;
    private int mColorHearing, mColorNotHearing;
    private SpeechService mSpeechService;
    private VoiceRecorder mVoiceRecorder;
    private static final int REQUEST_PERMISSION = 600;

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (text != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    textViewRecognizedText.setText(text);
                                } else {
                                    textViewStreamedText.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder binder) {
                    mSpeechService = SpeechService.from(binder);
                    mSpeechService.addListener(mSpeechServiceListener);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    mSpeechService = null;
                }
            };

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonStart = findViewById(R.id.btn_start);
        buttonStart.setOnClickListener(this);
        buttonStop = findViewById(R.id.btn_stop);
        buttonStop.setOnClickListener(this);


        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);
    }

    private void requestPermissions() {
        String[] permissionsArray = {Manifest.permission.RECORD_AUDIO};

        new PermissionUtils().requestPermission(this, permissionsArray, REQUEST_PERMISSION,
                new PermissionUtils.RequestPermissionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed() {
                        Toast.makeText(getApplicationContext(), "Request permission failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                onStartPressed();
                break;
            case R.id.btn_stop:
                onStopPressed();
                break;
        }
    }

    private void onStopPressed() {
        toggleButtonState(buttonStop, false);

        toggleButtonState(buttonStart, true);

        stopVoiceRecorder();
    }

    private void onStartPressed() {
        toggleButtonState(buttonStart, false);

        toggleButtonState(buttonStop, true);

        if (NetworkUtils.isNetworkAvailable(getApplicationContext())) {
            startVoiceRecorder();
        } else {
            Toast.makeText(this, "Internet Not Available!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void toggleButtonState(Button button, boolean state) {
        button.setEnabled(state);
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }
    };

    private void showStatus(final boolean status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setTextColor(status ? mColorHearing : mColorNotHearing);
            }
        });
    }
}
