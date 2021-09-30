/*
 * Email error reporter
 * Original author Androidblogger
 * http://androidblogger.blogspot.com/2009/12/how-to-improve-your-application-crash.html
 */
package de.daniel.mobilepauker2.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import de.daniel.mobilepauker2.R
import java.io.*
import java.util.*
import javax.inject.Inject

class ErrorReporter @Inject constructor(private val context: Context) :
    Thread.UncaughtExceptionHandler {

    private val CustomParameters = HashMap<String, String>()
    private var VersionName: String = ""
    private var PackageName: String = ""
    private var FilePath: String = ""
    private var PhoneModel: String = ""
    private var AndroidVersion: String = ""
    private var Board: String = ""
    private var Brand: String = ""
    private var Device: String = ""
    private var Display: String = ""
    private var FingerPrint: String = ""
    private var Host: String = ""
    private var ID: String = ""
    private var Model: String = ""
    private var Product: String = ""
    private var Tags: String = ""
    private var Time: Long = 0
    private var Type: String = ""
    private var User: String = ""

    private val availableInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return availableBlocks * blockSize
        }

    private val totalInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        }

    val isThereAnyErrorsToReport: Boolean
        get() {
            FilePath = context.filesDir.absolutePath
            return bIsThereAnyErrorFile()
        }

    override fun uncaughtException(t: Thread?, e: Throwable) {
        Log.d("ErrorReporter::uncaughtException", "Building error report")
        val report = StringBuilder()
        val curDate = Date()
        report.append("Error Report collected on : ").append(curDate.toString())
        report.append("\n")
        report.append("\n")
        report.append("Informations :")
        report.append("\n")
        report.append("==============")
        report.append("\n")
        report.append("\n")
        report.append(createInformationString())
        report.append("Custom Informations :\n")
        report.append("=====================\n")
        report.append(CreateCustomInfoString())
        report.append("\n\n")
        report.append("Stack : \n")
        report.append("======= \n")
        val result: Writer = StringWriter()
        val printWriter = PrintWriter(result)
        e.printStackTrace(printWriter)
        val stacktrace = result.toString()
        report.append(stacktrace)
        report.append("\n")
        report.append("Cause : \n")
        report.append("======= \n")

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        var cause = e.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            report.append(result.toString())
            cause = cause.cause
        }
        printWriter.close()
        report.append("****  End of current Report ***")
        saveAsFile(report.toString())
    }

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun addCustomData(Key: String, Value: String) {
        CustomParameters[Key] = Value
    }

    fun deleteErrorFiles() {
        try {
            FilePath = context.filesDir.absolutePath
            if (bIsThereAnyErrorFile()) {
                val fos = context.openFileOutput("error.stacktrace", Context.MODE_PRIVATE)
                val text = "\n"
                fos.write(text.toByteArray())
                fos.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    fun checkErrorAndSendMail() {
        try {
            FilePath = context.filesDir.absolutePath
            if (bIsThereAnyErrorFile()) {
                val wholeErrorText = StringBuilder()
                wholeErrorText.append("New Trace collected :\n")
                wholeErrorText.append("=====================\n ")
                val input = BufferedReader(
                    InputStreamReader(
                        context.openFileInput("error.stacktrace")
                    )
                )
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    wholeErrorText.append(line).append("\n")
                }
                input.close()

                // DELETE FILES !!!!
                deleteErrorFiles()
                sendErrorMail(wholeErrorText.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun CreateCustomInfoString(): String {
        val CustomInfo = StringBuilder()
        for (CurrentKey in CustomParameters.keys) {
            val CurrentVal = CustomParameters[CurrentKey]
            CustomInfo.append(CurrentKey).append(" = ").append(CurrentVal).append("\n")
        }
        return CustomInfo.toString()
    }

    private fun recoltInformations() {
        try {
            val pm = context.packageManager
            val pi: PackageInfo
            // Version
            if (pm != null) {
                pi = pm.getPackageInfo(context.packageName, 0)
                VersionName = pi.versionName
                // Package name
                PackageName = pi.packageName
                // Device model
                PhoneModel = Build.MODEL
                // Android version
                AndroidVersion = Build.VERSION.RELEASE
                Board = Build.BOARD
                Brand = Build.BRAND
                Device = Build.DEVICE
                Display = Build.DISPLAY
                FingerPrint = Build.FINGERPRINT
                Host = Build.HOST
                ID = Build.ID
                Model = Build.MODEL
                Product = Build.PRODUCT
                Tags = Build.TAGS
                Time = Build.TIME
                Type = Build.TYPE
                User = Build.USER
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun createInformationString(): String {
        recoltInformations()
        var returnVal = ""
        returnVal += "Version : $VersionName"
        returnVal += "\n"
        returnVal += "Package : $PackageName"
        returnVal += "\n"
        returnVal += "FilePath : $FilePath"
        returnVal += "\n"
        returnVal += "Phone Model$PhoneModel"
        returnVal += "\n"
        returnVal += "Android Version : $AndroidVersion"
        returnVal += "\n"
        returnVal += "Board : $Board"
        returnVal += "\n"
        returnVal += "Brand : $Brand"
        returnVal += "\n"
        returnVal += "Device : $Device"
        returnVal += "\n"
        returnVal += "Display : $Display"
        returnVal += "\n"
        returnVal += "Finger Print : $FingerPrint"
        returnVal += "\n"
        returnVal += "Host : $Host"
        returnVal += "\n"
        returnVal += "ID : $ID"
        returnVal += "\n"
        returnVal += "Model : $Model"
        returnVal += "\n"
        returnVal += "Product : $Product"
        returnVal += "\n"
        returnVal += "Tags : $Tags"
        returnVal += "\n"
        returnVal += "Time : $Time"
        returnVal += "\n"
        returnVal += "Type : $Type"
        returnVal += "\n"
        returnVal += "User : $User"
        returnVal += "\n"
        returnVal += "Total Internal memory : $totalInternalMemorySize"
        returnVal += "\n"
        returnVal += "Available Internal memory : $availableInternalMemorySize"
        returnVal += "\n"
        return returnVal
    }

    private fun sendErrorMail(ErrorContent: String) {
        val body = """
            ${context!!.resources.getString(R.string.crash_report_mail_body)}
            
            $ErrorContent
            
            
            """

        val emailIntent = Intent(Intent.ACTION_SEND)

        /* Fill it with Data */emailIntent.type = "plain/text"
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("fritsch_daniel@gmx.de"))
        emailIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            context!!.getString(R.string.crash_report_mail_subject)
        )

        context.startActivity(
            Intent.createChooser(
                emailIntent,
                "Send mail..."
            )
        )
    }

    private fun saveAsFile(ErrorContent: String) {
        try {
            val fileName = "error.stacktrace"
            val trace = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            trace.write(ErrorContent.toByteArray())
            trace.close()
        } catch (e: Exception) {
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun bIsThereAnyErrorFile(): Boolean {
        var bis: BufferedReader? = null
        return try {
            val inputStream = context.openFileInput("error.stacktrace")
            bis = BufferedReader(InputStreamReader(inputStream))
            bis.readLine() != ""
        } catch (e: FileNotFoundException) {
            false
        } catch (e: IOException) {
            false
        } finally {
            try {
                bis?.close()
            } catch (ignored: IOException) {
            }
        }
    }
}