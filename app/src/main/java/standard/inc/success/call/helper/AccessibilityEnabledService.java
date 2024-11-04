package standard.inc.success.call.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class AccessibilityEnabledService extends Service {
  private static final String TAG = "AServiceStatus";

  @Override
  public void onCreate() {
    super.onCreate();
    createForeGround(1234);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent.getAction();
    if (action != null) {
      Log.d(TAG, "onStartCommand, action: " + action);
      if (action.equals(Constants.getAccessibilityStatus)) {
        boolean accessibilityEnabled = AccessibilityServiceUtils.isAccessibilityServiceEnabled(getApplicationContext());
        sendAccessibilityStatus(accessibilityEnabled);
      }
    }

    return START_NOT_STICKY;
  }

  private void sendAccessibilityStatus(boolean accessibilityEnabled) {
    String packageName = getPackageName();
    Log.d(TAG, "sendAccessibilityStatus: " + packageName + " : " + Constants.onAccessibilityEnabled + ", accessibilityEnabled: " + accessibilityEnabled);

    Intent params = new Intent();
    params.putExtra("accessibilityEnabled", accessibilityEnabled);
    params.setAction(packageName + "." + Constants.onAccessibilityEnabled);
    sendBroadcast(params);
    stopSelf();
  }

  public void createForeGround(int notificationId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification myNotification = new Notification.Builder(getApplicationContext(), createChannel()).build();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(notificationId, myNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
      } else {
        startForeground(notificationId, myNotification);
      }

    } else {
      //noinspection deprecation
      Notification myNotification = new NotificationCompat.Builder(getApplicationContext()).build();
      startForeground(notificationId, myNotification);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private String createChannel() {
    String channelId = "HELPER_SERVICE_CHANNEL_ID";
    CharSequence channelName = "Helper service Channel";
    int importance = NotificationManager.IMPORTANCE_DEFAULT;

    NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);

    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.createNotificationChannel(notificationChannel);

    return channelId;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
