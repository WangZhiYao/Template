package com.example.template.feature.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.example.template.feature.main.databinding.FragmentMainBinding
import com.example.template.shared.ui.base.BaseFragment
import com.example.template.shared.ui.ext.observe
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/**
 * Stateful screen: owns the ViewModel, renders state through ViewBinding and handles
 * one-shot side effects.
 *
 * @author WangZhiYao
 * @since 2025/11/16
 */
@AndroidEntryPoint
class MainFragment : BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // State and one-shot side effects are collected only while STARTED, and collection
        // stops when the view is destroyed (viewLifecycleOwner).
        viewModel.observe(
            viewLifecycleOwner,
            state = ::renderState,
            sideEffect = ::handleSideEffect,
        )

        binding.refreshButton.setOnClickListener { viewModel.onRefreshGreeting() }
    }

    private fun renderState(state: MainState) {
        binding.greetingText.text = state.greeting
    }

    private suspend fun handleSideEffect(sideEffect: MainSideEffect) {
        when (sideEffect) {
            is MainSideEffect.GreetingSaved ->
                Snackbar.make(binding.root, sideEffect.text, Snackbar.LENGTH_SHORT).show()
        }
    }
}
