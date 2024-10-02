package standard.inc.success.call.helper;

public abstract class BroadcastAction {
  public static final String onRecordEnabled = "onRecordEnabled";
  public static final String onCallStateChange = "onCallStateChange";

  public static final String INCOMING_CALL_RECEIVED = "INCOMING_CALL_RECEIVED";
  public static final String INCOMING_CALL_ENDED = "INCOMING_CALL_ENDED";
  public static final String INCOMING_CALL_ANSWERED = "INCOMING_CALL_ANSWERED";

  public static final String OUTGOING_CALL_STARTED = "OUTGOING_CALL_STARTED";
  public static final String OUTGOING_CALL_ENDED = "OUTGOING_CALL_ENDED";
  public static final String OUTGOING_CALL_MISSED = "OUTGOING_CALL_MISSED";
}
