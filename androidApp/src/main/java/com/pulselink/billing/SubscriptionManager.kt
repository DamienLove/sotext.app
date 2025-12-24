package com.pulselink.billing

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pulselink.BuildConfig
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubscriptionState(
    val available: Boolean = BuildConfig.SUBSCRIPTION_ENABLED,
    val loading: Boolean = false,
    val product: ProductDetails? = null,
    val isPremiumActive: Boolean = false,
    val statusMessage: String? = null
)

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _state

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    init {
        if (BuildConfig.SUBSCRIPTION_ENABLED) {
            startConnection()
        }
    }

    private fun startConnection() {
        if (!isPlayServicesReady()) {
            _state.value = _state.value.copy(
                available = false,
                loading = false,
                statusMessage = "Google Play Services unavailable; billing disabled"
            )
            return
        }
        if (billingClient.isReady) {
            queryProductsAndPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val featureResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                    if (featureResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        _state.value = _state.value.copy(
                            available = false,
                            loading = false,
                            statusMessage = "Subscriptions not supported on this device"
                        )
                        return
                    }
                    queryProductsAndPurchases()
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        statusMessage = "Billing unavailable (${result.responseCode})"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(
                    loading = false,
                    statusMessage = "Billing disconnected"
                )
            }
        })
        _state.value = _state.value.copy(loading = true, statusMessage = null)
    }

    private fun isPlayServicesReady(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(context)
        if (status == ConnectionResult.SUCCESS) return true
        // Errors here are non-fatal for the app, but billing cannot proceed without Play Services/Store.
        return false
    }

    private fun queryProductsAndPurchases() {
        val productParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.SUBS_MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(productParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    product = details.productDetailsList.firstOrNull(),
                    loading = false
                )
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    statusMessage = "Unable to load subscription products"
                )
            }
        }

        val purchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(purchaseParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    fun launchSubscribe(activity: Activity) {
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(statusMessage = "Billing not ready; retrying connection")
            startConnection()
            return
        }
        val product = _state.value.product ?: run {
            _state.value = _state.value.copy(statusMessage = "Subscription not available")
            return
        }
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _state.value = _state.value.copy(statusMessage = "No active base plan/offer configured")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                statusMessage = "Purchase failed to start: ${result.responseCode}"
            )
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.value = _state.value.copy(statusMessage = "Purchase failed: ${result.responseCode}")
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.any { purchase ->
            purchase.products.contains(BuildConfig.SUBS_MONTHLY_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        _state.value = _state.value.copy(isPremiumActive = active, loading = false)
        scope.launch {
            settingsRepository.setPremiumUnlocked(active)
        }
        purchases.filter { it.isAcknowledged.not() }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* no-op */ }
        }
    }
}
