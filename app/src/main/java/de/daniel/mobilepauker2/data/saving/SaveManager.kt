package de.daniel.mobilepauker2.data.saving

import android.content.Context
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.utils.Constants
import javax.inject.Inject

class SaveManager @Inject constructor(var context: Context) {

    @Inject
    lateinit var dataManager: DataManager

    init {
        (context as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun saveLesson(fileName: String): Boolean {
        if (fileName == Constants.DEFAULT_FILE_NAME) {
            return false
        }

        return safeFile(fileName, false)
    }

    private fun safeFile(fileName: String, checkOverWrite: Boolean): Boolean {
        return false
    }

    private fun overwriteOK(fileName: String): Boolean {
        return false
    }
}