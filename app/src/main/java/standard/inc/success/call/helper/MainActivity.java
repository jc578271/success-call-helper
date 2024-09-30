package standard.inc.success.call.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
}