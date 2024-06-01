package org.acoustixaudio.tonesmith;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.shajikhan.ladspa.amprack.AudioEngine;

import java.util.ArrayList;
import java.util.StringJoiner;

public class SettingsActivity extends AppCompatActivity {
    AudioManager audioManager = null ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private AudioManager audioManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
            int defaultInputDevice = 0 ;
            int defaultOutputDevice = 0 ;

            ListPreference listPreference = findPreference("input");
            ListPreference listPreferenceOutput = findPreference("output");

            listPreferenceOutput.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    MainActivity.applySettings();
                    return false;
                }
            });

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    MainActivity.applySettings();
                    return false;
                }
            });

            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> entryValues = new ArrayList<>();
            SettingsActivity settingsActivity = (SettingsActivity) getActivity();
            audioManager = settingsActivity.audioManager;
            if (audioManager == null) {
                Toast.makeText(settingsActivity, "Cannot connect to audio manager", Toast.LENGTH_SHORT).show();
                return;
            }

            audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
            audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

            entries.add("default");
            entryValues.add("-1");

            String TITLE_TAG = getClass().getSimpleName();
            for (int i = 0; i < audioDevicesInput.length ; i ++) {
                String name = typeToString(audioDevicesInput[i].getType());
                int deviceID = audioDevicesInput[i].getId();
//                name = (String) audioDevicesInput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesInput[i].getProductName() + ")");
//                entryValues.add(String.valueOf(audioDevicesInput[i]));
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    AudioEngine.setRecordingDeviceId(new Integer(newValue.toString()));
                    return true;
                }
            });

            entries.clear();
            entryValues.clear();

            entries.add("default");
            entryValues.add("-1");

            for (int i = 0 ; i < audioDevicesOutput.length ; i ++) {
                String name = typeToString(audioDevicesOutput[i].getType());
                int deviceID = audioDevicesOutput[i].getId();
//                name = (String) audioDevicesOutput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesOutput[i].getProductName() + ")");
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreferenceOutput.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreferenceOutput.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreferenceOutput.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(getClass().getSimpleName(), "onPreferenceChange: [playbackDeviceID] " + newValue.toString());
//                    AudioEngine.setPlaybackDeviceId(Integer.parseInt(newValue.toString()));
                    return true;
                }
            });

            Preference privacy = findPreference("privacy") ;
            privacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String url = "https://amprack.acoustixaudio.org/privacy.php";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return false;
                }
            });

            Preference source = findPreference("source") ;
            source.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String url = "https://amprack.acoustixaudio.org/";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return false;
                }
            });

            Preference lamePreset = findPreference("lame_preset");
            lamePreset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(settingsActivity);

                    AudioEngine.setLamePreset (Integer.parseInt(prefs.getString("lame_preset", "1001")));
                    return false;
                }
            });

            Preference buy = findPreference("purchase");
            Preference about = findPreference("about");
            about.setSummary(
                    new StringJoiner(" ")
                            .add("Version")
                            .add (BuildConfig.VERSION_NAME)
                            .add("Build")
                            .add(String.valueOf(BuildConfig.VERSION_CODE))
                            .toString()
            );

            if (MainActivity.proVersion) {
                buy.setVisible(false);
                about.setTitle(about.getTitle() + " Premium");
            }

            buy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    startActivity(new Intent(settingsActivity, Purchase.class));
                    return false;
                }
            });
        }
    }

    static String typeToString(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "auxiliary line-level connectors";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth device supporting the A2DP profile";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth device typically used for telephony";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "built-in earphone speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "built-in microphone";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "built-in speaker";
            case AudioDeviceInfo.TYPE_BUS:
                return "BUS";
            case AudioDeviceInfo.TYPE_DOCK:
                return "DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "FM";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "FM tuner";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "HDMI audio return channel";
            case AudioDeviceInfo.TYPE_IP:
                return "IP";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "line analog";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "line digital";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "telephony";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TV tuner";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB accessory";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB device";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wired headphones";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "wired headset";
            default:
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "unknown";
        }
    }
}