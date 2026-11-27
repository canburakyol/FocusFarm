package com.focusfarm.app.review

import com.focusfarm.app.review.InAppReviewLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewModule {

    @Binds
    @Singleton
    abstract fun bindInAppReviewLauncher(impl: PlayInAppReviewLauncher): InAppReviewLauncher
}
