package com.develop.measurear;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.CheckBoxPreference ;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import android.provider.MediaStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

   private EditTextPreference img_1,img_2;
   private boolean lunch1=false,lunch2=false;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_pref);


        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();

        int count = prefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            // You don't need to set up preference summaries for checkbox preferences because
            // they are already set up in xml using summaryOff and summary On
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }



        Preference img1,img2;
        img1 = findPreference(getString(R.string.pref_image_one_key));
        img1.setOnPreferenceClickListener(preference1 -> {

            lunch1=true;

            Intent galleryIntent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent , 0 );
            return true;
        });

        img2 = findPreference(getString(R.string.pref_image_two_key));
        img2.setOnPreferenceClickListener(preference12 -> {
            lunch2=true;

            Intent galleryIntent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent , 0 );
            return true;
        });


        img_1 = findPreference(getString(R.string.pref_external_path_one_key));
        img_2 = findPreference(getString(R.string.pref_external_path_two_key));



    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


                if (null != data && lunch1) {

                    img_1.setText(getPath(getContext(), data.getData()));
                    lunch1=false;

                }

        else if (null != data && lunch2) {
                    img_2.setText(getPath(getContext(), data.getData()));
                    lunch2=false;

                }


    }

    public static String getPath(final Context context, final Uri uri) {
        String filePath = "";

        Pattern p = Pattern.compile("(\\d+)$");
        Matcher m = p.matcher(uri.toString());
        if (!m.find()) {
            return filePath;
        }
        String imgId = m.group();

        String[] column = { MediaStore.Images.Media.DATA };
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, sel, new String[]{ imgId }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();

        return filePath;



    }



    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Figure out which preference was changed
        Preference preference = findPreference(key);
        if (null != preference) {
            // Updates the summary for the preference
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }

/*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        SharedPreferences.Editor editor = prefs.edit();

        if (preference instanceof Preference) {
            if(preference.getKey().equals("image_one") && null != img1) {
                String value = sharedPreferences.getString(preference.getKey(), img1.getSummary().toString());

                editor.putString(preference.getKey(), value);
                editor.commit();
            }
            else if(preference.getKey().equals("image_two") && null != img2) {
                String value = sharedPreferences.getString(preference.getKey(), img2.getSummary().toString());

                editor.putString(preference.getKey(), value);
                editor.commit();
            }
        }*/
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            // For list preferences, figure out the label of the selected value
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                // Set the summary to that label
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            // For EditTextPreferences, set the summary to the value's simple string representation.
            preference.setSummary(value);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
