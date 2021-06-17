package de.daniel.mobilepauker2.application

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import javax.inject.Singleton

@Module
class ManagerModule(val context: Context) {
    @Singleton
    @Provides
    fun provideLessonManager(): LessonManager {
        return LessonManager(context)
    }

    @Singleton
    @Provides
    fun provideDataManager(): DataManager {
        return DataManager(context)
    }
}