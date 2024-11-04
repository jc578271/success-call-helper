package standard.inc.success.call.helper;

import android.content.Context;
import android.content.ComponentName;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import java.util.List;

public class AccessibilityServiceUtils {

  public static boolean isAccessibilityServiceEnabled(Context context) {
    // Construct the expected ComponentName for the service
    ComponentName expectedComponentName = new ComponentName(context, RecordAccessibilityService.class);
    String expectedServiceName = expectedComponentName.flattenToString();

    // Get the AccessibilityManager
    AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

    // Retrieve the list of enabled Accessibility Services
    List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

    // Iterate through the list to check if your service is enabled
    for (AccessibilityServiceInfo serviceInfo : enabledServices) {
      ComponentName componentName = new ComponentName(
        serviceInfo.getResolveInfo().serviceInfo.packageName,
        serviceInfo.getResolveInfo().serviceInfo.name
      );
      String serviceName = componentName.flattenToString();
      if (TextUtils.equals(expectedServiceName, serviceName)) {
        return true;
      }
    }
    return false;
  }
}

