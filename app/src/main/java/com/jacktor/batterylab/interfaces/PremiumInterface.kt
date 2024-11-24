package com.jacktor.batterylab.interfaces

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.View
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.queryPurchaseHistory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jacktor.batterylab.MainActivity
import com.jacktor.batterylab.PremiumActivity
import com.jacktor.batterylab.R
import com.jacktor.batterylab.fragments.HistoryFragment
import com.jacktor.batterylab.helpers.HistoryHelper
import com.jacktor.batterylab.helpers.ServiceHelper
import com.jacktor.batterylab.services.BatteryLabService
import com.jacktor.batterylab.services.CheckPremiumJob
import com.jacktor.batterylab.services.OverlayService
import com.jacktor.batterylab.utilities.Constants
import com.jacktor.batterylab.utilities.PreferencesKeys.BYPASS_DND
import com.jacktor.batterylab.utilities.PreferencesKeys.CAPACITY_IN_WH
import com.jacktor.batterylab.utilities.PreferencesKeys.CHARGING_DISCHARGE_CURRENT_IN_WATT
import com.jacktor.batterylab.utilities.PreferencesKeys.ENABLED_OVERLAY
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_CHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_DISCHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_BATTERY_IS_FULLY_CHARGED
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_FULL_CHARGE_REMINDER
import com.jacktor.batterylab.utilities.PreferencesKeys.NOTIFY_OVERHEAT_OVERCOOL
import com.jacktor.batterylab.utilities.PreferencesKeys.RESET_SCREEN_TIME_AT_ANY_CHARGE_LEVEL
import com.jacktor.batterylab.utilities.PreferencesKeys.SHOW_BATTERY_INFORMATION
import com.jacktor.batterylab.utilities.PreferencesKeys.SHOW_STOP_SERVICE
import com.jacktor.batterylab.utilities.PreferencesKeys.STOP_THE_SERVICE_WHEN_THE_CD
import com.jacktor.batterylab.utilities.PreferencesKeys.TAB_ON_APPLICATION_LAUNCH
import com.jacktor.batterylab.utilities.PreferencesKeys.TEXT_FONT
import com.jacktor.batterylab.utilities.Prefs
import com.jacktor.batterylab.utilities.Premium.PREMIUM_ID
import com.jacktor.batterylab.utilities.Premium.TOKEN_COUNT
import com.jacktor.batterylab.utilities.Premium.TOKEN_PREF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds


@SuppressLint("StaticFieldLeak")
interface PremiumInterface : PurchasesUpdatedListener {

    companion object {

        var mProductDetailsList: MutableList<ProductDetails>? = null
        var premiumContext: Context? = null
        var premiumActivity: Activity? = null
        var billingClient: BillingClient? = null

        var isPremium = false


    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {

        if (premiumContext == null) premiumContext = BatteryLabService.instance

        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.Default).launch {
                    handlePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            val pref = Prefs(premiumContext!!)
            if (purchases != null) pref.setString(
                TOKEN_PREF,
                purchases[0].purchaseToken
            )
            val tokenPref = pref.getString(TOKEN_PREF, null)
            isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT
            if (isPremium) premiumFeaturesUnlocked(premiumContext!!)
            ServiceHelper.checkPremiumJobSchedule(premiumContext!!)
        }
    }

    fun initiateBilling(isPurchasePremium: Boolean) {

        if (premiumContext == null) premiumContext = BatteryLabService.instance

        @Suppress("DEPRECATION")
        billingClient = BillingClient.newBuilder(premiumContext!!)
            .setListener(purchasesUpdatedListener()).enablePendingPurchases().build()

        if (billingClient?.connectionState == BillingClient.ConnectionState.DISCONNECTED)
            startConnection(isPurchasePremium)
    }

    fun startConnection(isPurchasePremium: Boolean) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    querySkuDetails()
                    if (isPurchasePremium) launchPurchaseFlow(mProductDetailsList!![0])

                    PremiumActivity.instance?.showProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                //startConnection(false)
            }
        })
    }

    private fun purchasesUpdatedListener() = PurchasesUpdatedListener { _, purchases ->
        if (purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.Default).launch {
                    handlePurchase(purchase)
                }
            }
        }
    }

    suspend fun handlePurchase(purchase: Purchase) {

        if (premiumContext == null) premiumContext = BatteryLabService.instance

        val pref = Prefs(premiumContext!!)

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                withContext(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                        if (it.responseCode == BillingResponseCode.OK) {
                            pref.setString(TOKEN_PREF, purchase.purchaseToken)
                            val tokenPref = pref.getString(TOKEN_PREF, null)
                            isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT

                            if (isPremium) premiumFeaturesUnlocked(premiumContext!!)
                        }
                    }
                }
            } else {
                pref.setString(TOKEN_PREF, purchase.purchaseToken)
                val tokenPref = pref.getString(TOKEN_PREF, null)
                isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT

                PremiumActivity.instance?.reloadScreen()
                //MainActivity.instance?.topAppBar?.menu?.findItem(R.id.premium)?.isVisible = false
                MainActivity.instance?.topAppBar?.menu?.findItem(R.id.history_premium)?.isVisible =
                    false
                MainActivity.instance?.topAppBar?.menu?.findItem(R.id.clear_history)?.isVisible =
                    true
                ServiceHelper.checkPremiumJobSchedule(premiumContext!!)
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            pref.setString(TOKEN_PREF, purchase.purchaseToken)
            val tokenPref = pref.getString(TOKEN_PREF, null)
            isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT

            if (isPremium) premiumFeaturesUnlocked(premiumContext!!)

            ServiceHelper.checkPremiumJobSchedule(premiumContext!!)
        }
    }

    private fun querySkuDetails() {
        val productList = mutableListOf(QueryProductDetailsParams.Product.newBuilder().apply {
            setProductId(PREMIUM_ID)
            setProductType(ProductType.INAPP)
        }.build())

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList).build()

        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { billingResult,
                                                                             productDetailsList ->

            if (billingResult.responseCode == BillingResponseCode.OK)
                mProductDetailsList = productDetailsList
        }
    }

    private fun showNotInstalledGooglePlayDialog(context: Context) {
        MaterialAlertDialogBuilder(context).apply {
            setIcon(R.drawable.ic_instruction_not_supported_24dp)
            setTitle(R.string.error)
            setMessage(R.string.not_installed_google_play_dialog)
            setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
            show()
        }
    }

    fun launchPurchaseFlow(productDetails: ProductDetails?) {

        if (!mProductDetailsList.isNullOrEmpty()) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams
                    .newBuilder().setProductDetails(productDetails!!).build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient?.launchBillingFlow(premiumActivity!!, billingFlowParams)
        }
    }


    fun checkPremium() {

        if (premiumContext == null) premiumContext = BatteryLabService.instance

        CoroutineScope(Dispatchers.IO).launch {

            val pref = Prefs(premiumContext!!)

            var tokenPref = pref.getString(TOKEN_PREF, null)

            if (tokenPref != null && tokenPref.count() == TOKEN_COUNT) isPremium = true
            else if (tokenPref != null && tokenPref.count() != TOKEN_COUNT)
                pref.remove(TOKEN_PREF)
            else if (tokenPref == null || tokenPref.count() != TOKEN_COUNT) {

                if (billingClient?.isReady != true) initiateBilling(false)

                delay(2.5.seconds)
                if (billingClient?.isReady == true) {
                    val params = QueryPurchaseHistoryParams.newBuilder()
                        .setProductType(ProductType.INAPP)

                    val purchaseHistoryResult = billingClient?.queryPurchaseHistory(params.build())

                    val purchaseHistoryRecordList = purchaseHistoryResult?.purchaseHistoryRecordList

                    if (!purchaseHistoryRecordList.isNullOrEmpty()) {

                        pref.setString(TOKEN_PREF, purchaseHistoryRecordList[0].purchaseToken)

                        tokenPref = pref.getString(TOKEN_PREF, null)

                        isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT

                        if (!isPremium) removePremiumFeatures(premiumContext!!)

                        delay(5.seconds)
                        billingClient?.endConnection()
                        billingClient = null
                    }
                    if (!isPremium) removePremiumFeatures(premiumContext!!)
                }
            }
        }
    }

    fun CheckPremiumJob.checkPremiumJob() {

        CoroutineScope(Dispatchers.IO).launch {

            val pref = Prefs(this@checkPremiumJob)

            if (billingClient?.isReady != true) initiateBilling(false)

            delay(2.5.seconds)
            if (billingClient?.isReady == true) {
                val params = QueryPurchaseHistoryParams.newBuilder()
                    .setProductType(ProductType.INAPP)

                val purchaseHistoryResult = billingClient?.queryPurchaseHistory(params.build())

                val purchaseHistoryRecordList = purchaseHistoryResult?.purchaseHistoryRecordList

                if (!purchaseHistoryRecordList.isNullOrEmpty()) {
                    pref.setString(TOKEN_PREF, purchaseHistoryRecordList[0].purchaseToken)
                    val tokenPref = pref.getString(TOKEN_PREF, null)
                    isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT
                    delay(5.seconds)
                    billingClient?.endConnection()
                    billingClient = null
                } else {
                    if (pref.contains(TOKEN_PREF)) pref.remove(TOKEN_PREF)
                    val tokenPref = pref.getString(TOKEN_PREF, null)
                    isPremium = tokenPref != null && tokenPref.count() == TOKEN_COUNT
                }

                if (!isPremium) removePremiumFeatures(this@checkPremiumJob)
            }
        }
    }

    private fun premiumFeaturesUnlocked(context: Context) {
        PremiumActivity.instance?.reloadScreen()

        val mainActivity = MainActivity.instance
        val historyFragment = HistoryFragment.instance
        val isHistoryNotEmpty = HistoryHelper.isHistoryNotEmpty(context)

        mainActivity?.topAppBar?.menu?.apply {
            findItem(R.id.premium)?.isVisible = false
            findItem(R.id.history_premium)?.isVisible = false
            findItem(R.id.clear_history)?.isVisible = isHistoryNotEmpty
        }

        historyFragment?.binding?.apply {
           refreshEmptyHistory.visibility =
                if (isHistoryNotEmpty) View.GONE else View.VISIBLE
            emptyHistoryLayout.visibility =
                if (isHistoryNotEmpty) View.GONE else View.VISIBLE
            historyRecyclerView.visibility =
                if (!isHistoryNotEmpty) View.GONE else View.VISIBLE
           refreshHistory.visibility =
                if (!isHistoryNotEmpty) View.GONE else View.VISIBLE
           emptyHistoryText.text =
                if (!isHistoryNotEmpty) context.resources?.getText(R.string.empty_history_text) else null
        }
    }

    private suspend fun removePremiumFeatures(context: Context) {

        val pref = Prefs(context)

        arrayListOf(
            SHOW_STOP_SERVICE,
            STOP_THE_SERVICE_WHEN_THE_CD,
            SHOW_BATTERY_INFORMATION,
            BYPASS_DND,
            NOTIFY_OVERHEAT_OVERCOOL,
            NOTIFY_BATTERY_IS_FULLY_CHARGED,
            NOTIFY_FULL_CHARGE_REMINDER,
            NOTIFY_BATTERY_IS_FULLY_CHARGED,
            NOTIFY_BATTERY_IS_CHARGED,
            NOTIFY_BATTERY_IS_DISCHARGED,
            TEXT_FONT,
            CAPACITY_IN_WH,
            CHARGING_DISCHARGE_CURRENT_IN_WATT,
            RESET_SCREEN_TIME_AT_ANY_CHARGE_LEVEL,
            TAB_ON_APPLICATION_LAUNCH,
            ENABLED_OVERLAY
        ).forEach {

            with(pref) {
                apply {
                    if (contains(it)) remove(it)
                }
            }
        }

        if (HistoryHelper.isHistoryNotEmpty(context)) HistoryHelper.clearHistory(context)

        withContext(Dispatchers.Main) {

            ServiceHelper.cancelJob(
                context,
                Constants.NOTIFY_FULL_CHARGE_REMINDER_JOB_ID
            )

            if (OverlayService.instance != null)
                ServiceHelper.stopService(context, OverlayService::class.java)
        }
    }

    fun PremiumActivity.showInstallAppFromGooglePlayDialog(context: Context) {
        MaterialAlertDialogBuilder(context).apply {
            setIcon(R.drawable.ic_instruction_not_supported_24dp)
            setTitle(R.string.premium_purchase_error)
            setMessage(R.string.install_the_app_from_gp)
            setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, Uri.parse(Constants.GOOGLE_PLAY_APP_LINK)
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                }
            }
            setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            show()
        }
    }

    fun PremiumActivity.checkForInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> true
            else -> false
        }
    }
}