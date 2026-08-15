package com.sotext.di

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.sotext.BuildConfig
import com.sotext.data.alert.AlertDispatcher
import com.sotext.data.alert.NotificationRegistrar
import com.sotext.data.alert.SoundCatalog
import com.sotext.data.beta.BetaAgreementRepositoryImpl
import com.sotext.data.contacts.DeviceContactsRepository
import com.sotext.data.emergency.EmergencyLocationRepository
import com.sotext.data.db.AlertEventDao
import com.sotext.data.db.AlertRepositoryImpl
import com.sotext.data.db.BlockedContactDao
import com.sotext.data.db.ArchivedThreadDao
import com.sotext.data.db.PinnedThreadDao
import com.sotext.data.db.BlockedContactRepositoryImpl
import com.sotext.data.db.ContactDao
import com.sotext.data.db.ContactRepositoryImpl
import com.sotext.data.db.ContactMessageDao
import com.sotext.data.db.MessageRepositoryImpl
import com.sotext.data.db.PulseLinkDatabase
import com.sotext.data.settings.SettingsRepositoryImpl
import com.sotext.data.settings.provideSettingsDataStore
import com.sotext.data.sms.SmsStore
import com.sotext.data.sms.SmsRepository
import com.sotext.domain.repository.AlertRepository
import com.sotext.domain.repository.BetaAgreementRepository
import com.sotext.domain.repository.BlockedContactRepository
import com.sotext.domain.repository.ContactRepository
import com.sotext.domain.repository.MessageRepository
import com.sotext.domain.repository.SettingsRepository
import com.sotext.shared.alert.AlertRelay
import com.sotext.shared.alert.AlertRelayClient
import com.sotext.util.AudioOverrideManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindBlockedContactRepository(impl: BlockedContactRepositoryImpl): BlockedContactRepository

    @Binds
    @Singleton
    abstract fun bindBetaAgreementRepository(impl: BetaAgreementRepositoryImpl): BetaAgreementRepository

    @Binds
    @Singleton
    abstract fun bindAiAssistantRepository(
        impl: com.sotext.data.ai.AiAssistantRepositoryImpl
    ): com.sotext.data.ai.AiAssistantRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): android.hardware.SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulseLinkDatabase =
        Room.databaseBuilder(context, PulseLinkDatabase::class.java, "pulselink.db")
            .addMigrations(*PulseLinkDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideContactDao(database: PulseLinkDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideAlertDao(database: PulseLinkDatabase): AlertEventDao = database.alertEventDao()

    @Provides
    fun provideContactMessageDao(database: PulseLinkDatabase): ContactMessageDao = database.contactMessageDao()

    @Provides
    fun provideBlockedContactDao(database: PulseLinkDatabase): BlockedContactDao = database.blockedContactDao()

    @Provides
    fun provideArchivedThreadDao(database: PulseLinkDatabase): ArchivedThreadDao = database.archivedThreadDao()

    @Provides
    fun providePinnedThreadDao(database: PulseLinkDatabase): PinnedThreadDao = database.pinnedThreadDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        provideSettingsDataStore(context)

    @Provides
    @Singleton
    fun provideNotificationRegistrar(@ApplicationContext context: Context): NotificationRegistrar =
        NotificationRegistrar(context)

    @Provides
    @Singleton
    fun provideSoundCatalog(@ApplicationContext context: Context): SoundCatalog =
        SoundCatalog(context)

    @Provides
    @Singleton
    fun provideSmsStore(
        @ApplicationContext context: Context,
        smsSyncTrigger: com.sotext.data.sms.SmsSyncTrigger
    ): SmsStore = SmsStore(context, smsSyncTrigger)

    @Provides
    @Singleton
    fun provideDeviceContactsRepository(@ApplicationContext context: Context): DeviceContactsRepository =
        DeviceContactsRepository(context)

    @Provides
    @Singleton
    fun provideSmsManager(@ApplicationContext context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
    }

    @Provides
    @Singleton
    fun provideSmsRepository(
        @ApplicationContext context: Context,
        archivedThreadDao: ArchivedThreadDao,
        pinnedThreadDao: PinnedThreadDao,
        contactDao: ContactDao
    ): SmsRepository = SmsRepository(context, archivedThreadDao, pinnedThreadDao, contactDao)

    @Provides
    @Singleton
    fun provideAlertDispatcher(
        @ApplicationContext context: Context,
        smsSender: com.sotext.data.sms.SmsSender,
        locationProvider: com.sotext.data.location.LocationProvider,
        registrar: NotificationRegistrar,
        soundCatalog: SoundCatalog,
        audioOverrideManager: AudioOverrideManager,
        emergencyLocationRepository: EmergencyLocationRepository
    ): AlertDispatcher = AlertDispatcher(
        context = context,
        smsSender = smsSender,
        locationProvider = locationProvider,
        registrar = registrar,
        soundCatalog = soundCatalog,
        audioOverrideManager = audioOverrideManager,
        emergencyLocationRepository = emergencyLocationRepository
    )

    @Provides
    @Singleton
    fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideAlertRelayClient(): AlertRelayClient =
        AlertRelay.create(BuildConfig.ALERT_RELAY_BASE_URL)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .callTimeout(Duration.ofSeconds(5))
            .build()

    @Provides
    @Singleton
    @Named("NumlookupApiKey")
    fun provideNumlookupApiKey(): String = BuildConfig.NUMLOOKUP_API_KEY

    @Provides
    @Singleton
    @Named("NumverifyApiKey")
    fun provideNumverifyApiKey(): String = BuildConfig.NUMVERIFY_API_KEY

    @Provides
    @Singleton
    @Named("IpQualityScoreApiKey")
    fun provideIpQualityScoreApiKey(): String = BuildConfig.IPQUALITYSCORE_API_KEY

    @Provides
    @Singleton
    @Named("RapidLookupApiKeyDefault")
    fun provideRapidLookupApiKeyDefault(): String = BuildConfig.RAPID_LOOKUP_API_KEY

    @Provides
    @Singleton
    @Named("RapidLookupApiHost")
    fun provideRapidLookupApiHost(): String = BuildConfig.RAPID_LOOKUP_API_HOST

    @Provides
    @Singleton
    @Named("TwilioAccountSid")
    fun provideTwilioAccountSid(): String = BuildConfig.TWILIO_ACCOUNT_SID

    @Provides
    @Singleton
    @Named("TwilioAuthToken")
    fun provideTwilioAuthToken(): String = BuildConfig.TWILIO_AUTH_TOKEN

    @Provides
    @Singleton
    @Named("TruecallerApiKeyDefault")
    fun provideTruecallerApiKeyDefault(): String = BuildConfig.TRUECALLER_API_KEY

    @Provides
    @Singleton
    @Named("TruecallerApiHost")
    fun provideTruecallerApiHost(): String = BuildConfig.TRUECALLER_API_HOST
}
