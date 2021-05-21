package de.daniel.mobilepauker2.application

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import javax.inject.Singleton

@Singleton
@Component(modules = [ManagerModule::class])
interface ApplicationSingletonComponent {
    @Component.Builder
    interface SingletonBuilder {
        @BindsInstance
        fun managerModule(context: Context): SingletonBuilder

        @BindsInstance
        fun application(application: Application): SingletonBuilder

        fun build(): ApplicationSingletonComponent
    }

    fun inject(manager: DataManager)
    fun inject(manager: LessonManager)
}