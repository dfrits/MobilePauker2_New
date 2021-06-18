package de.daniel.mobilepauker2.utils

import android.app.Activity
import android.content.Context
import android.text.format.DateFormat
import android.util.SparseLongArray
import android.widget.Toast
import dagger.Lazy
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.models.NextExpireDateResult
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.ENABLE_EXPIRE_TOAST
import java.io.File
import java.net.MalformedURLException
import java.util.*
import javax.inject.Inject

class Toaster @Inject constructor(val context: Context) {
    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var dataManager: Lazy<DataManager>

    init {
        (context as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun showToast(text: String, duration: Int) {
        (context as Activity).runOnUiThread {
            if (text.isNotEmpty()) {
                Toast.makeText(context, text, duration).show()
            }
        }
    }

    fun showToast(textResource: Int, duration: Int) {
        showToast(context.getString(textResource), duration)
    }

    fun showExpireToast(context: Context) {
        if (!settingsManager.getBoolPreference(context, ENABLE_EXPIRE_TOAST)) return

        val filePath: File = dataManager.get().getPathOfCurrentFile()
        val uri = filePath.toURI()
        val parser: FlashCardXMLPullFeedParser

        try {
            parser = FlashCardXMLPullFeedParser(uri.toURL())
            val result: NextExpireDateResult = parser.getNextExpireDate()
            if (result.timeStamp > Long.MIN_VALUE) {
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = result.timeStamp
                val date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                var text = context.getString(R.string.next_expire_date)
                text = "$text $date"
                showToast(text, Toast.LENGTH_LONG * 2)
            }
        } catch (ignored: MalformedURLException) {
        }
    }
}