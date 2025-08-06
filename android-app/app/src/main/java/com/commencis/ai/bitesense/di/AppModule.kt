package com.commencis.ai.bitesense.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.commencis.ai.bitesense.ai.MediaPipeModelManager
import com.commencis.ai.bitesense.ai.LocalBiteAnalyzer
import com.commencis.ai.bitesense.ai.NetworkBiteAnalyzer
import com.commencis.ai.bitesense.data.BiteHistoryRepository
import com.commencis.ai.bitesense.data.ModelDownloadRepository
import com.commencis.ai.bitesense.network.BiteSenseApiService
import com.commencis.ai.bitesense.util.CameraHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    private const val USER_PREFERENCES_NAME = "bite_history"
    
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext appContext: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
        )
    }
    
    @Provides
    @Singleton
    fun provideBiteHistoryRepository(
        dataStore: DataStore<Preferences>
    ): BiteHistoryRepository {
        return BiteHistoryRepository(dataStore)
    }
    
    @Provides
    @Singleton
    fun provideCameraHandler(
        @ApplicationContext context: Context
    ): CameraHandler {
        return CameraHandler(context)
    }
    
    @Provides
    @Singleton
    fun provideModelDownloadRepository(
        @ApplicationContext context: Context
    ): ModelDownloadRepository {
        return ModelDownloadRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideMediaPipeModelManager(
        @ApplicationContext context: Context,
        modelDownloadRepository: ModelDownloadRepository
    ): MediaPipeModelManager {
        return MediaPipeModelManager(context, modelDownloadRepository)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBiteSenseApi(okHttpClient: OkHttpClient): BiteSenseApiService {
        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiteSenseApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideLocalBiteAnalyzer(
        modelManager: MediaPipeModelManager
    ): LocalBiteAnalyzer {
        return LocalBiteAnalyzer(modelManager)
    }
    
    @Provides
    @Singleton
    fun provideNetworkBiteAnalyzer(
        api: BiteSenseApiService,
        modelManager: MediaPipeModelManager,
        @ApplicationContext context: Context
    ): NetworkBiteAnalyzer {
        return NetworkBiteAnalyzer(api, modelManager, context)
    }
}