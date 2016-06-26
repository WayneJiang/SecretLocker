package neverland.com.secretlocker;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String KEY_LISTPREFERENCE_ENCRYPT_STRENGTG = "key_listpreference_strength";
    private ListPreference mListPreference_EncryptStrength;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_items);

        mListPreference_EncryptStrength = (ListPreference) findPreference(KEY_LISTPREFERENCE_ENCRYPT_STRENGTG);
        mListPreference_EncryptStrength.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int index = mListPreference_EncryptStrength.findIndexOfValue(newValue.toString());
        CharSequence[] selectText = mListPreference_EncryptStrength.getEntries();
        mListPreference_EncryptStrength.setSummary(selectText[index]);
        return true;
    }
}
