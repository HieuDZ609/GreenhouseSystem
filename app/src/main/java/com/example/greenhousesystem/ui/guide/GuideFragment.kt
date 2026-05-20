package com.example.greenhousesystem.ui.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.greenhousesystem.databinding.FragmentGuideBinding

class GuideFragment : Fragment() {

    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gán sự kiện click cho Header 1
        binding.header1.setOnClickListener {
            toggleSection(binding.content1, binding.arrow1)
        }

        // Làm tương tự cho header2, header3...
    }

    private fun toggleSection(contentView: View, arrowView: View) {
        val isCurrentlyVisible = contentView.visibility == View.VISIBLE

        if (isCurrentlyVisible) {
            // Đang mở -> Thu lại
            contentView.visibility = View.GONE
            arrowView.animate().rotation(0f).setDuration(200).start()
        } else {
            // Đang đóng -> Mở ra
            contentView.visibility = View.VISIBLE
            arrowView.animate().rotation(180f).setDuration(200).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}