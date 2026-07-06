package net.rslvd.metricmind.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-free entitlement check via Play Billing. "Pro" is a one-time in-app product; entitlement
 * is derived from on-device queryPurchasesAsync. Acceptable for cosmetic/feature gates (never gate
 * anything safety-critical on a purely local check — it can be tampered with on rooted devices).
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refreshEntitlements()
            }

            override fun onBillingServiceDisconnected() { /* will reconnect on next start() */ }
        })
    }

    fun refreshEntitlements() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            _isPro.value = purchases.any { p ->
                p.products.contains(PRODUCT_PRO) &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            purchases.filter { !it.isAcknowledged }.forEach(::acknowledge)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) refreshEntitlements()
    }

    // launchPurchaseFlow(...) is wired from the paywall screen using queryProductDetailsAsync; omitted
    // here for brevity in the scaffold.

    companion object {
        const val PRODUCT_PRO = "metricmind_pro"
    }
}
