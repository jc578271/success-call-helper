package standard.inc.success.call.helper;

public abstract class Constants {
  public static final String onRecordEnabled = "onRecordEnabled";
  public static final String getRecordStatus = "getRecordStatus";
  public static final String getAccessibilityStatus = "getAccessibilityStatus";
  public static final String onAccessibilityEnabled = "onAccessibilityEnabled";
  public static final String onCallStateChange = "onCallStateChange";

  public static final String INCOMING_CALL_RECEIVED = "INCOMING_CALL_RECEIVED";
  public static final String INCOMING_CALL_ENDED = "INCOMING_CALL_ENDED";
  public static final String INCOMING_CALL_ANSWERED = "INCOMING_CALL_ANSWERED";

  public static final String OUTGOING_CALL_STARTED = "OUTGOING_CALL_STARTED";
  public static final String OUTGOING_CALL_ANSWERED = "OUTGOING_CALL_ANSWERED";
  public static final String OUTGOING_CALL_ENDED = "OUTGOING_CALL_ENDED";
  public static final String CALL_MISSED = "CALL_MISSED";

  public static final int IS_OUTGOING_CALL = 1;
  public static final int IS_INCOMING_CALL = 2;

  public static final int CALL_INIT = 0;
  public static final int CALL_RINGING = 1;
  public static final int CALL_CONNECTED = 2;
}
