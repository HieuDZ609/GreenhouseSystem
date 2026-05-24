package com.example.greenhousesystem.ui.guide
import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.greenhousesystem.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
class GeminiManager (private val context: Context){
    private lateinit var geminiModel: GenerativeModel
    private var initErrorMessage: String? = null
    init {
        initGemini()
    }
    private fun initGemini() {
        try {
            // 1. Kiểm tra API Key xem có trống hoặc chưa cấu hình không
            val apiKey = BuildConfig.API_KEY
            if (apiKey.isNullOrEmpty() || apiKey == "null") {
                throw IllegalArgumentException("API_KEY chưa được cấu hình hoặc bị trống trong build.gradle!")
            }

            // 2. Đọc file Markdown hướng dẫn từ Assets
            val markdownGuide = loadTextFromAssets("appFunctionInstructions.md")
            if (markdownGuide.isNullOrEmpty()) {
                throw IOException("Không tìm thấy file 'appFunctionInstructions.md' trong thư mục assets hoặc file bị trống!")
            }

            // 3. Xây dựng System Instruction
            val systemInstructionText = """
                Bạn là trợ lý AI thông minh tích hợp trong ứng dụng giám sát nhà kính tên là 'GreenHouse System'.
                Nhiệm vụ của bạn tuân thủ nghiêm ngặt theo 2 nguồn kiến thức sau:
                
                Nguồn 1 (Hướng dẫn sử dụng App - Định dạng Markdown):
                $markdownGuide
                👉 Khi người dùng hỏi về cách sử dụng, điều khiển thiết bị, xem thông số trong app, hãy dựa vào các tiêu đề (#, ##) và các gạch đầu dòng trong dữ liệu Markdown trên để hướng dẫn chi tiết từng bước cho họ.
                
                Nguồn 2 (Kiến thức Nông nghiệp & Cây trồng):
                👉 Khi người dùng hỏi các kiến thức ngoài app như kỹ thuật trồng trọt, chăm sóc cây, sâu bệnh, nhiệt độ ánh sáng phù hợp cho cây trồng, bạn hãy đóng vai là một Chuyên gia nông nghiệp giàu kinh nghiệm để trả lời thật chu đáo và khoa học.
                
                Lưu ý: Trả lời ngắn gọn, thân thiện, dùng ngôn ngữ tiếng Việt tự nhiên.
            """.trimIndent()

            // 4. Khởi tạo GenerativeModel
            geminiModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                systemInstruction = content { text(systemInstructionText) }
            )

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            initErrorMessage = "Lỗi cấu hình: ${e.message}"
        } catch (e: IOException) {
            e.printStackTrace()
            initErrorMessage = "Lỗi đọc file cấu hình: ${e.message}"
        } catch (e: Exception) {
            e.printStackTrace()
            initErrorMessage = "Lỗi khởi tạo hệ thống AI không xác định: ${e.localizedMessage}"
        }
    }
    suspend fun generateAiResponse(userMessage: String): String = withContext(Dispatchers.IO){
        if (initErrorMessage != null) {
            return@withContext initErrorMessage!!
        }

        val model = geminiModel ?: return@withContext "Lỗi: Hệ thống AI chưa được khởi tạo thành công."
        try {
            val response= geminiModel.generateContent(userMessage)
            response.text ?: "Xin lỗi, mình chưa hiểu ý bạn."
        }
        catch (e: Exception){
            e.printStackTrace()
            val errorLog = e.localizedMessage ?: ""
            when {
                errorLog.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "Lỗi API: API Key của bạn không hợp lệ hoặc đã hết hạn."
                errorLog.contains("Quota exceeded", ignoreCase = true) ->
                    "Lỗi giới hạn: Tài khoản Gemini đã hết lượt checkpoint miễn phí hôm nay."
                else ->
                    "Lỗi kết nối mạng hoặc lỗi từ server Gemini: Không thể trò chuyện lúc này."
            }
        }
    }
    private fun loadTextFromAssets(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            null
        }
    }
}