package com.tommasoberlose.anotherwidget.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityIntegrationsBinding
import com.tommasoberlose.anotherwidget.ui.viewmodels.IntegrationsViewModel
import kotlinx.android.synthetic.main.activity_integrations.*
import net.idik.lib.slimadapter.SlimAdapter

class IntegrationsActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: IntegrationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(IntegrationsViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityIntegrationsBinding>(this, R.layout.activity_integrations)

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.application_info_layout) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.default_name))

            }
            .attachTo(list_view)

        setupListener()
        subscribeUi(binding, viewModel)
    }

    private fun subscribeUi(binding: ActivityIntegrationsBinding, viewModel: IntegrationsViewModel) {
        binding.viewModel = viewModel

    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }
    }
}
