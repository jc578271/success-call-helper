package standard.inc.success.call.helper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
    Button openSettingButton = findViewById(R.id.openSetting);
    openSettingButton.setOnClickListener(v -> {
      Log.d(TAG, "openSettingButton: clicked");
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
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