package de.daniel.mobilepauker2.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.preference.ListPreference
import android.provider.Settings.*
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceFragmentCompat
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.*
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.MinFilter
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val preferenceScreen: PreferenceScreen = preferenceScreen
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        init(preferenceScreen)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        TODO("Not yet implemented")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePrefSummary(findPreference(key!!))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == Activity.RESULT_OK) {
            initSyncPrefs()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePrefSummary(findPreference(settingsManager.getSettingsKey(RING_TONE)))
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                init(preference.getPreference(i))
            }
        } else {
            if (preference is EditTextPreference) {
                preference.setOnBindEditTextListener { editText ->
                    editText.addTextChangedListener(MinFilter(preference))
                }
            }
            updatePrefSummary(preference)
        }
        findPreference<Preference>(settingsManager.getSettingsKey(RING_TONE))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(EXTRA_APP_PACKAGE, context?.packageName)
                intent.putExtra(EXTRA_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_ID)
                startActivity(intent)
                true
            }
    }

    private fun updatePrefSummary(preference: Preference?) {
        if (preference == null) return

        var summ = preference.summary
        val context = context
        if (summ != null) {
            if (preference is EditTextPreference) {
                val editTextP = preference
                if (editTextP.key == settingsManager.getSettingsKey(USTM))
                    summ = getString(R.string.ustm_summ)
                else if (editTextP.key == settingsManager.getSettingsKey(STM))
                    summ = getString(R.string.stm_summ)
                editTextP.summary = String.format(summ.toString(), editTextP.text)
            } else if (preference is ListPreference) {
                when (preference.key) {
                    settingsManager.getSettingsKey(REPEAT_CARDS) -> {
                        summ = getString(R.string.repeat_cards_summ)
                    }
                    settingsManager.getSettingsKey(RETURN_FORGOTTEN_CARDS) -> {
                        summ = getString(R.string.return_forgotten_cards_summ)
                    }
                    settingsManager.getSettingsKey(FLIP_CARD_SIDES) -> {
                        summ = getString(R.string.flip_card_sides_summ)
                    }
                }
                preference.summary = String.format(summ.toString(), preference.entry)
            }
        }
        val preferenceKey = preference.key
        if (preferenceKey != null) {
            if (preferenceKey == settingsManager.getSettingsKey(DB_PREFERENCE)) {
                initSyncPrefs()
            } else if (preferenceKey == settingsManager.getSettingsKey(RING_TONE)) {
                val notificationManager =
                    context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ringtonePath =
                    notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID).sound
                val ringtone = RingtoneManager.getRingtone(context, ringtonePath)
                preference.summary = ringtone.getTitle(context)
            }
        }
    }

    private fun removeSyncPrefAndSetAutoSync(enableAutoSync: Boolean) {
        val switchUp =
            findPreference(settingsManager.getSettingsKey(AUTO_UPLOAD)) as SwitchPreference?
        val switchDown =
            findPreference(settingsManager.getSettingsKey(AUTO_DOWNLOAD)) as SwitchPreference?
        if (enableAutoSync) {
            switchUp?.setSummary(R.string.auto_sync_enabled_upload_summ)
            switchDown?.setSummary(R.string.auto_sync_enabled_download_summ)
        } else {
            switchUp?.setSummary(R.string.auto_sync_disabled_summ)
            switchDown?.setSummary(R.string.auto_sync_disabled_summ)
        }
        switchUp?.isEnabled = enableAutoSync
        switchDown?.isEnabled = enableAutoSync
    }

    private fun initSyncPrefs() {
        Log.d("SettingsFragment::initSyncPrefs", "init syncprefs")
        val dbPref: Preference? =
            findPreference(settingsManager.getSettingsKey(DB_PREFERENCE))
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val accessToken = pref.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        if (accessToken == null) {
            setPrefAss(dbPref)
        } else {
            pref.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply()
            setPrefUnlink(dbPref)
            Log.d("SettingsFragment::initSyncPrefs", "enable autosync")
            removeSyncPrefAndSetAutoSync(true)
        }
    }

    private fun setPrefAss(dbPref: Preference?) {
        dbPref?.setTitle(R.string.associate_dropbox_title)
        //val assIntent = Intent(DropboxAccDialog::class.java) TODO
        //assIntent.putExtra(DropboxAccDialog.AUTH_MODE, true) TODO
        dbPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //startActivityForResult(assIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG) TODO
            false
        }
        Log.d("SettingsFragment::initSyncPrefs", "disable autosync")
        removeSyncPrefAndSetAutoSync(false)
    }

    private fun setPrefUnlink(dbPref: Preference?) {
        dbPref?.setTitle(R.string.unlink_dropbox_title)
        //val unlIntent = Intent(context, DropboxAccDialog::class.java) TODO
        //unlIntent.putExtra(DropboxAccDialog.UNL_MODE, true) TODO
        dbPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Log.d("SettingsFragment::initSyncPrefs", "unlinkDB clicked")
            //startActivityForResult(unlIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG) TODO
            false
        }
    }
}