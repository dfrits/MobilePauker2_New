package de.daniel.mobilepauker2.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings.*
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.*
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.MinFilter
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var dialogFragment: DialogFragment? = null
    private val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context?.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        addPreferencesFromResource(R.xml.preferences)
        val preferenceScreen: PreferenceScreen = preferenceScreen
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        init(preferenceScreen)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

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

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (activity is PaukerSettings) {
            val dialog: DialogFragment = when (preference) {
                is EditTextPreference -> {
                    EditTextPreferenceDialogFragmentCompat.newInstance(preference.key)
                }
                is ListPreference -> {
                    ListPreferenceDialogFragmentCompat.newInstance(preference.key)
                }
                else -> {
                    throw IllegalArgumentException(
                        "Tried to display dialog for unknown " +
                                "preference type. Did you forget to override onDisplayPreferenceDialog()?"
                    )
                }
            }

            if (preference is EditTextPreference) {
                preference.setOnBindEditTextListener { editText ->
                    editText.addTextChangedListener(MinFilter(dialog))
                }
            }

            dialog.setTargetFragment(this, 0)

            dialogFragment = dialog

            dialog.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        }else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                init(preference.getPreference(i))
            }
        } else {
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
                if (preference.key == settingsManager.getSettingsKey(USTM))
                    summ = getString(R.string.ustm_summ)
                else if (preference.key == settingsManager.getSettingsKey(STM))
                    summ = getString(R.string.stm_summ)
                preference.summary = String.format(summ.toString(), preference.text)
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
                    context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ringtonePath =
                    notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)?.sound
                ringtonePath?.let {
                    val ringtone = RingtoneManager.getRingtone(context, it)
                    preference.summary = ringtone.getTitle(context)
                }
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