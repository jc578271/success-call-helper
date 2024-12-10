package standard.inc.success.call.helper;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import standard.inc.success.call.helper.services.PhoneCallReceiver;

public class RecordAccessibilityService extends AccessibilityService {
  private static final String MAIN_APP_PACKAGE_NAME = Constants.MAIN_APP_PACKAGE_NAME;
  private static final String TAG = "RAService";

  private RecordService recordService;
  private int callState = 0;
  private int callType = 0; // 0: null, 1: outgoing, 2: incoming
  private String callNumber = null;
  private Long callStart = null;
  private boolean recordEnabled = false;
  private MyPhoneCallReceiver recordingReceiver;

  @Override
  public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    AccessibilityNodeInfo info = accessibilityEvent.getSource();
    TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);

    if (info != null && info.getText() != null) {
      Log.d(TAG, "info.getText(): "+ info.getPackageName() + " ___ " + telecomManager.getDefaultDialerPackage());

      String dialerPackageName = telecomManager.getDefaultDialerPackage();

      String[] parts = dialerPackageName.split("\\.");
      int partsToJoin = parts.length - 1;
      String incalluiPackageName = String.join(".", java.util.Arrays.copyOfRange(parts, 0, partsToJoin)) + ".incallui";

      if (info.getPackageName().equals(dialerPackageName) || info.getPackageName().equals(incalluiPackageName)) {
        onOutgoingCallAnswered();
      }
    }
  }

//  @Override
//  protected void onServiceConnected() {
//    super.onServiceConnected();
//    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//    info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
//    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
//    info.notificationTimeout = 0;
//    info.packageNames = null;
//    setServiceInfo(info);
//  }

  @Override
  public void onCreate() {
    super.onCreate();
    recordingReceiver = new MyPhoneCallReceiver();

    /* registerReceiver */
    IntentFilter filter = getIntentFilter();
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(recordingReceiver, filter, Context.RECEIVER_EXPORTED);
      } else {
        registerReceiver(recordingReceiver, filter);
      }

      sendRecordEnabled(false);
    } catch (Exception e) {
      Log.e(TAG, "Error registering receiver: " + e.getMessage());
    }
  }

  private @NonNull IntentFilter getIntentFilter() {

    // Register the system-wide broadcast receiver to listen for the custom intents
    IntentFilter filter = new IntentFilter();
    filter.addAction("android.intent.action.PHONE_STATE");
    filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + Constants.onRecordEnabled);
    filter.addAction(MAIN_APP_PACKAGE_NAME + "." + Constants.getRecordStatus);

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

  class MyPhoneCallReceiver extends PhoneCallReceiver {
    @Override
    protected void onCustomReceive(Context ctx, Intent intent) {
      String action = intent.getAction();
      if (action == null) return;

      switch (action) {
        case MAIN_APP_PACKAGE_NAME + "." + Constants.onRecordEnabled: {
          recordEnabled = intent.getBooleanExtra("recordEnabled", false);
          sendRecordEnabled(recordEnabled);
          break;
        }

        case MAIN_APP_PACKAGE_NAME + "." + Constants.getRecordStatus: {
          sendRecordEnabled(recordEnabled);
          break;
        }

        default:
          break;
      }
    }

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start) {
      if (number == null || number.isEmpty()) return;
      if (callState != Constants.CALL_INIT) return;
      callState = Constants.CALL_RINGING;

      callType = Constants.IS_INCOMING_CALL;
      callNumber = number;

      Intent params = new Intent();

      params.putExtra("status", Constants.INCOMING_CALL_RECEIVED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());

      startMainAppService(Constants.onCallStateChange, params);
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start) {
      if (callType != Constants.IS_INCOMING_CALL) return;
      if (callState != Constants.CALL_RINGING) return;
      callState = Constants.CALL_CONNECTED;

      callStart = new Date().getTime();

      Intent params = new Intent();

      params.putExtra("status", Constants.INCOMING_CALL_ANSWERED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());

      startMainAppService(Constants.onCallStateChange, params);

      if (recordEnabled) {
        startRecord("record-incoming-", String.valueOf(start.getTime()));
      }
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
      if (callType != Constants.IS_INCOMING_CALL) return;
      if (callState != Constants.CALL_CONNECTED) return;

      Intent params = new Intent();

      params.putExtra("status", Constants.INCOMING_CALL_ENDED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());
      params.putExtra("duration", (int) (new Date().getTime() - callStart));

      if (recordService != null) {
        String path = stopRecord();
        params.putExtra("filePath", path);
      }

      startMainAppService(Constants.onCallStateChange, params);
      resetCacheData();
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
      Log.d(TAG, "onOutgoingCallStarted, condition: " + (number == null || number.isEmpty()));
      Log.d(TAG, "onOutgoingCallStarted, number: " + number);
      if (number == null || number.isEmpty()) return;
      if (callState != Constants.CALL_INIT) return;
      callState = Constants.CALL_RINGING;

      callType = Constants.IS_OUTGOING_CALL;
      callNumber = number;
      callStart = new Date().getTime();

      Intent params = new Intent();

      params.putExtra("status", Constants.OUTGOING_CALL_STARTED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());

      startMainAppService(Constants.onCallStateChange, params);

      if (recordEnabled) {
        startRecord("record-outgoing-", String.valueOf(start.getTime()));
      }
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
      if (callType != Constants.IS_OUTGOING_CALL) return;
      if (callState != Constants.CALL_RINGING && callState != Constants.CALL_CONNECTED) return;

      Intent params = new Intent();

      params.putExtra("status", Constants.OUTGOING_CALL_ENDED);
      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());

      if (callStart != null) {
        params.putExtra("duration", (int) (new Date().getTime() - callStart));
      } else if (callState == Constants.CALL_RINGING) {
        params.putExtra("duration", 0);
      }

      if (recordService != null) {
        String path = stopRecord();
        params.putExtra("filePath", path);
      }

      startMainAppService(Constants.onCallStateChange, params);
      resetCacheData();
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start, Date end) {
      if (callState != Constants.CALL_RINGING) return;

      Intent params = new Intent();

      params.putExtra("status", callType == Constants.IS_INCOMING_CALL
        ? Constants.INCOMING_CALL_MISSED
        : Constants.OUTGOING_CALL_MISSED);

      params.putExtra("recordEnabled", recordEnabled);
      params.putExtra("number", callNumber);
      params.putExtra("myNumbers", getMyPhoneNumbers());

      startMainAppService(Constants.onCallStateChange, params);
      resetCacheData();
    }
  }

  private String[] getMyPhoneNumbers() {
    ArrayList<String> myPhoneList = new ArrayList<>();

    SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
    if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
      return new String[0];
    }
    List<SubscriptionInfo> subsInfoList = subscriptionManager.getActiveSubscriptionInfoList();
    Log.d(TAG, "Current list = " + subsInfoList);
    for (SubscriptionInfo subscriptionInfo : subsInfoList) {
      String number = subscriptionInfo.getNumber();
      myPhoneList.add(number);
      Log.d(TAG, " Number is  " + number);
    }

    return myPhoneList.toArray(new String[0]);
  }

  private void onOutgoingCallAnswered() {
    if (callType != Constants.IS_OUTGOING_CALL) return;
    if (callState != Constants.CALL_RINGING) return;

    Log.d(TAG, "onOutgoingCallAnswered");

    callState = Constants.CALL_CONNECTED;
    callStart = new Date().getTime();

    Intent params = new Intent();

    params.putExtra("status", Constants.OUTGOING_CALL_ANSWERED);
    params.putExtra("recordEnabled", recordEnabled);
    params.putExtra("number", callNumber);

    startMainAppService(Constants.onCallStateChange, params);
  }

  private void startMainAppService(String eventName, Intent eventData) {
    try {
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

    Intent params = new Intent();
    params.putExtra("recordEnabled", recordEnabled);
    params.setAction(packageName + "." + Constants.onRecordEnabled);
    sendBroadcast(params);
  }

  private void resetCacheData() {
    callType = 0;
    callState = Constants.CALL_INIT;
    callStart = null;
    callNumber = null;
  }
}

