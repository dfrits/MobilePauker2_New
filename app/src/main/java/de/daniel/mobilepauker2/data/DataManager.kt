package de.daniel.mobilepauker2.data

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.data.xml.FlashCardXMLStreamWriter
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.models.CacheFile
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(val context: @JvmSuppressWildcards Context) {
    private var fileAbsolutePath: String = ""
    var saveRequired: Boolean = false
    var currentFileName = Constants.DEFAULT_FILE_NAME
        private set

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var toaster: Toaster

    init {
        (context as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun setNewFileName(newName: String): Boolean {
        setCorrectFileEnding(newName).also {
            if (!isNameValid(it)) return false

            currentFileName = it
            return true
        }
    }

    fun getReadableCurrentFileName(): String = getReadableFileName(currentFileName)

    fun getReadableFileName(fileName: String) = if (validateFileEnding(fileName)) {
        fileName.substring(0, fileName.length - 7)
    } else if (fileName.endsWith(".pau") || fileName.endsWith(".xml")) {
        fileName.substring(0, fileName.length - 4)
    } else {
        fileName
    }

    fun getPathOfCurrentFile(): File = getFilePathForName(currentFileName)

    @Throws(IOException::class)
    fun getFilePathForName(filename: String): File {
        if (!validateFileEnding(filename)) {
            throw IOException("Filename invalid")
        }
        val filePath = "${Environment.getExternalStorageDirectory()}" +
                "${Constants.DEFAULT_APP_FILE_DIRECTORY}$filename"
        return File(filePath)
    }

    @Throws(SecurityException::class)
    fun listFiles(): Array<File> {
        val appDirectory = File(
            Environment.getExternalStorageDirectory()
                .toString() + Constants.DEFAULT_APP_FILE_DIRECTORY
        )

        if (!appDirectory.exists() && !appDirectory.mkdir()) return emptyArray()

        if (appDirectory.exists() && appDirectory.isDirectory) {
            val listFiles = appDirectory.listFiles { file ->
                isNameValid(file.name)
            }
            if (listFiles != null) return listFiles
        }

        return emptyArray()
    }

    fun isFileExisting(fileName: String): Boolean {
        val files = listFiles()
        for (file in files) {
            if (file.name == fileName) {
                return true
            }
        }
        return false
    }

    @Throws(IOException::class)
    fun loadLessonFromFile(file: File) {
        val uri = file.toURI()
        val xmlFlashCardFeedParser = FlashCardXMLPullFeedParser(uri.toURL())
        val lesson: Lesson = xmlFlashCardFeedParser.parse()
        currentFileName = file.name
        fileAbsolutePath = file.absolutePath
        lessonManager.setupLesson(lesson)
    }

    fun writeLessonToFile(isNewFile: Boolean): SaveResult {
        if (currentFileName == Constants.DEFAULT_FILE_NAME) {
            return SaveResult(false, context.getString(R.string.error_filename_invalid))
        }

        val result = try {
            FlashCardXMLStreamWriter(
                getFilePathForName(currentFileName),
                isNewFile,
                lessonManager.lesson
            ).writeLesson()
        } catch (e: IOException) {
            SaveResult(false, context.getString(R.string.error_filename_invalid))
        }

        if (result.successful) {
            saveRequired = false
        } else {
            Log.e("Save Lesson", result.errorMessage)
        }

        return result
    }

    @Deprecated("Wird durch neuen Sync ersetzt. Lektion wird lediglich gelöscht.")
    fun deleteLesson(file: File): Boolean {
        val filename = file.name
        try {
            if (file.delete()) {
                val fos = context.openFileOutput(
                    Constants.DELETED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                val text = "\n$filename;*;${System.currentTimeMillis()}"
                fos.write(text.toByteArray())
                fos.close()
            } else return false
        } catch (e: IOException) {
            return false
        }
        return try {
            val list: List<String> = getLokalAddedFiles()
            if (list.contains(filename)) {
                resetAddedFilesData()
                val fos = context.openFileOutput(
                    Constants.ADDED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                for (name in list) {
                    if (name != filename) {
                        val newText = "\n$name"
                        fos.write(newText.toByteArray())
                    }
                }
                fos.close()
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun addLesson(file: File) {
        var filename = file.name
        val index =
            if (filename.endsWith(".xml")) filename.indexOf(".xml")
            else filename.indexOf(".pau")
        if (index != -1) {
            filename = filename.substring(0, index)
            addLesson(filename)
        }
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun addCurrentLesson() {
        addLesson(currentFileName)
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun getLokalDeletedFiles(): Map<String, String> {
        val filesToDelete: MutableMap<String, String> = HashMap()
        try {
            val fis = context.openFileInput(Constants.DELETED_FILES_NAMES_FILE_NAME)
            val reader = BufferedReader(InputStreamReader(fis))
            var fileName = reader.readLine()
            while (fileName != null) {
                if (!fileName.trim { it <= ' ' }.isEmpty()) {
                    try {
                        val split: Array<String?> = fileName.split(";*;").toTypedArray()
                        val name = if (split[0] == null) "" else split[0]!!
                        val time = if (split[1] == null) "-1" else split[1]!!
                        filesToDelete[name] = time
                    } catch (e: Exception) {
                        filesToDelete[fileName] = "-1"
                    }
                }
                fileName = reader.readLine()
            }
        } catch (ignored: IOException) {
        }
        return filesToDelete
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun getLokalAddedFiles(): List<String> {
        val filesToAdd: MutableList<String> = ArrayList()
        try {
            val fis = context.openFileInput(Constants.ADDED_FILES_NAMES_FILE_NAME)
            val reader = BufferedReader(InputStreamReader(fis))
            var fileName = reader.readLine()
            while (fileName != null) {
                if (!fileName.trim { it <= ' ' }.isEmpty()) {
                    filesToAdd.add(fileName)
                }
                fileName = reader.readLine()
            }
        } catch (ignored: IOException) {
        }
        return filesToAdd
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun resetDeletedFilesData(): Boolean {
        return try {
            val fos = context.openFileOutput(
                Constants.DELETED_FILES_NAMES_FILE_NAME,
                Context.MODE_PRIVATE
            )
            val text = "\n"
            fos.write(text.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    fun resetAddedFilesData(): Boolean {
        return try {
            val fos =
                context.openFileOutput(Constants.ADDED_FILES_NAMES_FILE_NAME, Context.MODE_PRIVATE)
            val text = "\n"
            fos.write(text.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun isNameValid(filename: String): Boolean {
        return if (isNameEmpty(filename)) {
            false
        } else validateFileEnding(filename)
    }

    fun setCorrectFileEnding(name: String): String {
        if (name.endsWith(".pau")) return "$name.gz"

        if (name.endsWith(".pau.gz") || name.endsWith(".xml.gz")) return name

        return "$name.pau.gz"
    }

    private fun cacheFiles() {
        val currentFiles: MutableList<CacheFile> = mutableListOf()
        listFiles().forEach {
            currentFiles.add(CacheFile(it.path, it.lastModified()))
        }
        val json = Gson().toJson(currentFiles)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Constants.CACHED_FILES, json)
            .apply()
    }

    fun getCachedFiles(): List<File>? {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.CACHED_FILES, null) ?: return null

        val files = mutableListOf<File>()

        Gson().fromJson(json, Array<CacheFile>::class.java).forEach {
            val file = File(it.path)
            file.setLastModified(it.lastModified)
            files.add(file)
        }

        return files
    }

    fun cacheCursor(cursor: String) {

    }

    fun getCachedCursor(): String? {
        TODO("Not yet implemented")
    }

    @Deprecated("Wird durch neuen Sync ersetzt")
    private fun addLesson(fileName: String) {
        try {
            val fos =
                context.openFileOutput(Constants.ADDED_FILES_NAMES_FILE_NAME, Context.MODE_APPEND)
            val text = "\n$fileName"
            fos.write(text.toByteArray())
            fos.close()
        } catch (ignored: IOException) {
        }
        try {
            val map: Map<String, String> = getLokalDeletedFiles()
            if (map.keys.contains(fileName)) {
                resetDeletedFilesData()
                val fos = context.openFileOutput(
                    Constants.DELETED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                for ((key) in map) {
                    if (key != fileName) {
                        val newText = "\n$fileName;*;${System.currentTimeMillis()}"
                        fos.write(newText.toByteArray())
                    }
                }
                fos.close()
            }
        } catch (ignored: IOException) {
        }
    }

    private fun isNameEmpty(fileName: String): Boolean {
        if (fileName.isEmpty()) return true

        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName == ending) return true
        }
        return false
    }

    private fun validateFileEnding(fileName: String): Boolean {
        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName.endsWith(ending)) {
                return true
            }
        }
        return false
    }
}