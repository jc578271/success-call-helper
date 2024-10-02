package standard.inc.success.call.helper.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;

public abstract class PhoneCallReceiver extends BroadcastReceiver {
  private static final String TAG = "PhoneCallReceiver";

  //The receiver will be recreated whenever android feels like it.  We need a static variable to
  // remember data between instantiations
  private static int lastState = TelephonyManager.CALL_STATE_IDLE;
  private static Date callStartTime;
  private static boolean isIncoming;
  private TelephonyManager telephony;  //because the passed incoming is only valid in ringing


  @Override
  public void onReceive(final Context context, Intent intent) {
    String action = intent.getAction();
    assert action != null;

    /* onCustomReceive */
    onCustomReceive(context, intent);

    if (action.equals("android.intent.action.PHONE_STATE") ||
      action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
      /* TelephonyManager */
      TelephonyManager telephony =
        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

      telephony.listen(new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
//          Log.d(TAG, "onCallStateChanged, phoneNumber: " + phoneNumber);
          onCustomCallStateChanged(context, state, phoneNumber);
        }
      }, PhoneStateListener.LISTEN_CALL_STATE);
    }

//    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
//      == PackageManager.PERMISSION_GRANTED) {
//      telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        MyTelephonyCallback telephonyCallback = new MyTelephonyCallback(context);
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
//          == PackageManager.PERMISSION_GRANTED) {
//          telephony.registerTelephonyCallback(context.getMainExecutor(), telephonyCallback);
//        }
//      } else {
//        telephony.listen(new PhoneStateListener() {
//          @Override
//          public void onCallStateChanged(int state, String phoneNumber) {
//            onCustomCallStateChanged(context, state, phoneNumber);
//          }
//        }, PhoneStateListener.LISTEN_CALL_STATE);
//      }
//    }
  }

//  @RequiresApi(api = Build.VERSION_CODES.S)
//  public class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
//    private final Context context;
//
//    public MyTelephonyCallback(Context context) {
//      this.context = context;
//    }
//
//    @Override
//    public void onCallStateChanged(int state) {
//      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
//        ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//        return;
//      }
//      @SuppressLint("HardwareIds") String phoneNumber = telephony.getLine1Number();
//      Log.d(TAG, "phoneNumber: " + phoneNumber);
//      onCustomCallStateChanged(context, state, phoneNumber);
//    }
//  }

  //Derived classes should override these to respond to specific events of interest
  protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);

  protected abstract void onIncomingCallAnswered(Context ctx, String number, Date start);

  protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

  protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);

  protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

  protected abstract void onMissedCall(Context ctx, String number, Date start, Date end);

  protected abstract void onCustomReceive(Context ctx, Intent intent);

  //Deals with actual events

  //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
  //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
  public void onCustomCallStateChanged(Context context, int state, String number) {
    if (lastState == state) {
      //No change, debounce extras
      return;
    }
    switch (state) {
      case TelephonyManager.CALL_STATE_RINGING:
        isIncoming = true;
        callStartTime = new Date();
        onIncomingCallReceived(context, number, callStartTime);
        break;
      case TelephonyManager.CALL_STATE_OFFHOOK:
        //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
        if (lastState != TelephonyManager.CALL_STATE_RINGING) {
          isIncoming = false;
          callStartTime = new Date();
          onOutgoingCallStarted(context, number, callStartTime);
        } else {
          isIncoming = true;
          callStartTime = new Date();
          onIncomingCallAnswered(context, number, callStartTime);
        }

        break;
      case TelephonyManager.CALL_STATE_IDLE:
        //Went to idle-  this is the end of a call.  What type depends on previous state(s)
        if (lastState == TelephonyManager.CALL_STATE_RINGING) {
          //Ring but no pickup-  a miss
          onMissedCall(context, number, callStartTime, new Date());
        } else if (isIncoming) {
          onIncomingCallEnded(context, number, callStartTime, new Date());
        } else {
          onOutgoingCallEnded(context, number, callStartTime, new Date());
        }
        break;
    }
    lastState = state;
  }
}
