package de.daniel.mobilepauker2.application

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import javax.inject.Singleton

@Module
class ProviderModule(val application: Application) {
    @Singleton
    @Provides
    fun provideLessonManager(): LessonManager {
        return LessonManager(application)
    }

    @Singleton
    @Provides
    fun provideDataManager(): DataManager {
        return DataManager(application)
    }

    @Singleton
    @Provides
    fun provideContext(): Context {
        return application
    }

    @Singleton
    @Provides
    fun provideApplication(): Application {
        return application
    }
}