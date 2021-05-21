package de.daniel.mobilepauker2.application

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class ApplicationModule() {
    @Singleton
    @Binds
    abstract fun provideContext(application: Application): Context
}