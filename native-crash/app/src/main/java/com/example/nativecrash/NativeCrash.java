package com.example.nativecrash;

import static android.app.ApplicationExitInfo.REASON_CRASH_NATIVE;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.android.server.os.Protos;
import com.example.nativecrash.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class NativeCrash extends AppCompatActivity {

    static {
        System.loadLibrary("nativecrash");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextView bottomText = binding.bottomText;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Protos.Tombstone tombstone = getMostRecentNativeCrash();
            if (tombstone != null) {
                bottomText.setText(getTombstoneSummary(tombstone));
                Log.w(NativeCrash.class.getName(), "Native crash detected: " + tombstone);
            } else {
                bottomText.setText("No native crashes found");
            }
        } else {
            bottomText.setText(String.format("Tombstones not supported in API version < %d", Build.VERSION_CODES.S));
        }
    }

    /**
     * @param tombstone a proto object that represents a native crash
     * @return a human readable summary of the root cause of the given crash
     */
    private String getTombstoneSummary(Protos.Tombstone tombstone) {
        StringBuilder summary = new StringBuilder();
        summary.append("Most recent native crash: ").append("Causes=").append(tombstone.getCausesList().size());
        List<String> causes = tombstone
                .getCausesList()
                .stream()
                .map(Protos.Cause::getHumanReadable)
                .filter(cause -> !cause.isEmpty())
                .collect(Collectors.toList());
        for (String cause : causes) {
            summary.append(", cause=").append(cause);
        }

        Protos.Signal signal = tombstone.getSignalInfo();
        if (!signal.getName().isEmpty()) {
            summary.append(", signal=").append(signal.getName());
        }
        return summary.toString();
    }

    /**
     * @return A proto object that contains the details of the native crash if the most recent
     * execution of this app was terminated due to a native crash (e.g., segfault, signal, etc);
     * or null otherwise (i.e., normal termination or a Java/Kotlin exception).
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public Protos.Tombstone getMostRecentNativeCrash() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationExitInfo> exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 1);
        for (ApplicationExitInfo aei: exitReasons) {
            if (aei.getReason() == REASON_CRASH_NATIVE) {
                try {
                    return Protos.Tombstone.parseFrom(aei.getTraceInputStream());
                } catch (IOException e) {
                    Log.w(NativeCrash.class.getName(), "Failed to get native crash input stream");
                }
            }
        }
        return null;
    }

    public void onClickThrowJavaException(View view) {
        throw new RuntimeException("This is a java runtime exception");
    }

    public void onClickTriggerNullCppReference(View view) {
        triggerNullCppReference();
    }

    public void onClickSigabort(View view) {
        triggerSigabort();
    }

    // Triggers a segfault by means of accessing a null reference.
    public native void triggerNullCppReference();

    // Triggers a SIGABORT signal.
    public native void triggerSigabort();
}