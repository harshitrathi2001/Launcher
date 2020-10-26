/*
 *   Copyright (C) 2015. Kumaresan Rajeswaran
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.android.launcher;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import androidx.collection.ArrayMap;

/**
 * Settings for launcher
 */
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // register shared prefs listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        ListPreference lpIconThemePref = (ListPreference) findPreference(getString(R.string.pref_icon_theme_key));

        displayIconThemePref(lpIconThemePref);

        displayAboutDialog();
    }

    private void displayAboutDialog() {
        findPreference(getString(R.string.pref_about)).
                setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String about = String.format(getString(R.string.about_application), BuildConfig.VERSION_NAME);

                        AlertDialog d = new AlertDialog.Builder(SettingsActivity.this)
                                .setIcon(R.mipmap.ic_launcher_home)
                                .setCancelable(true)
                                .setTitle(R.string.application_name)
                                .setMessage(Html.fromHtml(about))
                                .create();
                        d.show();
                        ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                        return false;
                    }
                });
    }

    private void displayIconThemePref(ListPreference lpIconSetting) {
        ArrayMap<String, String> iconPacks = LauncherAppState.getInstance().getIconCache().getAvailableIconPacks();

        if (iconPacks != null && iconPacks.size() != 0) {
            String iconPackValues[] = iconPacks.keySet().toArray(new String[iconPacks.size() + 1]);
            iconPackValues[iconPacks.size()] = getString(R.string.pref_no_icon_theme);

            String[] iconPackTitles = new String[iconPacks.size() + 1];
            for (int i = 0; i < iconPacks.size(); i++) {
                iconPackTitles[i] = iconPacks.get(iconPacks.keyAt(i));
            }
            iconPackTitles[iconPacks.size()] = getString(R.string.pref_no_icon_theme);

            lpIconSetting.setEntries(iconPackTitles);
            lpIconSetting.setEntryValues(iconPackValues);
        } else {
            lpIconSetting.setEnabled(false);
            lpIconSetting.setShouldDisableView(true);
            lpIconSetting.setSummary(R.string.pref_icon_theme_summary_disabled);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_icon_theme_key).equals(key)) {
            final IconCache iconCache = LauncherAppState.getInstance().getIconCache();
            final String changedIconTheme = sharedPreferences.getString(key, "");
            if (!changedIconTheme.equals(iconCache.getCurrentIconTheme())) {
                Log.d(TAG, "Refreshing icon theme for = " + changedIconTheme);
                iconCache.setCurrentIconTheme(
                        (changedIconTheme.equals(getString(R.string.pref_no_icon_theme))) ?
                                null :
                                changedIconTheme);
                iconCache.flush();

                // need to make the user wait until icon pack's loaded
                new AsyncTask<Void, Void, Void>() {
                    // modal dialog is more appropriate
                    private ProgressDialog dialog;

                    @Override
                    protected void onPreExecute() {
                        if (!SettingsActivity.this.isFinishing()) {
                            dialog = ProgressDialog.show(SettingsActivity.this, "",
                                    getString(R.string.dialog_refresh_icons), true);
                        }
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if (!SettingsActivity.this.isFinishing()) {
                            dialog.dismiss();
                        }
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        iconCache.loadIconPackDrawables();
                        LauncherAppState.getInstance().getModel().rebindItemsOnIconThemeChange();
                        return null;
                    }
                }.execute();
            }
        }
    }

}
