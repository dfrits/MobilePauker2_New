package de.daniel.mobilepauker2.mainmenu

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class MainMenuViewModel @Inject constructor(
    val lessonManager: LessonManager,
    val dataManager: DataManager,
    val toaster: Toaster
) : ViewModel() {

    fun createNewLesson() {
        lessonManager.setupNewLesson()
        dataManager.saveRequired = false
    }
}