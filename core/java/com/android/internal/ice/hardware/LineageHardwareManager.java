/*
 * SPDX-FileCopyrightText: 2015-2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.internal.ice.hardware;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Range;

import com.android.internal.annotations.VisibleForTesting;

import com.android.internal.ice.hardware.AIDLHelper;

import vendor.lineage.touch.IGloveMode;
import vendor.lineage.touch.IKeyDisabler;
import vendor.lineage.touch.IKeySwapper;
import vendor.lineage.touch.IStylusMode;
import vendor.lineage.touch.ITouchscreenGesture;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Manages access to LineageOS hardware extensions
 *
 *  <p>
 *  This manager requires the HARDWARE_ABSTRACTION_ACCESS permission.
 *  <p>
 *  To get the instance of this class, utilize LineageHardwareManager#getInstance(Context context)
 */
public final class LineageHardwareManager {
    private static final String TAG = "LineageHardwareManager";

    // The VisibleForTesting annotation is to ensure Proguard doesn't remove these
    // fields, as they might be used via reflection. When the @Keep annotation in
    // the support library is properly handled in the platform, we should change this.

    /**
     * High touch sensitivity for touch panels
     */
    @VisibleForTesting
    public static final int FEATURE_HIGH_TOUCH_SENSITIVITY = 0x10;

    /**
     * Hardware navigation key disablement
     */
    @VisibleForTesting
    public static final int FEATURE_KEY_DISABLE = 0x20;

    /**
     * Hardware navigation key swapping
     */
    @VisibleForTesting
    public static final int FEATURE_KEY_SWAP = 0x40;

    /**
     * Touchscreen hovering
     */
    @VisibleForTesting
    public static final int FEATURE_TOUCH_HOVERING = 0x800;

    /**
     * Touchscreen gesture
     */
    @VisibleForTesting
    public static final int FEATURE_TOUCHSCREEN_GESTURES = 0x80000;

    private static final List<Integer> BOOLEAN_FEATURES = Arrays.asList(
        FEATURE_HIGH_TOUCH_SENSITIVITY,
        FEATURE_KEY_DISABLE,
        FEATURE_KEY_SWAP,
        FEATURE_TOUCH_HOVERING
    );

    private static LineageHardwareManager sLineageHardwareManagerInstance;

    private Context mContext;

    // AIDL hals
    private HashMap<Integer, IBinder> mAIDLMap = new HashMap<Integer, IBinder>();

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    private LineageHardwareManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
    }

    /**
     * Determine if a Lineage Hardware feature is supported on this device
     *
     * @param feature The Lineage Hardware feature to query
     *
     * @return true if the feature is supported, false otherwise.
     */
    public boolean isSupported(int feature) {
        return isSupportedAIDL(feature);
    }

    private boolean isSupportedAIDL(int feature) {
        if (!mAIDLMap.containsKey(feature)) {
            mAIDLMap.put(feature, getAIDLService(feature));
        }
        return mAIDLMap.get(feature) != null;
    }

    private IBinder getAIDLService(int feature) {
        switch (feature) {
            case FEATURE_HIGH_TOUCH_SENSITIVITY:
                return ServiceManager.waitForDeclaredService(
                        IGloveMode.DESCRIPTOR + "/default");
            case FEATURE_KEY_DISABLE:
                return ServiceManager.waitForDeclaredService(
                        IKeyDisabler.DESCRIPTOR + "/default");
            case FEATURE_KEY_SWAP:
                return ServiceManager.waitForDeclaredService(
                        IKeySwapper.DESCRIPTOR + "/default");
            case FEATURE_TOUCH_HOVERING:
                return ServiceManager.waitForDeclaredService(
                        IStylusMode.DESCRIPTOR + "/default");
            case FEATURE_TOUCHSCREEN_GESTURES:
                return ServiceManager.waitForDeclaredService(
                        ITouchscreenGesture.DESCRIPTOR + "/default");
        }
        return null;
    }

    /**
     * Get or create an instance of the {@link com.android.internal.custom.hardware.LineageHardwareManager}
     * @param context
     * @return {@link LineageHardwareManager}
     */
    public static LineageHardwareManager getInstance(Context context) {
        if (sLineageHardwareManagerInstance == null) {
            sLineageHardwareManagerInstance = new LineageHardwareManager(context);
        }
        return sLineageHardwareManagerInstance;
    }

    /**
     * Determine if the given feature is enabled or disabled.
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the Lineage Hardware feature to query
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean get(int feature) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (isSupportedAIDL(feature)) {
                IBinder b = mAIDLMap.get(feature);
                switch (feature) {
                    case FEATURE_HIGH_TOUCH_SENSITIVITY:
                        return IGloveMode.Stub.asInterface(b).getEnabled();
                    case FEATURE_KEY_DISABLE:
                        return IKeyDisabler.Stub.asInterface(b).getEnabled();
                    case FEATURE_KEY_SWAP:
                        return IKeySwapper.Stub.asInterface(b).getEnabled();
                    case FEATURE_TOUCH_HOVERING:
                        return IStylusMode.Stub.asInterface(b).getEnabled();
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Enable or disable the given feature
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the Lineage Hardware feature to set
     * @param enable true to enable, false to disale
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean set(int feature, boolean enable) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (isSupportedAIDL(feature)) {
                IBinder b = mAIDLMap.get(feature);
                switch (feature) {
                    case FEATURE_HIGH_TOUCH_SENSITIVITY:
                        IGloveMode.Stub.asInterface(b).setEnabled(enable);
                        break;
                    case FEATURE_KEY_DISABLE:
                        IKeyDisabler.Stub.asInterface(b).setEnabled(enable);
                        break;
                    case FEATURE_KEY_SWAP:
                        IKeySwapper.Stub.asInterface(b).setEnabled(enable);
                        break;
                    case FEATURE_TOUCH_HOVERING:
                        IStylusMode.Stub.asInterface(b).setEnabled(enable);
                        break;
                }
                return enable;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * @return a list of available touchscreen gestures on the devices
     */
    public TouchscreenGesture[] getTouchscreenGestures() {
        try {
            if (isSupportedAIDL(FEATURE_TOUCHSCREEN_GESTURES)) {
                ITouchscreenGesture touchscreenGesture = ITouchscreenGesture.Stub.asInterface(
                        mAIDLMap.get(FEATURE_TOUCHSCREEN_GESTURES));
                return AIDLHelper.fromAIDLGestures(touchscreenGesture.getSupportedGestures());
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * @return true if setting the activation status was successful
     */
    public boolean setTouchscreenGestureEnabled(
            TouchscreenGesture gesture, boolean state) {
        try {
            if (isSupportedAIDL(FEATURE_TOUCHSCREEN_GESTURES)) {
                ITouchscreenGesture touchscreenGesture = ITouchscreenGesture.Stub.asInterface(
                        mAIDLMap.get(FEATURE_TOUCHSCREEN_GESTURES));
                touchscreenGesture.setGestureEnabled(AIDLHelper.toAIDLGesture(gesture), state);
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }
}
