package de.daniel.mobilepauker2.application

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import javax.inject.Singleton

@Module
class ManagerModule {
    @Singleton
    @Provides
    fun provideLessonManager(context: Context): LessonManager {
        return LessonManager(context)
    }

    @Singleton
    @Provides
    fun provideDataManager(context: Context): DataManager {
        return DataManager(context)
    }
}