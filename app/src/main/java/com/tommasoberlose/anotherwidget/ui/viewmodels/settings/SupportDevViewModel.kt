//package com.tommasoberlose.anotherwidget.ui.viewmodels.settings
//
//import android.app.Activity
//import android.content.Context
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.android.billingclient.api.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class SupportDevViewModel : ViewModel() {
//
//    lateinit var billingClient: BillingClient
//    val products: MutableLiveData<List<SkuDetails>> = MutableLiveData(emptyList())
//
//    fun openConnection() {
//
//        billingClient.startConnection(object : BillingClientStateListener {
//            override fun onBillingSetupFinished(billingResult: BillingResult) {
//                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
//                    val params = SkuDetailsParams.newBuilder()
//                    params.setSkusList(listOf("donation_coffee", "donation_donuts", "donation_breakfast", "donation_lunch", "donation_dinner")).setType(BillingClient.SkuType.INAPP)
//                    viewModelScope.launch(Dispatchers.IO) {
//                        val skuDetailsList = billingClient.querySkuDetails(params.build()).skuDetailsList
//                        withContext(Dispatchers.Main) {
//                            products.value = skuDetailsList
//                        }
//                    }
//                }
//            }
//            override fun onBillingServiceDisconnected() {
//                // Try to restart the connection on the next request to
//                // Google Play by calling the startConnection() method.
//            }
//        })
//    }
//
//    fun purchase(activity: Activity, product: SkuDetails) {
//        val flowParams = BillingFlowParams.newBuilder()
//            .setSkuDetails(product)
//            .build()
//        billingClient.launchBillingFlow(activity, flowParams)
//    }
//
//    fun handlePurchase(purchase: Purchase) {
//        if (!purchase.isAcknowledged) {
//            viewModelScope.launch(Dispatchers.IO) {
//                val token = purchase.purchaseToken
//                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
//                    .setPurchaseToken(token)
//                billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
//
//                val consumeParams =
//                    ConsumeParams.newBuilder()
//                        .setPurchaseToken(token)
//                        .build()
//                billingClient.consumePurchase(consumeParams)
//            }
//        }
//    }
//
//    fun closeConnection() {
//        billingClient.endConnection()
//    }
//}