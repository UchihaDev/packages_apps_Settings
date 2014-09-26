/*
 * Copyright (C) 2010 The Android Open Source Project
 * Code has been modified - Copyright (C) 2014 Corey "Mr. Apocalypse" Edwards
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

package com.android.settings.apocalypse;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.view.RotationPolicy;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.DreamSettings;
import com.android.settings.slim.DisplayRotation;
import com.android.settings.apocalypse.AppMultiSelectListPreference;
import com.android.settings.cyanogenmod.NEWSeekBarPreference;
import java.util.HashSet;
import java.util.Set;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import com.android.settings.util.Helpers;

public class UserInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
	private static final String TAG = "UI Settings";	
		
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
	private static final String PREF_ENABLE_APP_CIRCLE_BAR = "enable_app_circle_bar";
	private static final String PREF_INCLUDE_APP_CIRCLE_BAR_KEY = "app_circle_bar_included_apps";
	private static final String KEY_TRIGGER_WIDTH = "trigger_width";
    private static final String KEY_TRIGGER_TOP = "trigger_top";
    private static final String KEY_TRIGGER_BOTTOM = "trigger_bottom";
	private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
	private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
	private static final String PREF_SHOW_HEADS_UP_BOTTOM = "show_heads_up_bottom";
	
    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";
	
	int expandedDesktopValue;
	
	
    private PreferenceScreen mDisplayRotationPreference;
	private AppMultiSelectListPreference mIncludedAppCircleBar;
	private NEWSeekBarPreference mTriggerWidthPref;
    private NEWSeekBarPreference mTriggerTopPref;
    private NEWSeekBarPreference mTriggerBottomPref;
	private CheckBoxPreference mEnableAppCircleBar;
	private ListPreference mExpandedDesktopPref;
	private CheckBoxPreference mExpandedDesktopNoNavbarPref;
	private CheckBoxPreference mShowHeadsUpBottom;
	
    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.ui_settings);
		
		PreferenceScreen prefScreen = getPreferenceScreen();

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            getPreferenceScreen().removePreference(mDisplayRotationPreference);
        }
		// App circle bar
		mEnableAppCircleBar = (CheckBoxPreference) findPreference(PREF_ENABLE_APP_CIRCLE_BAR);
		mEnableAppCircleBar.setChecked((Settings.System.getInt(getContentResolver(),
		Settings.System.ENABLE_APP_CIRCLE_BAR, 0) == 1));

		mIncludedAppCircleBar = (AppMultiSelectListPreference) findPreference(PREF_INCLUDE_APP_CIRCLE_BAR_KEY);
		Set<String> includedApps = getIncludedApps();
		if (includedApps != null) mIncludedAppCircleBar.setValues(includedApps);
		mIncludedAppCircleBar.setOnPreferenceChangeListener(this);
		
		mTriggerWidthPref = (NEWSeekBarPreference) findPreference(KEY_TRIGGER_WIDTH);
		mTriggerWidthPref.setValue(Settings.System.getInt(getContentResolver(),
		Settings.System.APP_CIRCLE_BAR_TRIGGER_WIDTH, 10));
		mTriggerWidthPref.setOnPreferenceChangeListener(this);

		mTriggerTopPref = (NEWSeekBarPreference) findPreference(KEY_TRIGGER_TOP);
		mTriggerTopPref.setValue(Settings.System.getInt(getContentResolver(),
		Settings.System.APP_CIRCLE_BAR_TRIGGER_TOP, 0));
		mTriggerTopPref.setOnPreferenceChangeListener(this);

		mTriggerBottomPref = (NEWSeekBarPreference) findPreference(KEY_TRIGGER_BOTTOM);
		mTriggerBottomPref.setValue(Settings.System.getInt(getContentResolver(),
		Settings.System.APP_CIRCLE_BAR_TRIGGER_HEIGHT, 100));
		mTriggerBottomPref.setOnPreferenceChangeListener(this);
		
		// Expanded desktop
        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopNoNavbarPref =
                (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);

        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);
		
		//Show HeadsUp at bottom		
		mShowHeadsUpBottom = (CheckBoxPreference) findPreference(PREF_SHOW_HEADS_UP_BOTTOM);
        mShowHeadsUpBottom.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.SHOW_HEADS_UP_BOTTOM, 0, UserHandle.USER_CURRENT) == 1);
        mShowHeadsUpBottom.setOnPreferenceChangeListener(this);
				
		try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

            if (hasNavBar) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);
                prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
            } else {
                // Hide no-op "Status bar visible" expanded desktop mode
                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                prefScreen.removePreference(mExpandedDesktopPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }		
    }
	
    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
        updateDisplayRotationPreferenceDescription();
		Settings.System.putInt(getContentResolver(),
		Settings.System.APP_CIRCLE_BAR_SHOW_TRIGGER, 1);
        
    }
	
    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
		Settings.System.putInt(getContentResolver(), Settings.System.APP_CIRCLE_BAR_SHOW_TRIGGER, 0);
    }
	
    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            return;
        }
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;

        int allowAllRotations = getResources().
                getBoolean(com.android.internal.R.bool.config_allowAllRotations) ? 1 : 0;

        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_ANGLES, -1);
        if (mode < 0) {
            // defaults
            mode = allowAllRotations == 1 ?
                    (DisplayRotation.ROTATION_0_MODE | DisplayRotation.ROTATION_90_MODE |
                            DisplayRotation.ROTATION_180_MODE | DisplayRotation.ROTATION_270_MODE) : // All angles
                    (DisplayRotation.ROTATION_0_MODE | DisplayRotation.ROTATION_90_MODE |
                            DisplayRotation.ROTATION_270_MODE); // All except 180
        }

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for (int i = 0; i < rotationList.size(); i++) {
                summary.append(delim).append(rotationList.get(i));
                if ((rotationList.size() - i) > 2) {
                    delim = ", ";
                } else {
                    delim = " & ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        preference.setSummary(summary);
    }
	
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
       if (preference == mEnableAppCircleBar) {
	    	boolean checked = ((CheckBoxPreference)preference).isChecked();
	    	Settings.System.putInt(getContentResolver(),
			Settings.System.ENABLE_APP_CIRCLE_BAR, checked ? 1:0);
		}				
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
	
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mIncludedAppCircleBar) {
	    	storeIncludedApps((Set<String>) objValue);
		} else if (preference == mTriggerWidthPref) {
	    	int width = ((Integer)objValue).intValue();
	    	Settings.System.putInt(getContentResolver(),
			Settings.System.APP_CIRCLE_BAR_TRIGGER_WIDTH, width);
	    	return true;
		} else if (preference == mTriggerTopPref) {
	    	int top = ((Integer)objValue).intValue();
	    	Settings.System.putInt(getContentResolver(),
			Settings.System.APP_CIRCLE_BAR_TRIGGER_TOP, top);
	    	return true;
		} else if (preference == mTriggerBottomPref) {
	    	int bottom = ((Integer)objValue).intValue();
	    	Settings.System.putInt(getContentResolver(),
			Settings.System.APP_CIRCLE_BAR_TRIGGER_HEIGHT, bottom);
	    	return true;
		} else if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            updateExpandedDesktop(expandedDesktopValue);
            return true;
		} else if (preference == mExpandedDesktopNoNavbarPref) {
            boolean value = (Boolean) objValue;
            updateExpandedDesktop(value ? 2 : 0);
            return true;
        } else if (preference == mShowHeadsUpBottom) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.SHOW_HEADS_UP_BOTTOM,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            Helpers.restartSystemUI();
            return true;
		}			

        return true;
    }
	private Set<String> getIncludedApps() {
		String included = Settings.System.getString(getActivity().getContentResolver(),
				Settings.System.WHITELIST_APP_CIRCLE_BAR);
		if (TextUtils.isEmpty(included)) {
			return null;
		}
		return new HashSet<String>(Arrays.asList(included.split("\\|")));
    }
	
	private void storeIncludedApps(Set<String> values) {
		StringBuilder builder = new StringBuilder();
		String delimiter = "";
		for (String value : values) {
			builder.append(delimiter);
			builder.append(value);
			delimiter = "|";
		}
		Settings.System.putString(getActivity().getContentResolver(),
			Settings.System.WHITELIST_APP_CIRCLE_BAR, builder.toString());
    }
	
	private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }
}