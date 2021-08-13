package de.daniel.mobilepauker2.shortcut

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.mainmenu.MainMenu
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.SHORTCUT_EXTRA
import de.daniel.mobilepauker2.utils.ErrorReporter
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.IOException
import javax.inject.Inject

class ShortcutReceiver : AppCompatActivity(R.layout.progress_dialog) {
    private val context: Context = this

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var errorReporter: ErrorReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.open_lesson_hint)
        val intent = intent
        if (Constants.SHORTCUT_ACTION == intent.action) {
            handleShortcut(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            if (resultCode == RESULT_OK) {
                Log.d("ShortcutReceiver::onActivityResult", "File wurde aktualisiert")
            } else {
                Log.d("ShortcutReceiver::onActivityResult", "File wurde nicht aktualisiert")
            }
            val filename = intent.getStringExtra(SHORTCUT_EXTRA)
            if (filename != null) {
                openLesson(filename)
            } else {
                Log.d("ShortcutReceiver::onActivityResult", "Filename is null")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun cancelClicked(view: View?) {}

    private fun handleShortcut(shortcutIntent: Intent) {
        /*if (LearnCardsActivity.isLearningRunning()) { // TODO
            toaster.showToast(
                R.string.shortcut_open_error_learning_running,
                Toast.LENGTH_SHORT
            )
            return
        }*/
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            toaster.showToast(
                context as Activity,
                R.string.shortcut_open_error_permission,
                Toast.LENGTH_SHORT
            )
            return
        }
        val filename = shortcutIntent.getStringExtra(SHORTCUT_EXTRA) ?: return
        if (settingsManager.getBoolPreference(SettingsManager.Keys.AUTO_DOWNLOAD)
        ) {
            Log.d("ShortcutReceiver::openLesson", "Check for newer version on DB")
            /*val accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.DROPBOX_ACCESS_TOKEN, null)
            val syncIntent = Intent(context, SyncDialog::class.java) // TODO
            try {
                syncIntent.putExtra(SyncDialog.FILES, paukerManager.getFilePath(context, filename))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
            syncIntent.action = SyncDialog.SYNC_FILE_ACTION
            startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN)*/
        } else {
            openLesson(filename)
        }
    }

    private fun openLesson(filename: String) {
        try {
            if (dataManager.currentFileName != filename) {
                dataManager.loadLessonFromFile(dataManager.getFilePathForName(filename))
                dataManager.saveRequired = false
            }
            val intent = Intent(context, MainMenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } catch (e: IOException) {
            toaster.showToast(
                context as Activity,
                getString(R.string.error_reading_from_xml),
                Toast.LENGTH_SHORT
            )
            errorReporter.addCustomData("ImportThread", "IOException?")
        }
    }
}