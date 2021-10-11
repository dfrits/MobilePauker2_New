package de.daniel.mobilepauker2.dropbox

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.dropbox.core.DbxException
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.Metadata
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.utils.*
import de.daniel.mobilepauker2.utils.Constants.ACCESS_TOKEN
import de.daniel.mobilepauker2.utils.Constants.FILES
import java.io.File
import java.io.Serializable
import java.util.*
import javax.inject.Inject

class SyncDialog : AppCompatActivity(R.layout.progress_dialog) {
    private val context: Context = this
    private val lifecycleOwner: LifecycleOwner = this
    private var files: List<File>? = null
    private var timeout: Timer? = null
    private var timerTask: TimerTask? = null
    private var cancelButton: Button? = null
    private var accessToken: String? = null

    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
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
            viewModel.errorLiveData.observe(lifecycleOwner) { errorOccured(it) }
            startSync(intent, serializableExtra!!)
        }

        override fun onUnavailable() {
            toaster.showToast(
                context as Activity,
                "Internetverbindung prüfen!", // TODO Strings
                Toast.LENGTH_LONG
            )
            finishDialog(RESULT_CANCELED)
        }
    }

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var viewModel: SyncDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (!isInternetAvailable(cm)) {
            toaster.showToast(context as Activity, "Internetverbindung prüfen!", Toast.LENGTH_LONG)
            finishDialog(RESULT_CANCELED)
        }

        val intent = intent
        accessToken = intent.getStringExtra(ACCESS_TOKEN)
        if (accessToken == null) {
            Log.d("SyncDialog::OnCreate", "Synchro mit accessToken = null gestartet")
            finishDialog(RESULT_CANCELED)
            return
        }

        cm.registerDefaultNetworkCallback(networkCallback)

        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.synchronizing)
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann
    override fun onBackPressed() {}

    override fun onResume() {
        super.onResume()
        timeout?.let {
            if (timerTask != null) {
                it.cancel()
                startTimer()
            }
        }
    }

    override fun onDestroy() {
        timeout?.let {
            if (timerTask != null) {
                it.cancel()
            }
        }

        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)

        viewModel.cancelTasks()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Log.d("SyncDialog::TouchEvent", "Touched")
        cancelButton?.let { cancelButton ->
            val pos = IntArray(2)
            cancelButton.getLocationInWindow(pos)
            if (ev.y <= pos[1] + cancelButton.height && ev.x > pos[0]
                && ev.y > pos[1] && ev.x <= pos[0] + cancelButton.width
                && ev.action == MotionEvent.ACTION_UP
            ) {
                cancelClicked(cancelButton)
            }
        }
        return false
    }

    fun cancelClicked(view: View) {
        Log.d("SyncDialog::cancelClicked", "Cancel Sync")
        view.isEnabled = false
        toaster.showToast(
            context as Activity,
            R.string.synchro_canceled_by_user,
            Toast.LENGTH_LONG
        )
        finishDialog(RESULT_CANCELED)
    }

    private fun startSync(intent: Intent, serializableExtra: Serializable) {
        val action = intent.action
        if (Constants.SYNC_ALL_ACTION == action && serializableExtra is Array<*>) {
            syncAllFiles(convertExtraToList(serializableExtra))
        } else if (serializableExtra is File && action != null) {
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

    private fun syncAllFiles(serializableExtra: List<File>) {
        showDialog()
        files = serializableExtra
        startTimer()
        initObserver()
        files?.let { viewModel.loadDataFromDropbox(it) }
    }

    private fun syncFile(serializableExtra: File, action: String) {

    }

    private fun showDialog() {
        val dialogFrame = findViewById<RelativeLayout>(R.id.pFrame)
        dialogFrame.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.synchronizing)
    }

    private fun convertExtraToList(serializableExtra: Array<*>): List<File> {
        val list = mutableListOf<File>()
        serializableExtra.forEach {
            list.add(it as File)
        }
        return list.toList()
    }

    private fun showCancelButton() {
        cancelButton = findViewById(R.id.cancel_button)
        cancelButton?.visibility = View.VISIBLE
        Log.d("SyncDialog::showCancelButton", "Button is enabled: " + cancelButton?.isEnabled)
    }

    private fun getFileIndex(file: File, list: Collection<File>): Int =
        list.indexOfFirst { it.name == file.name }

    private fun getFileIndex(fileName: String, list: Collection<String>): Int =
        list.indexOfFirst { it == fileName }

    private fun startTimer() {
        timeout = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    toaster.showToast(
                        context as Activity,
                        R.string.synchro_timeout,
                        Toast.LENGTH_SHORT
                    )
                }
                finishDialog(RESULT_CANCELED)
            }
        }
        timeout?.schedule(timerTask, 60000)
    }

    private fun initObserver() {
        viewModel.downloadList.observe(this) { downloadFiles(it, viewModel.downloadSize) }
        viewModel.uploadList.observe(this) { uploadFiles(it) }
        viewModel.deleteLocalList.observe(this) { deleteLocalFiles(it) }
        viewModel.deleteServerList.observe(this) { deleteFilesOnServer(it) }
    }

    private fun downloadFiles(list: List<FileMetadata>, downloadSize: Long) {
        val progressBar = findViewById<ProgressBar>(R.id.pBar)
        var task: CoroutinesAsyncTask<FileMetadata, FileMetadata, List<File>>? = null

        task = viewModel.downloadFiles(list, object : DownloadFileTask.Callback {
            override fun onDownloadStartet() {
                Log.d("SyncDialog:downloadFiles", "Download startet")
                progressBar.max = downloadSize.toInt()
                progressBar.isIndeterminate = false
            }

            override fun onProgressUpdate(metadata: FileMetadata) {
                Log.d(
                    "SyncDialog:downloadFiles",
                    "Download update: " + progressBar.progress + metadata.size
                )
                progressBar.progress = (progressBar.progress + metadata.size).toInt()
            }

            override fun onDownloadComplete(result: List<File>) {
                Log.d("SyncDialog:downloadFiles", "Download complete")
                progressBar.isIndeterminate = true
                viewModel.removeTask(task!!)
            }

            override fun onError(e: Exception?) {
                Log.e(
                    "LessonImportActivity::downloadFiles",
                    "Failed to download file.", e
                )
                toaster.showToast(
                    context as Activity,
                    R.string.simple_error_message,
                    Toast.LENGTH_SHORT
                )
                errorOccured(e)
            }
        })
    }

    private fun uploadFiles(list: List<File>) {
        var task: CoroutinesAsyncTask<File?, Void?, List<Metadata>>? = null
        task = viewModel.uploadFiles(list, object : UploadFileTask.Callback{
            override fun onUploadComplete(result: List<Metadata?>?) {
                viewModel.removeTask(task!!)
            }

            override fun onError(e: Exception?) {
                errorOccured(e)
            }
        })
    }

    private fun deleteLocalFiles(list: List<File>) {

    }

    private fun deleteFilesOnServer(list: List<File>) {

    }

    private fun errorOccured(e: Exception?) { // TODO Andere Exceptions einpflegen
        toaster.showToast(
            context as Activity,
            R.string.simple_error_message,
            Toast.LENGTH_SHORT
        )
        viewModel.cancelTasks()
        if (e is DbxException && e.requestId == "401") {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Dropbox token is invalid!")
                .setMessage(
                    "There is something wrong with the dropbox token. Maybe it is " +
                            "solved by the next try." // TODO Strings
                )
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton("Send E-Mail") { _, _ ->
                    errorReporter.init()
                    errorReporter.uncaughtException(null, e)
                    errorReporter.checkErrorAndSendMail()
                }
                .setOnDismissListener {
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(Constants.DROPBOX_ACCESS_TOKEN, null).apply()
                    finishDialog(AppCompatActivity.RESULT_CANCELED)
                }
                .setCancelable(false)
            builder.create().show()
        } else {
            finishDialog(AppCompatActivity.RESULT_CANCELED)
        }
    }

    private fun isInternetAvailable(cm: ConnectivityManager): Boolean {
        val networkCapabilities = cm.activeNetwork ?: return false
        val actNw = cm.getNetworkCapabilities(networkCapabilities) ?: return false

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}