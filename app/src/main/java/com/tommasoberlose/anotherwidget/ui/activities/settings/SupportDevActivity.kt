package com.tommasoberlose.anotherwidget.ui.activities.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivitySupportDevBinding
import com.tommasoberlose.anotherwidget.ui.viewmodels.settings.SupportDevViewModel
import com.tommasoberlose.anotherwidget.utils.toast
import net.idik.lib.slimadapter.SlimAdapter

class SupportDevActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var viewModel: SupportDevViewModel
    private lateinit var adapter: SlimAdapter
    private lateinit var binding: ActivitySupportDevBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(SupportDevViewModel::class.java)
        viewModel.billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build()
        binding = ActivitySupportDevBinding.inflate(layoutInflater)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<SkuDetails>(R.layout.inapp_product_layout) { item, injector ->
                item.sku
                injector
                    .with<TextView>(R.id.product_title) {
                        it.text = when (item.sku) {
                            "donation_coffee" -> getString(R.string.donation_coffee)
                            "donation_donuts" -> getString(R.string.donation_donuts)
                            "donation_breakfast" -> getString(R.string.donation_breakfast)
                            "donation_lunch" -> getString(R.string.donation_lunch)
                            else -> ""
                        }
                    }
                    .text(R.id.product_price, item.price)
                    .clicked(R.id.item) {
                        viewModel.purchase(this, item)
                    }
            }
            .attachTo(binding.listView)

        viewModel.openConnection()
        subscribeUi(viewModel)

        binding.actionBack.setOnClickListener {
            onBackPressed()
        }

        setContentView(binding.root)
    }

    private fun subscribeUi(viewModel: SupportDevViewModel) {
        viewModel.products.observe(this, Observer {
            if (it.isNotEmpty()) {
                binding.loader.isVisible = false
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
