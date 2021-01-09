package com.tommasoberlose.anotherwidget.ui.activities.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityIntegrationsBinding
import com.tommasoberlose.anotherwidget.ui.viewmodels.settings.IntegrationsViewModel
import net.idik.lib.slimadapter.SlimAdapter

class IntegrationsActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: IntegrationsViewModel
    private lateinit var binding: ActivityIntegrationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(IntegrationsViewModel::class.java)
        binding = ActivityIntegrationsBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.application_info_layout) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.default_name))

            }
            .attachTo(binding.listView)

        setupListener()
        subscribeUi(binding, viewModel)

        setContentView(binding.root)
    }

    private fun subscribeUi(binding: ActivityIntegrationsBinding, viewModel: IntegrationsViewModel) {
        binding.viewModel = viewModel

    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }
    }
}
