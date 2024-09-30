package standard.inc.success.call.helper;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class RecordAccessibilityService extends AccessibilityService {
    private static final String MAIN_APP_PACKAGE_NAME = "standard.inc.success.call";
    private static final String TAG = "RecordAccessibilityService";

    private static RecordService recordService = null;
    private Boolean isRecording = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, getPackageName());

        IntentFilter filter = getIntentFilter();

        // Register using the correct context
        try {
            registerReceiver(recordingReceiver, filter, Context.RECEIVER_EXPORTED);
            Log.d(TAG, "Receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver: " + e.getMessage());
        }
    }

    private @NonNull IntentFilter getIntentFilter() {

        // Register the system-wide broadcast receiver to listen for the custom intents
        IntentFilter filter = new IntentFilter();
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallReceived);
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallAnswered);
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallEnded);
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.OutgoingCallStarted);
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.OutgoingCallEnded);
        filter.addAction(MAIN_APP_PACKAGE_NAME + BroadcastAction.MissedCall);

        return filter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recordingReceiver);
    }

    @Override
    public void onInterrupt() {
        stopRecord();
    }

    private String stopRecord() {
        isRecording = false;
        return recordService.stopRecord();
    }

    private void startRecord(String name, String start) {
        recordService = new RecordService();
        recordService.setFileName(name + start.replaceAll("[^a-zA-Z0-9-_.]", "_") + ".wav");
        recordService.setPath(getFilesDir().getPath());
        recordService.startRecord();
        isRecording = true;
    }

    // Define the BroadcastReceiver
    private final BroadcastReceiver recordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (recordService == null) return;

            String action = intent.getAction();
            String number = intent.getStringExtra("number");
            String start = intent.getStringExtra("start");
            assert start != null;
            String end = intent.getStringExtra("end");
            boolean recordEnable = intent.getBooleanExtra("recordEnable", false);

            /* IncomingCallReceived */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallReceived).equals(action)) {
                Log.i(TAG, "CALL_RECORDER INCOMING_RECEIVED" + number);
                Intent params = new Intent();
                params.putExtra("number", number);
                params.putExtra("type", "INCOMING_RECEIVED");
                emitDeviceEvent(BroadcastAction.onIncomingCallReceived, params);
                return;
            }

            /* IncomingCallAnswered */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallAnswered).equals(action)) {
                Log.i(TAG, "CALL_RECORDER INCOMING_ANSWERED");
                if (isRecording) return;
                if (!recordEnable) {
                    Intent params = new Intent();
                    params.putExtra("number", number);
                    params.putExtra("type", "INCOMING_ANSWERED");
                    params.putExtra("reason", "Record is disabled");
                    emitDeviceEvent(BroadcastAction.onBlockRecordPhoneCall, params);
                    return;
                }
                startRecord("record-incoming-", start);
                return;
            }

            /* IncomingCallEnded */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.IncomingCallEnded).equals(action)) {
                Log.i(TAG, "CALL_RECORDER INCOMING_ENDED");
                String path = stopRecord();
                Intent params = new Intent();
                params.putExtra("filePath", path);
                params.putExtra("number", number);
                params.putExtra("start", start);
                params.putExtra("end", end);
                emitDeviceEvent(BroadcastAction.onIncomingCallRecorded, params);
                return;
            }

            /* OutgoingCallStarted */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.OutgoingCallStarted).equals(action)) {
                Log.i(TAG, "CALL_RECORDER OUTGOING_STARTED");
                if (isRecording) return;
                if (!recordEnable) {
                    Intent params = new Intent();
                    params.putExtra("number", number);
                    params.putExtra("type", "OUTGOING_STARTED");
                    params.putExtra("reason", "Record is disabled");
                    emitDeviceEvent(BroadcastAction.onBlockRecordPhoneCall, params);
                    return;
                }
                startRecord("record-outgoing-", start);
                return;
            }

            /* OutgoingCallEnded */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.OutgoingCallEnded).equals(action)) {
                Log.i(TAG, "CALL_RECORDER OUTGOING_ENDED");
                String path = stopRecord();
                Intent params = new Intent();
                params.putExtra("filePath", path);
                params.putExtra("number", number);
                params.putExtra("start", start);
                params.putExtra("end", end);
                emitDeviceEvent(BroadcastAction.onOutgoingCallRecorded, params);
                return;
            }

            /* MissedCall */
            if ((MAIN_APP_PACKAGE_NAME + BroadcastAction.MissedCall).equals(action)) {
                Log.i(TAG, "CALL_RECORDER MISSED");
                Intent params = new Intent();
                params.putExtra("number", number);
                params.putExtra("start", start);
                emitDeviceEvent(BroadcastAction.onMissedCall, params);
            }
        }
    };

    private void emitDeviceEvent(String eventName, Intent eventData) {
        String packageName = getPackageName();
        Log.i(TAG, "sendBroadcast: " + packageName + " : " + eventName);
        eventData.setAction(packageName + eventName);
        sendBroadcast(eventData);
    }
}

