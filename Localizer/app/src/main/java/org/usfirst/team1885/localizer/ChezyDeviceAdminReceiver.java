package org.usfirst.team1885.localizer;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;

public class ChezyDeviceAdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), ChezyDeviceAdminReceiver.class);
    }

}