package com.tommasoberlose.anotherwidget.ui.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.util.Util
import kotlinx.android.synthetic.main.activity_support_dev.*

class SupportDevActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {
    internal lateinit var bp: BillingProcessor

    internal val BILLING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAox5CcxuoLJ6CmNS7s6lVQzJ253njKKGF8MoQ/gQ5gEw2Fr03fBvtHpiVMpnjhNLw5NMeIpzRvkVqeQ7BfkC7c0BLCJUqf/fFA11ArQe8na6QKt5O4d+v4sbHtP7mm3GQNPOBaqRzcpFZaiAbfk6mnalo+tzM47GXrQFt5bNSrMctCs7bbChqJfH2cyMW0F8DHWEEeO5xElBmH3lh4FVpwIUTPYJIV3n0yhE3qqRA0WXkDej66g/uAt/rebmMZLmwNwIive5cObU4o41YyKRv2wSAicrv3W40LftzXAOOordIbmzDFN8ksh3VrnESqwCDGG97nZVbPG/+3LD0xHWiRwIDAQAB"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_dev)
        bp = BillingProcessor(this, BILLING_KEY, this)

        action_website.setOnClickListener {
            Util.openURI(this, "http://tommasoberlose.com/")
        }

        action_translate.setOnClickListener {
            Util.openURI(this, "https://github.com/tommasoberlose/another-widget/blob/master/app/src/main/res/values/strings.xml")
        }
    }

    override fun onBillingInitialized() {
        loader.visibility = View.GONE
        try {
            val isAvailable = BillingProcessor.isIabServiceAvailable(this)
            val isOneTimePurchaseSupported = bp.isOneTimePurchaseSupported
            if (isAvailable && isOneTimePurchaseSupported) {
                val coffee = bp.getPurchaseListingDetails("donation_coffee")
                val donuts = bp.getPurchaseListingDetails("donation_donuts")
                val breakfast = bp.getPurchaseListingDetails("donation_breakfast")
                val lunch = bp.getPurchaseListingDetails("donation_lunch")
                val dinner = bp.getPurchaseListingDetails("donation_dinner")

                if (coffee != null) {
                    import_donation_coffee.text = coffee.priceText
                    action_donation_coffee.setOnClickListener {
                        bp.purchase(this, "donation_coffee")
                    }
                } else {
                    action_donation_coffee.visibility = View.GONE
                }

                if (donuts != null) {
                    import_donation_donuts.text = donuts.priceText
                    action_donation_donuts.setOnClickListener {
                        bp.purchase(this, "donation_donuts")
                    }
                } else {
                    action_donation_donuts.visibility = View.GONE
                }

                if (breakfast != null) {
                    import_donation_breakfast.text = breakfast.priceText
                    action_donation_breakfast.setOnClickListener {
                        bp.purchase(this, "donation_breakfast")
                    }
                } else {
                    action_donation_breakfast.visibility = View.GONE
                }

                if (lunch != null) {
                    import_donation_lunch.text = lunch.priceText
                    action_donation_lunch.setOnClickListener {
                        bp.purchase(this, "donation_lunch")
                    }
                } else {
                    action_donation_lunch.visibility = View.GONE
                }

                if (dinner != null) {
                    import_donation_dinner.text = dinner.priceText
                    action_donation_dinner.setOnClickListener {
                        bp.purchase(this, "donation_dinner")
                    }
                } else {
                    action_donation_dinner.visibility = View.GONE
                }

                products_list.visibility = View.VISIBLE
            } else {
                products_card.visibility = View.GONE
            }
        } catch (ignored: Exception) {
            products_card.visibility = View.GONE
        }
    }

    override fun onPurchaseHistoryRestored() {
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        Toast.makeText(this, R.string.thanks, Toast.LENGTH_SHORT).show()
        bp.consumePurchase(productId)
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    public override fun onDestroy() {
        bp.release()
        super.onDestroy()
    }
}
