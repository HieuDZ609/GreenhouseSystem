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


        binding.header1.setOnClickListener {
            toggleSection(binding.content1, binding.arrow1)
        }

        binding.header2.setOnClickListener {
            toggleSection(binding.content2, binding.arrow2)
        }
        binding.header3.setOnClickListener {
            toggleSection(binding.content3, binding.arrow3)
        }
        binding.header4.setOnClickListener {
            toggleSection(binding.content4, binding.arrow4)
        }
        binding.header5.setOnClickListener {
            toggleSection(binding.content5, binding.arrow5)
        }

        val floatAnimation = android.animation.ObjectAnimator.ofFloat(
            binding.tvSpeechBubble, "translationY", 0f, -10f, 0f
        ).apply {
            duration = 1500
            repeatCount = android.animation.ObjectAnimator.INFINITE
            repeatMode = android.animation.ObjectAnimator.REVERSE
        }
        floatAnimation.start()

        // Xử lý xuất hiện fragment chat_bot
        binding.fabRobot.setOnClickListener {
            binding.tvSpeechBubble.visibility= View.GONE
            floatAnimation.cancel()

            val chatBotDialog= ChatBotDialogFragment()
            chatBotDialog.onDialogDismissed = {
                binding.tvSpeechBubble.visibility = View.VISIBLE
                floatAnimation.start()
            }
            chatBotDialog.show(parentFragmentManager, "ChatBotDialog")
        }
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