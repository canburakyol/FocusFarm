package com.focusfarm.app.session

import com.focusfarm.app.notifications.SessionProgressNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionNotificationModule {
    @Binds
    @Singleton
    abstract fun bindSessionProgressNotifier(impl: AndroidSessionProgressNotifier): SessionProgressNotifier
}
