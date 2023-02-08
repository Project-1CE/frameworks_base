/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.gmscompat;

import android.app.Application;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";

    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";
    private static final String PACKAGE_FINSKY = "com.android.vending";

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

    private AttestationHooks() { }

    private static void setBuildField(String key, Object value) {
        try {
            // Unlock
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);

            // Edit
            field.set(null, value);

            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionField(String key, Integer value) {
        try {
            // Unlock
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);

            // Edit
            field.set(null, value);

            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    public static void initApplicationBeforeOnCreate(Application app) {
        final String packageName = app.getPackageName();
        final String processName = Application.getProcessName();

        if (PACKAGE_GMS.equals(packageName) && PROCESS_UNSTABLE.equals(processName)) {
            sIsGms = true;
            setBuildField("DEVICE", "walleye");
            setBuildField("PRODUCT", "walleye");
            setBuildField("MODEL", "Pixel 2");
            setBuildField("FINGERPRINT", "google/walleye/walleye:8.1.0/OPM1.171019.011/4448085:user/release-keys");
            setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.O);
        } else if (PACKAGE_FINSKY.equals(packageName)) {
            sIsFinsky = true;
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            throw new UnsupportedOperationException();
        }
    }
}
