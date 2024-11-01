package standard.inc.success.call.helper;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

import standard.inc.success.call.helper.services.PhoneCallReceiver;

public class RecordAccessibilityService extends AccessibilityService {
  private static final String MAIN_APP_PACKAGE_NAME = "standard.inc.success.call";
  private static final String TAG = "RAService";

  private RecordService recordService;
  private int callState = 0;
  private boolean recordEnabled = false;

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

      sendRecordEnabled(false);
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
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.getRecordStatus);

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
    @Override
    protected void onCustomReceive(Context ctx, Intent intent) {
      String action = intent.getAction();
      if (action == null) return;

      switch (action) {
        case MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.onRecordEnabled: {
          recordEnabled = intent.getBooleanExtra("recordEnabled", false);
          Log.d(TAG, "onRecordEnabled, recordEnabled: " + recordEnabled);
          sendRecordEnabled(recordEnabled);
          break;
        }

        case MAIN_APP_PACKAGE_NAME + "." + BroadcastAction.getRecordStatus: {
          Log.d(TAG, "getRecordStatus, recordEnabled: " + recordEnabled);
          sendRecordEnabled(recordEnabled);
          break;
        }

        default: break;
      }
    }

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start) {
      Log.d(TAG, "CALL_RECORDER INCOMING_RECEIVED, callState: " + callState);
      if (callState != 0) return;
      callState = 1;
      Intent params = new Intent();

      params.putExtra("type", BroadcastAction.INCOMING_CALL_RECEIVED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));

      startMainAppService(BroadcastAction.onCallStateChange, params);
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start) {
      Log.d(TAG, "CALL_RECORDER INCOMING_ANSWERED, callState: " + callState);
      if (callState != 1) return;
      callState = 2;
      Intent params = new Intent();

      params.putExtra("type", BroadcastAction.INCOMING_CALL_ANSWERED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));

      startMainAppService(BroadcastAction.onCallStateChange, params);

      if (recordEnabled) {
        startRecord("record-incoming-", String.valueOf(start.getTime()));
      }
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
      Log.d(TAG, "CALL_RECORDER INCOMING_ENDED, callState: " + callState);
      if (callState != 2) return;
      callState = 0;
      Intent params = new Intent();

      params.putExtra("type", BroadcastAction.INCOMING_CALL_ENDED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));

      params.putExtra("end", String.valueOf(end.getTime()));
      if (recordService != null) {
        String path = stopRecord();
        params.putExtra("filePath", path);
      }

      startMainAppService(BroadcastAction.onCallStateChange, params);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
      Log.d(TAG, "CALL_RECORDER OUTGOING_STARTED, callState: " + callState);
      if (callState != 0) return;
      callState = 1;
      Intent params = new Intent();

      params.putExtra("type", BroadcastAction.OUTGOING_CALL_STARTED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));

      startMainAppService(BroadcastAction.onCallStateChange, params);

      if (recordEnabled) {
        startRecord("record-outgoing-", String.valueOf(start.getTime()));
      }
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
      try {
        Log.d(TAG, "CALL_RECORDER OUTGOING_ENDED, callState: " + callState);

        if (callState != 1) return;
        callState = 0;

        Intent params = new Intent();

        params.putExtra("type", BroadcastAction.OUTGOING_CALL_ENDED);
        params.putExtra("recordEnabled", recordEnabled);
        params.putExtra("number", number);
        params.putExtra("start", String.valueOf(start.getTime()));

        params.putExtra("end", String.valueOf(end.getTime()));

        if (recordService != null) {
          String path = stopRecord();
          params.putExtra("filePath", path);
        }

        startMainAppService(BroadcastAction.onCallStateChange, params);
      } catch (Exception e) {
        Log.e(TAG, "onOutgoingCallEnded, error: " + e.getMessage());
      }

    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start, Date end) {
      Log.d(TAG, "CALL_RECORDER MISSED, callState: " + callState);
      if (callState != 1) return;
      callState = 0;

      Intent params = new Intent();
      params.putExtra("type", BroadcastAction.OUTGOING_CALL_MISSED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", number);
      params.putExtra("start", String.valueOf(start.getTime()));

      params.putExtra("end", String.valueOf(end.getTime()));

      startMainAppService(BroadcastAction.onCallStateChange, params);
    }
  };

  private void startMainAppService(String eventName, Intent eventData) {
    try {

      Log.d(TAG, "startMainAppService: " + MAIN_APP_PACKAGE_NAME + " : " + eventName);
//      eventData.setAction(packageName + "." + eventName);
//      sendBroadcast(eventData);

      eventData.setAction(eventName);
      eventData.setComponent(new ComponentName(MAIN_APP_PACKAGE_NAME, MAIN_APP_PACKAGE_NAME + ".telephony.HelperService"));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(eventData);
      } else {
        startService(eventData);
      }

    } catch (Exception e) {
      Log.e(TAG, Objects.requireNonNull(e.getMessage()));
    }
  }

  private void sendRecordEnabled(boolean recordEnabled) {
    String packageName = getPackageName();
    Log.d(TAG, "sendRecordEnabled: " + packageName + " : " + BroadcastAction.onRecordEnabled);

    Intent params = new Intent();
    params.putExtra("recordEnabled", recordEnabled);
    params.setAction(packageName + "." + BroadcastAction.onRecordEnabled);
    sendBroadcast(params);
  }
}

