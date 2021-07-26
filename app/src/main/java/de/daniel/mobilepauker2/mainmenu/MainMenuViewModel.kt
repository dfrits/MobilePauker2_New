package de.daniel.mobilepauker2.mainmenu

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class MainMenuViewModel @Inject constructor(
    val lessonManager: LessonManager,
    val dataManager: DataManager
) : ViewModel() {

    fun createNewLesson() {
        lessonManager.setupNewLesson()
        dataManager.saveRequired = false
    }

    fun checkLessonIsSetup() {
        if (!lessonManager.isLessonSetup()) lessonManager.createNewLesson()
    }

    fun getBatchSize(batchType: BatchType) = lessonManager.getBatchSize(batchType)

    fun resetLesson() {
        lessonManager.resetLesson()
    }

    fun getDescription(): String = lessonManager.lessonDescription
}