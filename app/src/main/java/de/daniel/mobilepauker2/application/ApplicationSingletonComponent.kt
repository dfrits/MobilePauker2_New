package de.daniel.mobilepauker2.application

import dagger.Component
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import javax.inject.Singleton

@Singleton
@Component(modules = [ProviderModule::class])
interface ApplicationSingletonComponent {

    fun inject(manager: DataManager)
    fun inject(manager: LessonManager)
}