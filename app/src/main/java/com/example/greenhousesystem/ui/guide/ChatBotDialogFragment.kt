package com.example.greenhousesystem.ui.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.greenhousesystem.databinding.FragmentChatBotDialogBinding
import com.example.greenhousesystem.model.ChatMessage
import com.example.greenhousesystem.data.ChatMessageDao
import com.example.greenhousesystem.data.AppDatabase
import com.example.greenhousesystem.ui.guide.GeminiManager
import kotlinx.coroutines.launch

class ChatBotDialogFragment : DialogFragment() {

    private var _binding: FragmentChatBotDialogBinding? = null
    private val binding get() = _binding!!

    var onDialogDismissed: (() -> Unit)? = null

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatBotAdapter
    private lateinit var chatMessageDao: ChatMessageDao
    private lateinit var geminiManager: GeminiManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        _binding = FragmentChatBotDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatMessageDao= AppDatabase.getDatabase(requireContext()).ChatMessageDao()
        geminiManager = GeminiManager(requireContext())
        // 2. Cài đặt RecyclerView
        setupRecyclerView()
        // Dữ liệu trên room
        loadingChatHistory()
        // 3. Xử lý nút Đóng ("X")
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // 4. Xử lý khi bấm nút Gửi
        binding.btnSend.setOnClickListener {
            val userText = binding.etMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                // Thêm tin nhắn của User vào danh sách
                sendMessageToAi(userText)
                binding.etMessage.setText("")
            }
        }
    }


    private fun loadingChatHistory(){
        lifecycleScope.launch {
            chatMessageDao.getAllMessages().collect{
                listRoomDatabase ->
                    messageList.clear()
                    messageList.addAll(listRoomDatabase)
                    chatAdapter.notifyDataSetChanged()

                    if(messageList.isNotEmpty()){
                        binding.recyclerViewChat.scrollToPosition(messageList.size -1)
                    }
            }
        }
    }

    private fun sendMessageToAi(userMessage: String){
        lifecycleScope.launch {
            binding.btnSend.isEnabled = false

            chatMessageDao.insertMessage(ChatMessage(message = userMessage, isUser = true))

            // AI rep
            val responseText = geminiManager.generateAiResponse(userMessage)

            chatMessageDao.insertMessage(ChatMessage(message = responseText, isUser = false))

            binding.btnSend.isEnabled = true
        }
    }
    private fun setupRecyclerView() {
        chatAdapter = ChatBotAdapter(messageList)

        val layoutManager = LinearLayoutManager(requireContext())


        binding.recyclerViewChat.layoutManager = layoutManager
        binding.recyclerViewChat.adapter = chatAdapter

        chatAdapter.notifyDataSetChanged()
    }


    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}