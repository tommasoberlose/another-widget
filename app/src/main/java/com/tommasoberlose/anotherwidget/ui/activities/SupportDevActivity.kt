package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivitySupportDevBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.SupportDevViewModel
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.activity_support_dev.*
import net.idik.lib.slimadapter.SlimAdapter

class SupportDevActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private val BILLING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAox5CcxuoLJ6CmNS7s6lVQzJ253njKKGF8MoQ/gQ5gEw2Fr03fBvtHpiVMpnjhNLw5NMeIpzRvkVqeQ7BfkC7c0BLCJUqf/fFA11ArQe8na6QKt5O4d+v4sbHtP7mm3GQNPOBaqRzcpFZaiAbfk6mnalo+tzM47GXrQFt5bNSrMctCs7bbChqJfH2cyMW0F8DHWEEeO5xElBmH3lh4FVpwIUTPYJIV3n0yhE3qqRA0WXkDej66g/uAt/rebmMZLmwNwIive5cObU4o41YyKRv2wSAicrv3W40LftzXAOOordIbmzDFN8ksh3VrnESqwCDGG97nZVbPG/+3LD0xHWiRwIDAQAB"
    private lateinit var viewModel: SupportDevViewModel
    private lateinit var adapter: SlimAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(SupportDevViewModel::class.java)
        viewModel.billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build()
        val binding = DataBindingUtil.setContentView<ActivitySupportDevBinding>(this, R.layout.activity_support_dev)


        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<SkuDetails>(R.layout.inapp_product_layout) { item, injector ->
                injector
                    .text(R.id.product_title, item.title.replace("(Another Widget)", ""))
                    .text(R.id.product_price, item.price)
                    .clicked(R.id.item) {
                        viewModel.purchase(this, item)
                    }
            }
            .attachTo(list_view)

        viewModel.openConnection()
        subscribeUi(viewModel)

        action_back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun subscribeUi(viewModel: SupportDevViewModel) {
        viewModel.products.observe(this, Observer {
            if (it.isNotEmpty()) {
                loader.isVisible = false
            }
            adapter.updateData(it.sortedWith(compareBy(SkuDetails::getPriceAmountMicros)))
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    viewModel.handlePurchase(purchase)
                    toast(getString(R.string.thanks))
                }
            }
        } else if (billingResult.responseCode == USER_CANCELED) {
            // DO nothing
        } else {
            toast(getString(R.string.error))
        }
    }

    public override fun onDestroy() {
        viewModel.closeConnection()
        super.onDestroy()
    }
}
