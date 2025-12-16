package com.pulselink.di

import com.pulselink.callid.CallerIdProvider
import com.pulselink.callid.IpQualityScoreClient
import com.pulselink.callid.NumLookupApiClient
import com.pulselink.callid.NumverifyApiClient
import com.pulselink.callid.RapidPhoneLookupClient
import com.pulselink.callid.RealCallLookupClient
import com.pulselink.callid.TwilioLookupClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CallerIdModule {

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindTwilioLookupProvider(impl: TwilioLookupClient): CallerIdProvider

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindRapidLookupProvider(impl: RapidPhoneLookupClient): CallerIdProvider

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindRealCallProvider(impl: RealCallLookupClient): CallerIdProvider

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindNumLookupProvider(impl: NumLookupApiClient): CallerIdProvider

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindNumverifyProvider(impl: NumverifyApiClient): CallerIdProvider

    @Binds
    @Singleton
    @IntoSet
    abstract fun bindIpQualityScoreProvider(impl: IpQualityScoreClient): CallerIdProvider
}
