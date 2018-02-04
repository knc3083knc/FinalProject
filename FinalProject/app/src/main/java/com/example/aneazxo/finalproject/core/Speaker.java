package com.example.aneazxo.finalproject.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Wuttinan on 13/6/2559.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class Speaker extends UtteranceProgressListener implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    public static Speaker speaker;


    public static Speaker getInstance(Context context) {
        // if (speaker == null) {
        speaker = new Speaker(context);
        //}
        return speaker;
    }

    private Context context;
    private TextToSpeech tts;
    private Locale locale = Locale.getDefault();
    private String enginePackageName;
    private String message;
    private boolean isRunning;
    private int speakCount;

    //private static Locale THAI;
    public Speaker(Context context) {
        this.context = context;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void speak(String message) {
        this.message = message;

        if (tts == null || !isRunning) {
            speakCount = 0;

            if (enginePackageName != null && !enginePackageName.isEmpty()) {
                tts = new TextToSpeech(context, this, enginePackageName);
            } else {
                tts = new TextToSpeech(context, this);
            }

            isRunning = true;
        } else {
            startSpeak();
        }
    }

    public Speaker setEngine(String packageName) {
        enginePackageName = packageName;
        return this;
    }

    public Speaker setLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    private void startSpeak() {

        HashMap<String, String> params = new HashMap<String, String>();

        if (locale != null) {
            tts.setLanguage(locale);
        }
        /*tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Debug.ON) System.out.println("use OnUtteranceProgressListener");
            tts.setOnUtteranceProgressListener(this);
        } else {
            if (Debug.ON) System.out.println("use OnUtteranceCompletedListener");
            tts.setOnUtteranceCompletedListener(this);
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(message, TextToSpeech.QUEUE_ADD, null, "");
        } else {
            tts.speak(message, TextToSpeech.QUEUE_ADD, params);
        }
    }

    private void clear() {
        isRunning = false;
        while (speakCount != 0) {
            tts.stop();
            tts.shutdown();
            speakCount--;
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            startSpeak();
            System.out.println("Finish onInit");
        }
    }

    @Override
    public void onDone(String utteranceId) {
        clear();
        System.out.println("Finish onDone");
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {
        clear();
        System.out.println("onUtteranceCompleted");
    }

    @Override
    public void onStart(String utteranceId) {
        System.out.println("onStart");
    }

    @Override
    public void onError(String utteranceId) {
        clear();
    }
}
