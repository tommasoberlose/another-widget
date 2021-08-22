package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.blockingBulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomDateBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.CustomDateViewModel
import com.tommasoberlose.anotherwidget.utils.getCapWordString
import com.tommasoberlose.anotherwidget.utils.openURI
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class CustomDateActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomDateViewModel
    private lateinit var binding: ActivityCustomDateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomDateViewModel::class.java)
        binding = ActivityCustomDateBinding.inflate(layoutInflater)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_date_example_item) { item, injector ->
                injector
                    .text(R.id.custom_date_example_format, item)
                    .text(R.id.custom_date_example_value, SimpleDateFormat(item, Locale.getDefault()).format(
                        DATE.time))
            }
            .attachTo(binding.listView)

        adapter.updateData(
            listOf(
                "d", "dd", "EE", "EEEE", "MM", "MMM", "MMMM", "yy", "yyyy"
            )
        )

        setupListener()
        subscribeUi(binding, viewModel)

        binding.dateFormat.requestFocus()

        setContentView(binding.root)
    }

    private var formatJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomDateBinding, viewModel: CustomDateViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.dateInput.observe(this, { dateFormat ->
            formatJob?.cancel()
            formatJob = lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.loader.visibility = View.VISIBLE
                }

                delay(200)
                var text = if (dateFormat != "") {
                    try {
                        SimpleDateFormat(dateFormat, Locale.getDefault()).format(DATE.time)
                    } catch (e: Exception) {
                        ERROR_STRING
                    }
                } else {
                    ERROR_STRING
                }

                if (viewModel.isDateCapitalize.value == true) {
                    text = text.getCapWordString()
                }

                if (viewModel.isDateUppercase.value == true) {
                    text = text.toUpperCase(Locale.getDefault())
                }

                withContext(Dispatchers.Main) {
                    binding.loader.visibility = View.INVISIBLE
                    binding.dateFormatValue.text = text
                }

            }
        })

        viewModel.isDateCapitalize.observe(this, {
            viewModel.dateInput.value = viewModel.dateInput.value
            updateCapitalizeUi()
        })

        viewModel.isDateUppercase.observe(this, {
            viewModel.dateInput.value = viewModel.dateInput.value
            updateCapitalizeUi()
        })
    }

    private fun updateCapitalizeUi() {
        when {
            viewModel.isDateUppercase.value == true -> {
                binding.actionCapitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.round_publish))
                binding.actionCapitalize.alpha = 1f
            }
            viewModel.isDateCapitalize.value == true -> {
                binding.actionCapitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.ic_capitalize))
                binding.actionCapitalize.alpha = 1f
            }
            else -> {
                binding.actionCapitalize.setImageDrawable(ContextCompat.getDrawable(this@CustomDateActivity, R.drawable.round_publish))
                binding.actionCapitalize.alpha = 0.3f
            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }

        binding.actionCapitalize.setOnClickListener {
            when {
                viewModel.isDateUppercase.value == true -> {
                    viewModel.isDateCapitalize.value = false
                    viewModel.isDateUppercase.value = false
                }
                viewModel.isDateCapitalize.value == true -> {
                    viewModel.isDateCapitalize.value = false
                    viewModel.isDateUppercase.value = true
                }
                else -> {
                    viewModel.isDateCapitalize.value = true
                    viewModel.isDateUppercase.value = false
                }
            }
        }

        binding.actionCapitalize.setOnLongClickListener {
            toast(getString(R.string.action_capitalize_the_date))
            true
        }

        binding.actionDateFormatInfo.setOnClickListener {
            openURI("https://developer.android.com/reference/java/text/SimpleDateFormat")
        }
    }

    override fun onBackPressed() {
        Preferences.blockingBulk {
            dateFormat = viewModel.dateInput.value ?: ""
            isDateCapitalize = viewModel.isDateCapitalize.value ?: true
            isDateUppercase = viewModel.isDateUppercase.value ?: false
        }
        super.onBackPressed()
    }

    companion object {
        const val ERROR_STRING = "--"
        val DATE: Calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, 10)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, 1993)
        }
    }
}
