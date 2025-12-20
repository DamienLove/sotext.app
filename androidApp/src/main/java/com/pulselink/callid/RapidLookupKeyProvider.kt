package com.pulselink.callid

import com.pulselink.data.remoteconfig.RemoteConfigService
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RapidLookupKeyProvider @Inject constructor(
    private val remoteConfigService: RemoteConfigService,
    @Named("RapidLookupApiKeyDefault") private val defaultKey: String
) {
    fun currentKey(): String =
        remoteConfigService.getString("rapid_lookup_api_key").ifBlank { defaultKey }
}
