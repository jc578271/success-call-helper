package standard.inc.success.call.helper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private TextView accessibilityStatusTextView;
  private AccessibilityContentObserver accessibilityContentObserver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    String[] permissionsList = providePermissions();
    if (!hasPermissions(this, permissionsList)) {
      ActivityCompat.requestPermissions(this, permissionsList, 100);
    }

    accessibilityStatusTextView = findViewById(R.id.accessibilityStatusTextView);

    /* open accessibility settings */
    Button openSettingButton = findViewById(R.id.openSetting);
    openSettingButton.setOnClickListener(v -> {
      Log.d(TAG, "openSettingButton: clicked");
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      startActivity(intent);
    });

    accessibilityContentObserver = new AccessibilityContentObserver(new Handler());
    getContentResolver().registerContentObserver(
      Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
      false,
      accessibilityContentObserver);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (accessibilityContentObserver != null) {
      getContentResolver().unregisterContentObserver(accessibilityContentObserver);
    }
  }

  @SuppressLint("SetTextI18n")
  private void updateAccessibilityStatus() {
    boolean isEnabled = isAccessibilityServiceEnabled();

    if (isEnabled) {
      accessibilityStatusTextView.setText("Your Accessibility Service is enabled");
    } else {
      accessibilityStatusTextView.setText("Your Accessibility Service is disabled");
    }
  }

  private boolean isAccessibilityServiceEnabled() {
    Context context = this;
    String serviceId = context.getPackageName() + "/" + RecordAccessibilityService.class.getName();

    String enabledServices = Settings.Secure.getString(
      context.getContentResolver(),
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    );

    // Check if the service is enabled
    return !TextUtils.isEmpty(enabledServices) &&
      enabledServices.contains(serviceId) &&
      Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;
  }

  private class AccessibilityContentObserver extends ContentObserver {
    AccessibilityContentObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      updateAccessibilityStatus();
    }
  }

  private String[] providePermissions() {
    List<String> permissionsList = new Vector() {{
      add(android.Manifest.permission.RECORD_AUDIO);
    }};
    String[] list = new String[permissionsList.size()];
    return permissionsList.toArray(list);
  }

  private static boolean hasPermissions(Context context, final String... permissions) {
    if (context != null && permissions != null) {
      for (String permission : permissions) {
        if (PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(context, permission)) {
          return false;
        }
      }
    }
    return true;
  }
}