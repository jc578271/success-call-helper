package standard.inc.success.call.helper;

public abstract class BroadcastAction {
  public static final String IncomingCallReceived = "IncomingCallReceived";
  public static final String IncomingCallAnswered = "IncomingCallAnswered";
  public static final String IncomingCallEnded = "IncomingCallEnded";
  public static final String OutgoingCallStarted = "OutgoingCallStarted";
  public static final String OutgoingCallEnded = "OutgoingCallEnded";
  public static final String MissedCall = "MissedCall";

  public static final String onIncomingCallReceived = "onIncomingCallReceived";
  public static final String onBlockRecordPhoneCall = "onBlockRecordPhoneCall";
  public static final String onIncomingCallRecorded = "onIncomingCallRecorded";
  public static final String onOutgoingCallRecorded = "onOutgoingCallRecorded";
  public static final String onMissedCall = "onMissedCall";
}
