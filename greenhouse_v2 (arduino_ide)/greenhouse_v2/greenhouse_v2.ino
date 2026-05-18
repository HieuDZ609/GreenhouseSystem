/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║        GREENHOUSE IoT FIRMWARE — ESP32-S3 (Arduino Core 3.x)           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  PART 1 — HƯỚNG DẪN NỐI MẠCH (WIRING GUIDE)                           ║
 * ║  ═══════════════════════════════════════════════════════                 ║
 * ║                                                                          ║
 * ║  [ DHT22 ]                                                               ║
 * ║  ─────────────────────────────────────────────────────────────           ║
 * ║  DHT22 có 4 chân (trái → phải khi nhìn mặt trước lưới):                ║
 * ║    Pin 1 (VCC)  → 3.3V của ESP32-S3                                     ║
 * ║    Pin 2 (DATA) → GPIO 4 của ESP32-S3                                   ║
 * ║                   + Điện trở PULL-UP 10kΩ nối từ DATA lên 3.3V         ║
 * ║                   ⚠️ BẮT BUỘC có pull-up! Không có → đọc NaN liên tục  ║
 * ║    Pin 3 (NC)   → Bỏ trống (Not Connected)                              ║
 * ║    Pin 4 (GND)  → GND của ESP32-S3                                      ║
 * ║                                                                          ║
 * ║  Sơ đồ pull-up:                                                          ║
 * ║    3.3V ──┬── 10kΩ ──┬── GPIO 4                                         ║
 * ║           │           └── DHT22 Pin 2 (DATA)                            ║
 * ║           └── DHT22 Pin 1 (VCC)                                         ║
 * ║                                                                          ║
 * ║  [ RGB LED — Common Cathode (Âm Chung) ]                                ║
 * ║  ─────────────────────────────────────────────────────────────           ║
 * ║  LED RGB có 4 chân. Nhận biết chân:                                     ║
 * ║    → Chân DÀI NHẤT = GND (Cathode chung)                                ║
 * ║    → 3 chân còn lại (ngắn hơn) = R, G, B (Anode)                       ║
 * ║    → Thứ tự từ trái sang phải: R | Cathode(GND) | G | B                ║
 * ║                                                                          ║
 * ║  Vì sao cần điện trở khác nhau cho từng màu?                            ║
 * ║    Mỗi màu LED có Vf (điện áp ngưỡng) khác nhau:                       ║
 * ║      LED Đỏ  (R): Vf ≈ 1.8V → cần điện trở LỚN HƠN để giảm dòng      ║
 * ║      LED Xanh lá (G): Vf ≈ 2.2V                                         ║
 * ║      LED Xanh dương (B): Vf ≈ 3.0V → Vf gần 3.3V → điện trở nhỏ hơn  ║
 * ║    Dùng điện trở đồng đều cho cả 3 → màu bị mất cân bằng (đỏ quá sáng)║
 * ║                                                                          ║
 * ║  Kết nối:                                                                ║
 * ║    Chân R   → 330Ω → GPIO 15  (màu đỏ, Vf thấp → dùng R cao hơn)      ║
 * ║    Chân G   → 220Ω → GPIO 16  (màu xanh lá)                            ║
 * ║    Chân B   → 220Ω → GPIO 17  (màu xanh dương, Vf cao → R nhỏ hơn)    ║
 * ║    Chân GND → GND của ESP32-S3 (chân dài nhất)                         ║
 * ║                                                                          ║
 * ║  Sơ đồ nối:                                                              ║
 * ║    GPIO 15 ── 330Ω ──[R anode]─┐                                        ║
 * ║    GPIO 16 ── 220Ω ──[G anode]─┤── [Cathode chung] ── GND              ║
 * ║    GPIO 17 ── 220Ω ──[B anode]─┘                                        ║
 * ║                                                                          ║
 * ║  ⚠️ KHÔNG nối trực tiếp LED vào GPIO không có điện trở → cháy GPIO!    ║
 * ║                                                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  PART 2 — CẤU TRÚC FIREBASE REALTIME DATABASE                          ║
 * ║  ═══════════════════════════════════════════════════════                 ║
 * ║                                                                          ║
 * ║  GreenHouseSystem/                                                       ║
 * ║  ├── sensors/                    ← ESP32 GHI (WRITE)                    ║
 * ║  │   ├── temperature: 28.5                                               ║
 * ║  │   ├── humidity: 65.2                                                  ║
 * ║  │   └── timestamp: 1717123456000                                        ║
 * ║  │                                                                       ║
 * ║  └── devices/                                                            ║
 * ║      └── led/                    ← ESP32 ĐỌC (STREAM)                  ║
 * ║          ├── isOn: true                                                  ║
 * ║          ├── red: 255                                                    ║
 * ║          ├── green: 0                                                    ║
 * ║          ├── blue: 80                                                    ║
 * ║          └── mode: "FLOWERING"                                           ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * PART 3 — FIRMWARE CODE (bên dưới)
 *
 * Thư viện cần cài trong Arduino Library Manager:
 *   1. WiFiManager by tzapu         (≥ 2.0.17)
 *   2. Firebase ESP Client by Mobizt (≥ 4.4.x)
 *   3. DHT sensor library by Adafruit
 *   4. Adafruit Unified Sensor by Adafruit
 *
 * Board: ESP32S3 Dev Module (Arduino Core 3.x)
 */

// ════════════════════════════════════════════════════════════════
//  INCLUDES
// ════════════════════════════════════════════════════════════════
#include <Arduino.h>
#include <WiFi.h>
#include <WiFiManager.h>           // Captive Portal — không hardcode WiFi
#include <Firebase_ESP_Client.h>   // Firebase Realtime Database
#include <DHT.h>                   // Cảm biến DHT22
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>       // Mutex (SemaphoreHandle_t)

// Bắt buộc với Firebase ESP Client v4+
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"


// ════════════════════════════════════════════════════════════════
//  ⚙️ CẤU HÌNH — Chỉnh 4 hằng số này trước khi nạp firmware
// ════════════════════════════════════════════════════════════════
#define FIREBASE_API_KEY     "AIzaSyBiGw33P9_giJzGwqn8-U6vAVVtnA8ucAI"
#define FIREBASE_DB_URL      "https://greenhousesystem-97224-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define FIREBASE_USER_EMAIL  "hieuhdt.24it@vku.udn.vn"
#define FIREBASE_USER_PASS   "123456"


// ════════════════════════════════════════════════════════════════
//  📌 GPIO PIN MAPPING
// ════════════════════════════════════════════════════════════════
// DHT22
#define DHT_PIN        4       // GPIO 4 — DATA (cần 10kΩ pull-up lên 3.3V)
#define DHT_TYPE       DHT22

// RGB LED — Common Cathode
// Mỗi màu nối qua điện trở trước khi vào GPIO:
//   R → 330Ω → GPIO 15
//   G → 220Ω → GPIO 16
//   B → 220Ω → GPIO 17
#define LED_RED_PIN    15
#define LED_GREEN_PIN  16
#define LED_BLUE_PIN   17


// ════════════════════════════════════════════════════════════════
//  📡 PWM CONFIGURATION (Arduino Core 3.x API)
//
//  ⚠️ QUAN TRỌNG — Core 3.x đổi hoàn toàn API so với Core 2.x:
//    Core 2.x: ledcSetup(channel, freq, res) + ledcAttachPin(pin, ch)
//    Core 3.x: ledcAttach(pin, freq, res)    ← Gộp thành 1 hàm
//              ledcWrite(pin, duty)           ← Dùng pin thay vì channel
//  Dùng API Core 2.x trên Core 3.x → BUILD ERROR hoặc hành vi sai!
// ════════════════════════════════════════════════════════════════
#define PWM_FREQUENCY  5000    // 5 kHz — không thấy nhấp nháy bằng mắt
#define PWM_RESOLUTION    8    // 8-bit: duty cycle 0 → 255


// ════════════════════════════════════════════════════════════════
//  ⏱️ SMOOTH FADE PARAMETERS
// ════════════════════════════════════════════════════════════════
#define FADE_DURATION_MS   500   // Tổng thời gian fade: 500ms
#define FADE_STEP_MS        15   // Cập nhật mỗi 15ms → ~33 bước
//  33 bước × 15ms = ~495ms ≈ 500ms, đủ mượt mà, CPU nhẹ


// ════════════════════════════════════════════════════════════════
//  🔥 FIREBASE OBJECTS
// ════════════════════════════════════════════════════════════════
FirebaseData   fbStream;     // Object riêng cho Stream LED (Core 1)
FirebaseData   fbSensor;     // Object riêng cho push Sensor (Core 0)
FirebaseAuth   auth;
FirebaseConfig firebaseConfig;


// ════════════════════════════════════════════════════════════════
//  🌡️ DHT SENSOR
// ════════════════════════════════════════════════════════════════
DHT dht(DHT_PIN, DHT_TYPE);


// ════════════════════════════════════════════════════════════════
//  🔒 MUTEX & BIẾN DÙNG CHUNG GIỮA 2 TASKS
//
//  Vấn đề: firebaseStreamCallback() chạy trong context của
//  Firebase library thread, còn smoothFadeTo() chạy trong
//  Task_Stream (Core 1). Nếu cả 2 cùng đọc/ghi ledTarget*
//  không đồng bộ → Race Condition → LED nhấp nháy sai màu.
//
//  Giải pháp: SemaphoreHandle_t (Binary Mutex) của FreeRTOS.
//  Quy tắc: Bất kỳ ai muốn đọc/ghi ledTarget* phải xin mutex trước.
// ════════════════════════════════════════════════════════════════
SemaphoreHandle_t ledMutex;  // Mutex bảo vệ biến LED

// Màu MỤC TIÊU — nhận từ Firebase Stream
volatile int  ledTargetR  = 0;
volatile int  ledTargetG  = 0;
volatile int  ledTargetB  = 0;
volatile bool ledIsOn     = false;
volatile bool ledNewData  = false;  // Flag: có config mới cần xử lý

// Màu HIỆN TẠI — giá trị đang xuất ra PWM thực tế
int ledCurrentR = 0;
int ledCurrentG = 0;
int ledCurrentB = 0;


// ════════════════════════════════════════════════════════════════
//  📂 FIREBASE PATHS (khớp với Android App)
// ════════════════════════════════════════════════════════════════
const char* PATH_SENSORS    = "GreenHouseSystem/sensors";
const char* PATH_LED_STREAM = "GreenHouseSystem/devices/led";


// ════════════════════════════════════════════════════════════════
//  FUNCTION PROTOTYPES
// ════════════════════════════════════════════════════════════════
void     initPWM();
void     initWiFi();
void     initFirebase();
void     setLedPWM(int r, int g, int b);
void     smoothFadeTo(int targetR, int targetG, int targetB);
void     Task_Sensors(void* pvParameters);
void     Task_Stream(void* pvParameters);
void     streamCallback(FirebaseStream data);
void     streamTimeoutCallback(bool timeout);


// ════════════════════════════════════════════════════════════════
//  SETUP — Chạy 1 lần trên Core 1 (mặc định của Arduino)
// ════════════════════════════════════════════════════════════════
void setup() {
    Serial.begin(115200);
    delay(500);
    Serial.println("\n\n╔══════════════════════════════╗");
    Serial.println("║  GREENHOUSE FIRMWARE BOOT    ║");
    Serial.println("╚══════════════════════════════╝");

    // ── 1. Khởi tạo Mutex trước tiên ────────────────────────────
    // Tạo Mutex trước khi init bất cứ thứ gì để tránh task nào
    // đó cố gắng lấy mutex trước khi nó được tạo.
    ledMutex = xSemaphoreCreateMutex();
    configASSERT(ledMutex != NULL);  // Dừng nếu tạo thất bại
    Serial.println("[INIT] Mutex đã tạo.");

    // ── 2. Khởi tạo PWM cho LED (Core 3.x API) ──────────────────
    initPWM();

    // ── 3. Khởi tạo DHT22 ───────────────────────────────────────
    dht.begin();
    Serial.println("[INIT] DHT22 đã khởi tạo trên GPIO " + String(DHT_PIN));

    // ── 4. WiFi qua WiFiManager (Captive Portal) ─────────────────
    initWiFi();

    // ── 5. Firebase ──────────────────────────────────────────────
    initFirebase();

    // ── 6. Tạo FreeRTOS Tasks ───────────────────────────────────
    /**
     * xTaskCreatePinnedToCore(function, name, stackSize,
     *                         param, priority, handle, coreID)
     *
     * Task_Sensors → Core 0: đọc DHT22 + push Firebase mỗi 5 giây
     * Task_Stream  → Core 1: lắng nghe Firebase Stream + điều khiển LED
     *
     * Stack 8192 bytes: Firebase SSL/TLS cần nhiều stack,
     *                   8KB là mức an toàn tối thiểu.
     * Priority 2: Đủ để chạy đều đặn, không tranh chấp với WiFi stack.
     */
    xTaskCreatePinnedToCore(
        Task_Sensors,    // Hàm thực thi
        "Task_Sensors",  // Tên (hiển thị trong debug)
        8192,            // Stack size (bytes)
        NULL,            // Tham số truyền vào (không dùng)
        2,               // Priority
        NULL,            // Task handle (không cần lưu)
        0                // ← Ghim vào Core 0
    );

    xTaskCreatePinnedToCore(
        Task_Stream,
        "Task_Stream",
        8192,
        NULL,
        2,
        NULL,
        1               // ← Ghim vào Core 1
    );

    Serial.println("[SETUP] 2 FreeRTOS tasks đã tạo thành công.");
    Serial.println("[SETUP] Hệ thống sẵn sàng!");
}

// ════════════════════════════════════════════════════════════════
//  LOOP — Để trống
//  Toàn bộ logic nằm trong FreeRTOS tasks, không dùng loop().
// ════════════════════════════════════════════════════════════════
void loop() {
    // FreeRTOS scheduler quản lý hoàn toàn
    // Đặt vTaskDelay để không waste CPU trong task mặc định
    vTaskDelay(portMAX_DELAY);
}


// ════════════════════════════════════════════════════════════════
//  initPWM — Khởi tạo PWM cho 3 chân LED (Core 3.x API)
//
//  Arduino Core 3.x gộp ledcSetup + ledcAttachPin thành 1 hàm:
//    ledcAttach(pin, frequency, resolution)
//  Sau đó điều khiển bằng:
//    ledcWrite(pin, dutyCycle)   ← Dùng PIN, không phải channel number
//
//  So với Core 2.x:
//    ledcSetup(0, 5000, 8);        // Cũ
//    ledcAttachPin(LED_RED, 0);    // Cũ
//    ledcWrite(0, 128);            // Cũ — dùng channel
// ════════════════════════════════════════════════════════════════
void initPWM() {
    // ✅ Core 3.x: Chỉ cần 1 dòng cho mỗi chân
    ledcAttach(LED_RED_PIN,   PWM_FREQUENCY, PWM_RESOLUTION);
    ledcAttach(LED_GREEN_PIN, PWM_FREQUENCY, PWM_RESOLUTION);
    ledcAttach(LED_BLUE_PIN,  PWM_FREQUENCY, PWM_RESOLUTION);

    // Tắt LED lúc khởi động (duty = 0)
    setLedPWM(0, 0, 0);
    Serial.println("[PWM] 3 kênh LED đã khởi tạo (Core 3.x API).");
}


// ════════════════════════════════════════════════════════════════
//  initWiFi — WiFiManager Captive Portal
//
//  Luồng hoạt động:
//  1. ESP32 thử đọc WiFi đã lưu trong flash NVS → kết nối
//  2. Nếu không có WiFi đã lưu (lần đầu) hoặc không kết nối được:
//     → Tạo Access Point tên "Greenhouse_Setup"
//  3. Người dùng dùng điện thoại kết nối vào AP "Greenhouse_Setup"
//  4. Trình duyệt tự mở trang cấu hình (Captive Portal)
//  5. Chọn mạng WiFi nhà + nhập password → Lưu vào flash → Kết nối
//  6. Lần sau khởi động: tự kết nối không cần cấu hình lại
//
//  ⚠️ KHÔNG bao giờ hardcode SSID/Password trong firmware!
//     → Nếu đổi router sẽ phải nạp lại firmware
// ════════════════════════════════════════════════════════════════
void initWiFi() {
    Serial.println("[WiFi] Khởi động WiFiManager...");

    WiFiManager wifiManager;

    // Nếu sau 180 giây không ai cấu hình → reset và khởi động lại
    wifiManager.setConfigPortalTimeout(180);

    // Tắt debug output của WiFiManager để Serial sạch hơn (tuỳ chọn)
    // wifiManager.setDebugOutput(false);

    // autoConnect(apName, apPassword):
    //   - Thử kết nối WiFi đã lưu
    //   - Nếu thất bại → bật AP "Greenhouse_Setup" với password bên dưới
    //   - Bỏ qua password (NULL) nếu muốn AP mở hoàn toàn
    bool connected = wifiManager.autoConnect("Greenhouse_Setup", "green1234");

    if (!connected) {
        Serial.println("[WiFi] ❌ Timeout! Khởi động lại ESP32...");
        delay(2000);
        ESP.restart();
    }

    Serial.print("[WiFi] ✅ Đã kết nối! IP: ");
    Serial.println(WiFi.localIP());
}


// ════════════════════════════════════════════════════════════════
//  initFirebase — Cấu hình và kết nối Firebase RTDB
//
//  Firebase ESP Client v4+ yêu cầu cấu hình qua struct:
//    config.api_key      → Web API Key từ Firebase Console
//    config.database_url → URL của Realtime Database
//    auth.user.email/password → Email Authentication
//
//  Token tự động được refresh bởi SDK.
//  tokenStatusCallback in ra Serial khi token thay đổi trạng thái.
// ════════════════════════════════════════════════════════════════
void initFirebase() {
    Serial.println("[Firebase] Đang khởi tạo...");

    firebaseConfig.api_key      = FIREBASE_API_KEY;
    firebaseConfig.database_url = FIREBASE_DB_URL;

    auth.user.email    = FIREBASE_USER_EMAIL;
    auth.user.password = FIREBASE_USER_PASS;

    // Callback báo trạng thái token (defined in TokenHelper.h)
    firebaseConfig.token_status_callback = tokenStatusCallback;

    // Số lần thử tạo lại token khi thất bại (mặc định = 0 = vô hạn)
    firebaseConfig.max_token_generation_retry = 5;

    // Khởi động Firebase
    Firebase.begin(&firebaseConfig, &auth);

    // Tự động reconnect khi mất WiFi
    Firebase.reconnectNetwork(true);

    // Giới hạn buffer để tiết kiệm RAM
    // Rx = 4096 bytes, Tx = 1024 bytes
    fbStream.setBSSLBufferSize(4096, 1024);
    fbSensor.setBSSLBufferSize(4096, 1024);

    // Chờ token được cấp (tối đa 15 giây)
    Serial.print("[Firebase] Đang chờ token");
    unsigned long t0 = millis();
    while (Firebase.authTokenInfo().status != token_status_ready) {
        if (millis() - t0 > 15000) {
            Serial.println("\n[Firebase] ⚠️ Timeout token. Tiếp tục...");
            break;
        }
        Serial.print(".");
        delay(300);
    }
    Serial.println("\n[Firebase] ✅ Sẵn sàng!");
}


// ════════════════════════════════════════════════════════════════
//  setLedPWM — Ghi duty cycle trực tiếp lên 3 chân LED
//
//  Dùng khi cần set màu ngay lập tức (không fade).
//  Ví dụ: isOn = false → tắt về (0, 0, 0) ngay.
//
//  ✅ Core 3.x: ledcWrite(pin, duty) — dùng pin, không dùng channel
// ════════════════════════════════════════════════════════════════
void setLedPWM(int r, int g, int b) {
    r = constrain(r, 0, 255);
    g = constrain(g, 0, 255);
    b = constrain(b, 0, 255);

    // ✅ Core 3.x API: ledcWrite(pin, dutyCycle)
    ledcWrite(LED_RED_PIN,   r);
    ledcWrite(LED_GREEN_PIN, g);
    ledcWrite(LED_BLUE_PIN,  b);

    // Cập nhật biến lưu màu hiện tại
    ledCurrentR = r;
    ledCurrentG = g;
    ledCurrentB = b;
}


// ════════════════════════════════════════════════════════════════
//  smoothFadeTo — Fade mượt từ màu hiện tại sang màu mới
//
//  Thuật toán: Linear Interpolation (Lerp) từng bước
//  ─────────────────────────────────────────────────────────────
//  Công thức tại bước step (1 → totalSteps):
//    t = step / totalSteps        → tiến trình 0.0 → 1.0
//    value = start + (target - start) × t
//
//  Ví dụ fade từ đỏ (255,0,0) → xanh lá (0,255,0) trong 33 bước:
//    Bước 1:  R=248, G=8,   B=0
//    Bước 17: R=128, G=128, B=0   ← giữa chừng
//    Bước 33: R=0,   G=255, B=0
//  ─────────────────────────────────────────────────────────────
//
//  vTaskDelay(pdMS_TO_TICKS(15)) sau mỗi bước:
//    → Nhường CPU cho tasks khác (Task_Sensors vẫn chạy bình thường)
//    → Không phải delay() blocking — Task_Sensors không bị trễ
//
//  Abort sớm: Nếu trong khi fade có lệnh LED mới đến
//    → Phát hiện qua ledNewData = true trong mutex check
//    → Dừng fade ngay, trả về task loop để xử lý lệnh mới
//    → Tránh LED bị "kẹt" giữa 2 màu khi người dùng đổi nhanh
// ════════════════════════════════════════════════════════════════
void smoothFadeTo(int targetR, int targetG, int targetB) {
    const int totalSteps = FADE_DURATION_MS / FADE_STEP_MS;  // ~33

    // Lưu điểm bắt đầu
    float startR = (float)ledCurrentR;
    float startG = (float)ledCurrentG;
    float startB = (float)ledCurrentB;

    targetR = constrain(targetR, 0, 255);
    targetG = constrain(targetG, 0, 255);
    targetB = constrain(targetB, 0, 255);

    Serial.printf("[LED] Fade: (%d,%d,%d) → (%d,%d,%d)\n",
                  ledCurrentR, ledCurrentG, ledCurrentB,
                  targetR, targetG, targetB);

    for (int step = 1; step <= totalSteps; step++) {

        // ── Kiểm tra xem có lệnh mới không (Abort nếu có) ──────
        // Dùng timeout=0 (không chờ) để không delay vòng lặp fade
        if (xSemaphoreTake(ledMutex, 0) == pdTRUE) {
            bool hasNewCommand = ledNewData;
            xSemaphoreGive(ledMutex);
            if (hasNewCommand) {
                Serial.println("[LED] Lệnh mới đến — dừng fade sớm.");
                return;  // Task loop sẽ bắt đầu fade mới ngay
            }
        }

        // ── Tính màu tại bước này (Linear Lerp) ─────────────────
        float t = (float)step / (float)totalSteps;
        int r = (int)roundf(startR + (targetR - startR) * t);
        int g = (int)roundf(startG + (targetG - startG) * t);
        int b = (int)roundf(startB + (targetB - startB) * t);

        setLedPWM(r, g, b);

        // ── Nhường CPU cho tasks khác trong 15ms ─────────────────
        // pdMS_TO_TICKS() chuyển ms → FreeRTOS tick (tránh hardcode)
        vTaskDelay(pdMS_TO_TICKS(FADE_STEP_MS));
    }

    // Đảm bảo đúng màu cuối (tránh sai số float ở step cuối)
    setLedPWM(targetR, targetG, targetB);
    Serial.println("[LED] ✅ Fade hoàn tất.");
}


// ════════════════════════════════════════════════════════════════
//  TASK_SENSORS — Core 0
//  Nhiệm vụ: Đọc DHT22 mỗi 5 giây → push lên Firebase RTDB
//
//  Path Firebase: GreenHouseSystem/sensors
//  JSON ghi lên:
//      "temperature": 28.5,
//      "humidity": 65.2,
//      "timestamp": 1717123456  (millis() — không có NTP)
//    }
//
//  Xử lý lỗi:
//  - isnan(temp): DHT22 trả về NaN khi đọc thất bại → bỏ qua
//  - Firebase write fail: in log, không crash, thử lại lần sau
//  - WiFi disconnect: Firebase.reconnectNetwork(true) tự xử lý
// ════════════════════════════════════════════════════════════════
void Task_Sensors(void* pvParameters) {
    Serial.println("[Task_Sensors] ▶ Bắt đầu trên Core 0.");

    // Đợi Firebase sẵn sàng trước khi push
    while (!Firebase.ready()) {
        Serial.println("[Task_Sensors] ⏳ Chờ Firebase...");
        vTaskDelay(pdMS_TO_TICKS(1500));
    }
    Serial.println("[Task_Sensors] ✅ Firebase sẵn sàng, bắt đầu đọc cảm biến.");

    int consecutiveErrors = 0;  // Đếm lỗi liên tiếp để cảnh báo

    for (;;) {
        // ── Đọc DHT22 ────────────────────────────────────────────
        float temperature = dht.readTemperature();
        float humidity    = dht.readHumidity();

        // isnan() kiểm tra kết quả không hợp lệ (Not a Number)
        // DHT22 cần ~2 giây giữa các lần đọc — đọc quá nhanh → NaN
        if (isnan(temperature) || isnan(humidity)) {
            consecutiveErrors++;
            Serial.printf("[Task_Sensors] ⚠️ Đọc DHT22 thất bại! (Lỗi liên tiếp: %d)\n",
                          consecutiveErrors);
            if (consecutiveErrors >= 5) {
                Serial.println("[Task_Sensors] ❌ 5 lỗi liên tiếp! Kiểm tra:");
                Serial.println("   1. Dây nối GPIO 4 đến DHT22 pin DATA");
                Serial.println("   2. Điện trở pull-up 10kΩ từ DATA lên 3.3V");
                Serial.println("   3. Nguồn 3.3V cho DHT22");
                consecutiveErrors = 0;  // Reset để không spam log
            }
            vTaskDelay(pdMS_TO_TICKS(5000));
            continue;
        }

        consecutiveErrors = 0;

        // ── Tạo JSON và push lên Firebase ────────────────────────
        FirebaseJson sensorJson;
        sensorJson.set("temperature", temperature);
        sensorJson.set("humidity",    humidity);
        sensorJson.set("timestamp/.sv", "timestamp");
        
        Serial.printf("[Task_Sensors] 📤 T=%.1f°C | H=%.1f%% → Firebase...\n",
                      temperature, humidity);

        // Firebase.RTDB.setJSON: ghi đè toàn bộ node
        if (Firebase.RTDB.setJSON(&fbSensor, PATH_SENSORS, &sensorJson)) {
            Serial.println("[Task_Sensors] ✅ Push thành công!");
        } else {
            // In lý do lỗi — có thể là: token expired, network error, path sai...
            Serial.println("[Task_Sensors] ❌ Push thất bại: " + fbSensor.errorReason());
        }

        static unsigned long lastHistoryPush = 0;
        if (millis() - lastHistoryPush > 900000 || lastHistoryPush == 0) {
            if (Firebase.RTDB.pushJSON(&fbSensor, "GreenHouseSystem/sensorHistory", &sensorJson)) {
                Serial.println("[Task_Sensors] 📈 Đã lưu 1 điểm lịch sử mới.");
                lastHistoryPush = millis();
            }
        }   
        // ── Chờ 5 giây (non-blocking) ────────────────────────────
        // vTaskDelay đặt task vào trạng thái Blocked → CPU chạy task khác
        // Khác hoàn toàn với delay(5000) — delay() "giữ CPU" (spin-wait)
        vTaskDelay(pdMS_TO_TICKS(5000));
    }

    // Không bao giờ đến đây — FreeRTOS tasks chạy vô tận
    vTaskDelete(NULL);
}


// ════════════════════════════════════════════════════════════════
//  streamCallback — Callback nhận dữ liệu từ Firebase Stream
//
//  Hàm này được Firebase SDK gọi khi node GreenHouseSystem/devices/led
//  có thay đổi. Chạy trong context của Firebase library thread.
//
//  Quy tắc trong callback:
//  ✅ Đọc dữ liệu + lưu vào biến volatile
//  ✅ Lấy và trả Mutex nhanh nhất có thể
//  ❌ KHÔNG gọi smoothFadeTo() — hàm này blocking 500ms
//  ❌ KHÔNG gọi Serial.println quá nhiều — có thể chậm callback
// ════════════════════════════════════════════════════════════════
void streamCallback(FirebaseStream data) {
    static int lastR = 0;
    static int lastG = 0;
    static int lastB = 0;
    static bool lastOnStatus = false;
    // 1. In toàn bộ data nhận được để soi lỗi kiểu dữ liệu
    Serial.println("\n[Stream] 📥 Đã nhận config mới!");
    Serial.printf("[Stream] Raw Payload: %s\n", data.payload().c_str());

    // 2. Chỉ xử lý nếu dữ liệu nhận về là JSON (nguyên object LED)
    if (data.dataType() == "json") {
        FirebaseJson &json = data.jsonObject();
        FirebaseJsonData result;

        // 2. Bóc tách và CẬP NHẬT "BỘ NHỚ" (chỉ cập nhật nếu trong JSON có key đó)
        if (json.get(result, "isOn"))  lastOnStatus = result.to<bool>();
        if (json.get(result, "red"))   lastR = result.to<int>();
        if (json.get(result, "green")) lastG = result.to<int>();
        if (json.get(result, "blue"))  lastB = result.to<int>();

        // 3. Tính toán Target thực tế dựa trên trạng thái Bật/Tắt
        // Nếu OnStatus là true -> lấy màu trong bộ nhớ. Nếu false -> về 0.
        int targetR = lastOnStatus ? lastR : 0;
        int targetG = lastOnStatus ? lastG : 0;
        int targetB = lastOnStatus ? lastB : 0;

        Serial.printf("[LOG] Trạng thái: %s | Màu ghi nhớ: (%d,%d,%d) | Mục tiêu LED: (%d,%d,%d)\n",
                      lastOnStatus ? "BẬT" : "TẮT", 
                      lastR, lastG, lastB,
                      targetR, targetG, targetB);

        // 4. Gửi dữ liệu an toàn sang Task xử lý LED (Core 1)
        if (xSemaphoreTake(ledMutex, pdMS_TO_TICKS(20)) == pdTRUE) {
            ledTargetR = targetR;
            ledTargetG = targetG;
            ledTargetB = targetB;
            ledIsOn    = lastOnStatus;
            ledNewData = true; 
            xSemaphoreGive(ledMutex);
        }
    }
}


// ════════════════════════════════════════════════════════════════
//  streamTimeoutCallback — Gọi khi Stream bị timeout/ngắt kết nối
//
//  Firebase SDK sẽ tự động reconnect stream sau callback này.
//  Chỉ cần log để biết tình trạng.
// ════════════════════════════════════════════════════════════════
void streamTimeoutCallback(bool timeout) {
    if (timeout) {
        Serial.println("[Stream] ⏱️ Stream timeout — Firebase đang reconnect...");
    }
    if (!fbStream.httpConnected()) {
        Serial.println("[Stream] 🔌 HTTP disconnected.");
    }
}


// ════════════════════════════════════════════════════════════════
//  TASK_STREAM — Core 1
//  Nhiệm vụ:
//  1. Khởi tạo Firebase Stream lắng nghe GreenHouseSystem/devices/led
//  2. Vòng lặp poll ledNewData flag (đặt bởi streamCallback)
//  3. Khi có lệnh mới → đọc target colors → gọi smoothFadeTo()
//  4. Giám sát kết nối stream, tự reconnect nếu cần
// ════════════════════════════════════════════════════════════════
void Task_Stream(void* pvParameters) {
    Serial.println("[Task_Stream] ▶ Bắt đầu trên Core 1.");

    // Chờ Firebase sẵn sàng
    while (!Firebase.ready()) {
        Serial.println("[Task_Stream] ⏳ Chờ Firebase...");
        vTaskDelay(pdMS_TO_TICKS(1500));
    }

    // ── Bắt đầu Firebase Stream ───────────────────────────────────
    // beginStream() chỉ cần gọi 1 lần.
    // setStreamCallback() đăng ký hàm xử lý khi có data mới và khi timeout.
    if (!Firebase.RTDB.beginStream(&fbStream, PATH_LED_STREAM)) {
        Serial.println("[Task_Stream] ❌ beginStream thất bại: " + fbStream.errorReason());
    }

    Firebase.RTDB.setStreamCallback(
        &fbStream,
        streamCallback,           // Gọi khi có dữ liệu mới
        streamTimeoutCallback     // Gọi khi timeout/ngắt kết nối
    );

    Serial.println("[Task_Stream] ✅ Stream đang lắng nghe: " + String(PATH_LED_STREAM));

    // Biến local để tránh giữ mutex trong khi fade
    int fadeR = 0, fadeG = 0, fadeB = 0;

    for (;;) {
        // ── Kiểm tra có lệnh LED mới không ───────────────────────
        bool shouldFade = false;

        // Timeout 5ms để không block vòng lặp quá lâu
        if (xSemaphoreTake(ledMutex, pdMS_TO_TICKS(5)) == pdTRUE) {
            if (ledNewData) {
                // Copy giá trị target ra biến local
                fadeR      = ledTargetR;
                fadeG      = ledTargetG;
                fadeB      = ledTargetB;
                ledNewData = false;    // Reset flag
                shouldFade = true;
            }
            xSemaphoreGive(ledMutex);
            // ↑ QUAN TRỌNG: Release mutex TRƯỚC KHI gọi smoothFadeTo()
            //   vì smoothFadeTo() mất 500ms và sẽ cần mutex bên trong
        }

        // ── Fade LED (ngoài vùng mutex) ───────────────────────────
        if (shouldFade) {
            smoothFadeTo(fadeR, fadeG, fadeB);
        }

        // ── Tự phục hồi nếu stream ngắt ──────────────────────────
        // Firebase SDK thường tự reconnect qua streamTimeoutCallback,
        // nhưng kiểm tra thêm một lớp nữa để đảm bảo chắc chắn.
        if (!fbStream.httpConnected()) {
            Serial.println("[Task_Stream] 🔄 Stream mất kết nối. Đang reconnect...");
            if (!Firebase.RTDB.beginStream(&fbStream, PATH_LED_STREAM)) {
                Serial.println("[Task_Stream] ❌ Reconnect thất bại: " + fbStream.errorReason());
                vTaskDelay(pdMS_TO_TICKS(5000));  // Chờ 5s rồi thử lại
            } else {
                Firebase.RTDB.setStreamCallback(&fbStream, streamCallback, streamTimeoutCallback);
                Serial.println("[Task_Stream] ✅ Reconnect thành công!");
            }
        }

        // Poll mỗi 20ms — đủ nhạy để phát hiện lệnh mới nhanh
        vTaskDelay(pdMS_TO_TICKS(20));
    }

    vTaskDelete(NULL);
}
