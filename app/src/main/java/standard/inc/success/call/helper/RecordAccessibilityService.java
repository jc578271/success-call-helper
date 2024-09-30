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

public class RecordAccessibilityService extends AccessibilityService {
  private static final String MAIN_APP_PACKAGE_NAME = "standard.inc.success.call";
  private static final String TAG = "RAService";

  private RecordService recordService;
  private int callState = 0;

  @Override
  public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
  }

  @Override
  public void onCreate() {
    super.onCreate();

    Log.d(TAG, getPackageName());

    IntentFilter filter = getIntentFilter();

    // Register using the correct context
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(recordingReceiver, filter, Context.RECEIVER_EXPORTED);
      } else {
        registerReceiver(recordingReceiver, filter);
      }
      Log.d(TAG, "Receiver registered");
    } catch (Exception e) {
      Log.e(TAG, "Error registering receiver: " + e.getMessage());
    }
  }

  private @NonNull IntentFilter getIntentFilter() {

    // Register the system-wide broadcast receiver to listen for the custom intents
    IntentFilter filter = new IntentFilter();
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallReceived);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallAnswered);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallEnded);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.OutgoingCallStarted);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.OutgoingCallEnded);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.MissedCall);

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
    return recordService.stopRecord();
  }

  private void startRecord(String name, String start) {
    recordService = new RecordService();
    recordService.setFileName(name + start.replaceAll("[^a-zA-Z0-9-_.]", "_") + ".wav");
    recordService.setPath(getFilesDir().getPath());
    recordService.startRecord();
  }

  // Define the BroadcastReceiver
  private final BroadcastReceiver recordingReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.i(TAG, "BroadcastReceiver, action: " + action);

      String number = intent.getStringExtra("number");
      String start = intent.getStringExtra("start");
      String end = intent.getStringExtra("end");
      boolean recordEnable = intent.getBooleanExtra("recordEnable", false);

      /* --------- INCOMING ------------- */
      /* IncomingCallReceived */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallReceived).equals(action)) {
        Log.i(TAG, "CALL_RECORDER INCOMING_RECEIVED, callState: " + callState);
        if (callState > 0) return;
        callState++;

        Intent params = new Intent();
        params.putExtra("number", number);
        params.putExtra("type", "INCOMING_RECEIVED");
        sendFromHelperBroadcast(BroadcastAction.onIncomingCallReceived, params);
        return;
      }

      /* IncomingCallAnswered */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallAnswered).equals(action)) {
        Log.i(TAG, "CALL_RECORDER INCOMING_ANSWERED, callState: " + callState);
        if (callState > 1) return;
        callState++;
        if (!recordEnable) {
          Intent params = new Intent();
          params.putExtra("number", number);
          params.putExtra("type", "INCOMING_ANSWERED");
          params.putExtra("reason", "Record is disabled");
          sendFromHelperBroadcast(BroadcastAction.onBlockRecordPhoneCall, params);
          return;
        }
        assert start != null;
        startRecord("record-incoming-", start);
        return;
      }

      /* IncomingCallEnded */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.IncomingCallEnded).equals(action)) {
        Log.i(TAG, "CALL_RECORDER INCOMING_ENDED, callState: " + callState);
        if (recordService == null) return;
        if (callState > 2) return;
        callState = 0;
        String path = stopRecord();
        Intent params = new Intent();
        params.putExtra("filePath", path);
        params.putExtra("number", number);
        params.putExtra("start", start);
        params.putExtra("end", end);
        sendFromHelperBroadcast(BroadcastAction.onIncomingCallRecorded, params);
        return;
      }

      /* --------- OUTGOING ------------- */
      /* OutgoingCallStarted */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.OutgoingCallStarted).equals(action)) {
        Log.i(TAG, "CALL_RECORDER OUTGOING_STARTED, callState: " + callState);
        if (callState > 0) return;
        callState++;
        if (!recordEnable) {
          Intent params = new Intent();
          params.putExtra("number", number);
          params.putExtra("type", "OUTGOING_STARTED");
          params.putExtra("reason", "Record is disabled");
          sendFromHelperBroadcast(BroadcastAction.onBlockRecordPhoneCall, params);
          return;
        }
        assert start != null;
        startRecord("record-outgoing-", start);
        return;
      }

      /* OutgoingCallEnded */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.OutgoingCallEnded).equals(action)) {
        Log.i(TAG, "CALL_RECORDER OUTGOING_ENDED, callState: " + callState);
        if (recordService == null) return;
        if (callState > 1) return;
        callState = 0;
        String path = stopRecord();
        Intent params = new Intent();
        params.putExtra("filePath", path);
        params.putExtra("number", number);
        params.putExtra("start", start);
        params.putExtra("end", end);
        sendFromHelperBroadcast(BroadcastAction.onOutgoingCallRecorded, params);
        return;
      }

      /* MissedCall */
      if ((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.MissedCall).equals(action)) {
        Log.i(TAG, "CALL_RECORDER MISSED, callState: " + callState);
        if (callState > 1) return;
        callState = 0;
        Intent params = new Intent();
        params.putExtra("number", number);
        params.putExtra("start", start);
        sendFromHelperBroadcast(BroadcastAction.onMissedCall, params);
      }
    }
  };

  private void sendFromHelperBroadcast(String eventName, Intent eventData) {
    String packageName = getPackageName();
    Log.i(TAG, "sendBroadcast: " + packageName + " : " + eventName);
    eventData.setAction(packageName + "." + eventName);
    sendBroadcast(eventData);
  }
}

