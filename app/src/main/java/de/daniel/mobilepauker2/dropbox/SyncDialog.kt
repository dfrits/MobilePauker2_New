package de.daniel.mobilepauker2.dropbox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.ACCESS_TOKEN
import de.daniel.mobilepauker2.utils.Constants.FILES
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.File
import java.io.Serializable
import java.util.*
import javax.inject.Inject

class SyncDialog : AppCompatActivity(R.layout.progress_dialog) {
    private val context: Context = this
    private val files: Array<File>? = null
    private val timeout: Timer? = null
    private val timerTask: TimerTask? = null

    @Inject
    lateinit var toaster: Toaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        val intent = intent
        val accessToken = intent.getStringExtra(ACCESS_TOKEN)
        if (accessToken == null) {
            Log.d("SyncDialog::OnCreate", "Synchro mit accessToken = null gestartet")
            finishDialog(RESULT_CANCELED)
            return
        }
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                toaster.showToast(
                    context as Activity,
                    "Internetverbindung prüfen!", //TODO Strings
                    Toast.LENGTH_LONG
                )
                finishDialog(RESULT_CANCELED)
            }

            override fun onAvailable(network: Network) {
                DropboxClientFactory.init(accessToken)
                val serializableExtra = intent.getSerializableExtra(FILES)
                startSync(intent, serializableExtra!!)
            }
        })

        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.synchronizing)
    }

    private fun startSync(intent: Intent, serializableExtra: Serializable) {
        val action = intent.action
        if (Constants.SYNC_ALL_ACTION == action && serializableExtra is Array<*>) {
            syncAllFiles(serializableExtra as Array<File>)
        } else if (serializableExtra is File) {
            syncFile(serializableExtra, action)
        } else {
            Log.d("SyncDialog::OnCreate", "Synchro mit falschem Extra gestartet")
            finishDialog(RESULT_CANCELED)
        }
    }

    private fun finishDialog(result: Int) {
        setResult(result)
        finish()
    }

    private fun syncAllFiles(serializableExtra: Array<File>) {

    }

    private fun syncFile(serializableExtra: File, action: String?) {

    }

    private fun showProgressbar() {
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.synchronizing)
    }
}