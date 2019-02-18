package com.izettle.wrench.provider

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

@Module
abstract class WrenchProviderModule {
    @ContributesAndroidInjector
    internal abstract fun contributesWrenchProvider(): WrenchProvider

    companion object {
        @Provides
        fun providePackageManagerWrapper(application: Application): IPackageManagerWrapper = PackageManagerWrapper(application.packageManager)
    }
}