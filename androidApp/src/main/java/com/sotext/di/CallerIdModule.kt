package com.sotext.di

import com.sotext.callid.CallerIdProvider
import com.sotext.callid.IpQualityScoreClient
import com.sotext.callid.NumLookupApiClient
import com.sotext.callid.NumverifyApiClient
import com.sotext.callid.RapidPhoneLookupClient
import com.sotext.callid.RealCallLookupClient
import com.sotext.callid.TwilioLookupClient
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
