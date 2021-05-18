package de.daniel.mobilepauker2.data

import android.app.Activity
import android.content.Context
import android.widget.Toast
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Toaster.Companion.showToast
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(val context: Context) {
    private var fileAbsolutePath: String = ""
    private var saveRequired: Boolean = false
    private var currentFileName = Constants.DEFAULT_FILE_NAME

    @Inject
    lateinit var lessonManager: LessonManager

    fun setNewFileName(newName: String): Boolean {
        setCorrectFileEnding(newName).also {
            if (!isNameValid(it)) return false

            currentFileName = it
            return true
        }
    }

    fun getReadableFileName(): String {
        val filename: String = currentFileName

        return if (validateFileEnding(filename)) {
            filename.substring(0, filename.length - 7)
        } else if (filename.endsWith(".pau") || filename.endsWith(".xml")) {
            filename.substring(0, filename.length - 4)
        } else {
            filename
        }
    }

    @Throws(IOException::class)
    fun getFilePath(filename: String): File {
        if (!validateFileEnding(filename)) {
            showToast(
                context as Activity,
                R.string.error_filename_invalid,
                Toast.LENGTH_LONG
            )
            throw IOException("Filename invalid")
        }
        val filePath = context.getExternalFilesDir(null)
            .toString() + Constants.DEFAULT_APP_FILE_DIRECTORY + filename
        return File(filePath)
    }

    @Throws(SecurityException::class)
    fun listFiles(): Array<File> {
        val appDirectory = File(
            context.getExternalFilesDir(null).toString() + Constants.DEFAULT_APP_FILE_DIRECTORY
        )

        if (!appDirectory.exists() && !appDirectory.mkdir()) return emptyArray()

        if (appDirectory.exists() && appDirectory.isDirectory) {
            val listFiles = appDirectory.listFiles { file -> isNameValid(file.name) }
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
        lessonManager.setLesson(lesson)
    }

    private fun setCorrectFileEnding(name: String): String {
        if (name.endsWith(".pau")) return "$name.gz"

        if (name.endsWith(".pau.gz") || name.endsWith(".xml.gz")) return name

        return "$name.pau.gz"
    }

    private fun isNameValid(filename: String): Boolean {
        return if (isNameEmpty(filename)) {
            false
        } else validateFileEnding(filename)
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