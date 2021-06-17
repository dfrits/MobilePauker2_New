package de.daniel.mobilepauker2.application

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(val context: Context) {
    @Singleton
    @Provides
    fun provideContext(application: Application) = context
}