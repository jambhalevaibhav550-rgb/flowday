package com.example.flowday.di

import android.content.Context
import com.example.flowday.R
import com.example.flowday.sign_in.GoogleAuthClient
import com.google.android.gms.auth.api.identity.Identity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    @AuthClientQualifier
    fun provideGoogleAuthClient(
        @ApplicationContext context: Context
    ): GoogleAuthClient {
        return GoogleAuthClient(
            context = context,
            oneTapClient = Identity.getSignInClient(context)
        )
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClientQualifier
