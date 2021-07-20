package de.daniel.mobilepauker2.mainmenu

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
        toaster.showToast(R.string.new_lession_created, Toast.LENGTH_SHORT)
    }
}