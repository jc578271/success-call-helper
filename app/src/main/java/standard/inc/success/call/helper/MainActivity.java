package standard.inc.success.call.helper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

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

    /* open accessibility settings */
    Button openAccessibilitySettingButton = findViewById(R.id.openAccessibilitySetting);
    openAccessibilitySettingButton.setOnClickListener(v -> {
      Log.d(TAG, "openAccessibilitySettingButton: clicked");
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    });

    /* open info settings */
    Button openInfoSettingButton = findViewById(R.id.openInfoSetting);
    openInfoSettingButton.setOnClickListener(v -> {
      Log.d(TAG, "openInfoSettingButton: clicked");
//      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//      intent.setData(Uri.parse("package:" + getPackageName()));
      Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  /* Permissions */
  private String[] providePermissions() {
    List<String> permissionsList = new Vector<String>() {{
      add(Manifest.permission.RECORD_AUDIO);
      add(Manifest.permission.READ_PHONE_STATE);
      add(Manifest.permission.READ_CALL_LOG);
      add(Manifest.permission.PROCESS_OUTGOING_CALLS);
//      add(Manifest.permission.READ_SMS);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        add(Manifest.permission.READ_PHONE_NUMBERS);
      }
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