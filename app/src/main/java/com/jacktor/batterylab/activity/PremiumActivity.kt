package com.jacktor.batterylab.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.ImmutableList
import com.google.firebase.analytics.FirebaseAnalytics
import com.jacktor.batterylab.MainActivity
import com.jacktor.batterylab.MainApp
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.PremiumAdapter
import com.jacktor.batterylab.databinding.ActivityPremiumBinding
import com.jacktor.batterylab.interfaces.PremiumInterface
import com.jacktor.batterylab.interfaces.PremiumInterface.Companion.billingClient
import com.jacktor.batterylab.interfaces.PremiumInterface.Companion.isPremium
import com.jacktor.batterylab.interfaces.PremiumInterface.Companion.mProductDetailsList
import com.jacktor.batterylab.interfaces.PremiumInterface.Companion.premiumActivityRef
import com.jacktor.batterylab.interfaces.RecyclerPremiumInterface
import com.jacktor.batterylab.utilities.Premium.PREMIUM_ID
import com.jacktor.batterylab.utilities.preferences.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds


class PremiumActivity() : AppCompatActivity(), RecyclerPremiumInterface, PremiumInterface {

    override var premiumContext: Context? = null

    private var activity: Activity? = null
    private var pref: Prefs? = null
    private var handler: Handler? = null
    private var adapter: PremiumAdapter? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var connectionState = 0

    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menetapkan referensi weak ke PremiumActivity
        premiumActivityRef = WeakReference(this)

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        activity = this
        handler = Handler(Looper.getMainLooper())
        pref = Prefs(this)
        mProductDetailsList = ArrayList()

        initiateBilling(false)

        binding.restoreFab.hide()

        connectionState = billingClient?.connectionState!!

        CoroutineScope(Dispatchers.Main).launch {
            delay(5.seconds)

            if (connectionState == 0) {
                binding.loadProducts.visibility = View.GONE
                binding.recyclerview.visibility = View.GONE
                binding.restoreFab.show()

                if (checkForInternet(this@PremiumActivity)) binding.jacktorMsg.text =
                    getString(R.string.purchase_cannot_be_made)
                else binding.jacktorMsg.text = getString(R.string.internet_connection_required)

                binding.jacktorImg.setImageResource(R.mipmap.jacktor_pose_3)
                binding.restoreFab.text = getString(R.string.refresh)
                binding.restoreFab.icon =
                    AppCompatResources.getDrawable(this@PremiumActivity, R.drawable.ic_refresh_24dp)

                val param = binding.premiumJacktor.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 200, 0, 0)
                binding.premiumJacktor.layoutParams = param
            }
        }

        //Top App Bar
        val topAppBar = findViewById<View>(R.id.topAppBar) as MaterialToolbar
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (isPremium) {
            binding.jacktorMsg.text = getString(R.string.jacktor_msg_purchased)
            binding.jacktorImg.setImageResource(R.mipmap.jacktor_pose_2)
        }

        //Tombol back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(activity, MainActivity::class.java))
                finish()
            }
        })


        //restore purchases
        binding.restoreFab.setOnClickListener {

            if (connectionState == 0) reloadScreen()
            else restorePurchases()
        }

        binding.showAllFeatures.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setIcon(R.drawable.ic_premium_24)
                setTitle(getString(R.string.premium))
                setMessage(getString(R.string.premium_dialog))
                setPositiveButton(R.string.dialog_button_close) { d, _ ->
                    d.dismiss()
                }

                setCancelable(false)
                show()
            }
        }
    }

    fun showProducts() {
        val productList = ImmutableList.of(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PREMIUM_ID)
                .setProductType(BillingClient.ProductType.INAPP).build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient!!.queryProductDetailsAsync(
            params
        ) { _: BillingResult?, prodDetailsList: List<ProductDetails>? ->
            // Process the result
            mProductDetailsList!!.clear()
            handler!!.postDelayed({
                connectionState = 2
                binding.restoreFab.show()
                binding.loadProducts.visibility = View.GONE
                mProductDetailsList!!.addAll(prodDetailsList!!)
                //Log.d(TAG, productDetailsList!!.size.toString() + " number of products")
                adapter = PremiumAdapter(
                    applicationContext,
                    mProductDetailsList!!,
                    this as RecyclerPremiumInterface
                )
                binding.recyclerview.setHasFixedSize(true)
                binding.recyclerview.layoutManager = LinearLayoutManager(
                    this, LinearLayoutManager.VERTICAL, false
                )
                binding.recyclerview.adapter = adapter
            }, 2000)
        }
    }

    private fun restorePurchases() {
        @Suppress("DEPRECATION")
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases()
            .setListener { _: BillingResult?, _: List<Purchase?>? -> }.build()
        val finalBillingClient: BillingClient = billingClient as BillingClient
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                startConnection(false)
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP).build()
                    ) { billingResult1: BillingResult, list: List<Purchase?> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.isNotEmpty()) {
                                checkPremium()

                                showSnackbar(
                                    binding.restoreFab,
                                    getString(R.string.restore_purchases_success),
                                    Snackbar.LENGTH_SHORT
                                )

                            } else {
                                showSnackbar(
                                    binding.restoreFab,
                                    getString(R.string.restore_purchases_not_found),
                                    Snackbar.LENGTH_SHORT
                                )
                            }
                        }
                    }
                }
            }
        })
    }


    @Suppress("DEPRECATION")
    fun reloadScreen() {
        //Reload the screen
        finish()

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else overridePendingTransition(0, 0)

        startActivity(intent)

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else overridePendingTransition(0, 0)
    }

    fun showSnackbar(view: View?, message: String?, duration: Int) {
        Snackbar.make(view!!, message!!, duration).show()
    }

    override fun onItemClick(pos: Int) {
        if (isPremium) {
            showSnackbar(
                binding.restoreFab, getString(R.string.already_purchased_sb), Snackbar.LENGTH_SHORT
            )
        } else {
            showSnackbar(binding.restoreFab, getString(R.string.wait), Snackbar.LENGTH_LONG)
            if (MainApp.Companion.isGooglePlay(this)) {
                if (billingClient?.isReady == true) {
                    launchPurchaseFlow(mProductDetailsList!![pos])
                } else {
                    initiateBilling(true)
                }
            } else showInstallAppFromGooglePlayDialog(this)
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

            billingClient?.launchBillingFlow(this, billingFlowParams)
        }
    }

    override fun onResume() {
        super.onResume()

        //Firebase Analytics
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, localClassName)
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Premium")
        }
        firebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }


    override fun onDestroy() {
        premiumActivityRef?.clear()
        super.onDestroy()
    }
}