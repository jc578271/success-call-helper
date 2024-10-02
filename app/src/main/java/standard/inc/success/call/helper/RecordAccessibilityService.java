package standard.inc.success.call.helper;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

import standard.inc.success.call.helper.services.PhoneCallReceiver;

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

    /* registerReceiver */
    IntentFilter filter = getIntentFilter();
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(recordingReceiver, filter, Context.RECEIVER_EXPORTED);
      } else {
        registerReceiver(recordingReceiver, filter);
      }
      Log.d(TAG, "Receiver registered");

      Intent params = new Intent();
      params.putExtra("recordEnabled", false);
      sendFromHelperBroadcast(BroadcastAction.onRecordEnabled, params);
      Log.d(TAG, "RecordEnabled");
    } catch (Exception e) {
      Log.e(TAG, "Error registering receiver: " + e.getMessage());
    }
  }

  private @NonNull IntentFilter getIntentFilter() {

    // Register the system-wide broadcast receiver to listen for the custom intents
    IntentFilter filter = new IntentFilter();
    filter.addAction("android.intent.action.PHONE_STATE");
    filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.onRecordEnabled);

    return filter;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    sendRecordEnabled(false);
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
    recordService = new RecordService(this);
    recordService.setFileName(name + start.replaceAll("[^a-zA-Z0-9-_.]", "_") + ".wav");
    recordService.startRecord();
  }

  private final PhoneCallReceiver recordingReceiver = new PhoneCallReceiver() {
//    private static final String TAG = "PhoneCallReceiver";
    public boolean recordEnabled = false;

    @Override
    protected void onCustomReceive(Context ctx, Intent intent) {
      String action = intent.getAction();
//      Log.d(TAG, "onCustomReceive, action: " + action);
      if (Objects.requireNonNull(action).equals((MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.onRecordEnabled))) {
        recordEnabled = intent.getBooleanExtra("recordEnabled", false);
        Log.d(TAG, "onCustomReceive, recordEnabled: " + recordEnabled);
        sendRecordEnabled(recordEnabled);
      }
    }

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start) {
      Log.i(TAG, "CALL_RECORDER INCOMING_RECEIVED, callState: " + callState);
      if (callState > 0) return;
      callState = 1;

      Intent params = new Intent();
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));
      params.putExtra("type", "INCOMING_RECEIVED");
      sendFromHelperBroadcast(BroadcastAction.onIncomingCallReceived, params);
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start) {
      Log.i(TAG, "CALL_RECORDER INCOMING_ANSWERED, callState: " + callState);
      if (callState > 1) return;
      callState = 2;
      if (!recordEnabled) {
        Intent params = new Intent();
        params.putExtra("number", number);
        params.putExtra("start", String.valueOf(start.getTime()));
        params.putExtra("type", "INCOMING_ANSWERED");
        params.putExtra("reason", "Record is disabled");
        sendFromHelperBroadcast(BroadcastAction.onBlockRecordPhoneCall, params);
        return;
      }
      startRecord("record-incoming-", String.valueOf(start.getTime()));
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
      Log.i(TAG, "CALL_RECORDER INCOMING_ENDED, callState: " + callState);
      if (recordService == null) return;
      if (callState > 2) return;
      callState = 0;
      String path = stopRecord();
      Intent params = new Intent();
      params.putExtra("filePath", path);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));
      params.putExtra("end", String.valueOf(end.getTime()));
      sendFromHelperBroadcast(BroadcastAction.onIncomingCallRecorded, params);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
      Log.i(TAG, "CALL_RECORDER OUTGOING_STARTED, callState: " + callState);
      if (callState > 0) return;
      callState = 1;
      if (!recordEnabled) {
        Intent params = new Intent();
        params.putExtra("number", number);
        params.putExtra("type", "OUTGOING_STARTED");
        params.putExtra("reason", "Record is disabled");
        sendFromHelperBroadcast(BroadcastAction.onBlockRecordPhoneCall, params);
        return;
      }
      startRecord("record-outgoing-", String.valueOf(start.getTime()));
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
      Log.i(TAG, "CALL_RECORDER OUTGOING_ENDED, callState: " + callState);
      if (recordService == null) return;
      if (callState > 1) return;
      callState = 0;
      String path = stopRecord();
      Intent params = new Intent();
      params.putExtra("filePath", path);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));
      params.putExtra("end", String.valueOf(end.getTime()));
      sendFromHelperBroadcast(BroadcastAction.onOutgoingCallRecorded, params);
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
      Log.i(TAG, "CALL_RECORDER MISSED, callState: " + callState);
      if (callState > 1) return;
      callState = 0;
      Intent params = new Intent();
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));
      sendFromHelperBroadcast(BroadcastAction.onMissedCall, params);
    }
  };

  private void sendFromHelperBroadcast(String eventName, Intent eventData) {
    try {
      String packageName = getPackageName();
      Log.i(TAG, "sendBroadcast: " + packageName + " : " + eventName);
      eventData.setAction(packageName + "." + eventName);
      sendBroadcast(eventData);
    } catch (Exception e) {
      Log.e(TAG, Objects.requireNonNull(e.getMessage()));
    }
  }

  private void sendRecordEnabled(boolean recordEnabled) {
    Intent params = new Intent();
    params.putExtra("recordEnabled", recordEnabled);
    sendFromHelperBroadcast(BroadcastAction.onRecordEnabled, params);
  }
}

