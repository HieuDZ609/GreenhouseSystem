import { motion } from 'framer-motion';
import { 
  Settings as SettingsIcon, 
  Wifi, 
  Cpu, 
  RefreshCw, 
  Save,
  Terminal,
  Copy,
  Check
} from 'lucide-react';
import { useState } from 'react';

const esp32Code = `// GreenHouse Smart 2026 - ESP32-S3 Firmware
// Hardware: ESP32-S3 DevKitC, DHT22, Relay Module

#include <WiFi.h>
#include <WebSocketsClient.h>
#include <DHT.h>
#include <ArduinoJson.h>

// Pin Definitions
#define DHT_PIN 4
#define DHT_TYPE DHT22
#define LED_RELAY 12
#define FAN_RELAY 14
#define PUMP_RELAY 27
#define VENT_RELAY 26

// WiFi Credentials
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASS";

// WebSocket Server
const char* ws_host = "your-server.com";
const int ws_port = 8080;

DHT dht(DHT_PIN, DHT_TYPE);
WebSocketsClient webSocket;

struct DeviceState {
  bool led = false;
  bool fan = false;
  bool pump = false;
  bool vent = false;
} devices;

unsigned long lastSensorRead = 0;
const unsigned long SENSOR_INTERVAL = 5000; // 5 seconds

void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.println("WebSocket disconnected!");
      break;
    case WStype_CONNECTED:
      Serial.println("WebSocket connected!");
      break;
    case WStype_TEXT: {
      StaticJsonDocument<256> doc;
      deserializeJson(doc, payload);
      
      const char* command = doc["command"];
      const char* device = doc["device"];
      bool state = doc["state"];
      
      if (strcmp(command, "control") == 0) {
        handleDeviceControl(device, state);
      }
      break;
    }
  }
}

void handleDeviceControl(const char* device, bool state) {
  int pin = -1;
  
  if (strcmp(device, "led") == 0) { pin = LED_RELAY; devices.led = state; }
  else if (strcmp(device, "fan") == 0) { pin = FAN_RELAY; devices.fan = state; }
  else if (strcmp(device, "pump") == 0) { pin = PUMP_RELAY; devices.pump = state; }
  else if (strcmp(device, "vent") == 0) { pin = VENT_RELAY; devices.vent = state; }
  
  if (pin >= 0) {
    digitalWrite(pin, state ? HIGH : LOW);
    sendDeviceStatus();
  }
}

void readSensors() {
  float temp = dht.readTemperature();
  float humidity = dht.readHumidity();
  
  if (isnan(temp) || isnan(humidity)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }
  
  StaticJsonDocument<256> doc;
  doc["type"] = "sensor";
  doc["temperature"] = temp;
  doc["humidity"] = humidity;
  doc["soilMoisture"] = readSoilMoisture();
  doc["lightLevel"] = readLightLevel();
  doc["timestamp"] = millis();
  
  char buffer[256];
  serializeJson(doc, buffer);
  webSocket.sendTXT(buffer);
  
  Serial.printf("Temp: %.1f°C, Humidity: %.1f%%\\n", temp, humidity);
}

void sendDeviceStatus() {
  StaticJsonDocument<256> doc;
  doc["type"] = "devices";
  doc["led"] = devices.led;
  doc["fan"] = devices.fan;
  doc["pump"] = devices.pump;
  doc["vent"] = devices.vent;
  
  char buffer[256];
  serializeJson(doc, buffer);
  webSocket.sendTXT(buffer);
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  
  pinMode(LED_RELAY, OUTPUT);
  pinMode(FAN_RELAY, OUTPUT);
  pinMode(PUMP_RELAY, OUTPUT);
  pinMode(VENT_RELAY, OUTPUT);
  
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\\nWiFi Connected!");
  
  webSocket.begin(ws_host, ws_port, "/");
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
}

void loop() {
  webSocket.loop();
  
  if (millis() - lastSensorRead >= SENSOR_INTERVAL) {
    lastSensorRead = millis();
    readSensors();
  }
}`;

export function Settings() {
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState<'general' | 'esp32'>('general');

  const handleCopy = () => {
    navigator.clipboard.writeText(esp32Code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h2 className="text-2xl font-bold text-white flex items-center gap-2">
          <SettingsIcon className="w-6 h-6 text-[#00ccff]" style={{ filter: 'drop-shadow(0 0 4px rgba(0, 204, 255, 0.3))' }} />
          Settings & Configuration
        </h2>
        <p className="text-gray-400 text-sm mt-1">Configure your greenhouse system and IoT hardware</p>
      </motion.div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setActiveTab('general')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition-all ${
            activeTab === 'general' 
              ? 'bg-[rgba(0,255,128,0.1)] text-[#00ff80] border border-[rgba(0,255,128,0.2)]' 
              : 'text-gray-400 hover:text-white'
          }`}
        >
          <Wifi className="w-4 h-4" /> Connection
        </button>
        <button
          onClick={() => setActiveTab('esp32')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition-all ${
            activeTab === 'esp32' 
              ? 'bg-[rgba(0,255,128,0.1)] text-[#00ff80] border border-[rgba(0,255,128,0.2)]' 
              : 'text-gray-400 hover:text-white'
          }`}
        >
          <Cpu className="w-4 h-4" /> ESP32-S3 Code
        </button>
      </div>

      {activeTab === 'general' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-4"
        >
          {/* WiFi Settings */}
          <div className="glass-card p-5">
            <h3 className="text-white font-semibold mb-4 flex items-center gap-2">
              <Wifi className="w-4 h-4 text-[#00ff80]" /> WiFi Configuration
            </h3>
            <div className="grid gap-4">
              <div>
                <label className="text-gray-400 text-xs mb-1 block">Network SSID</label>
                <input 
                  type="text" 
                  defaultValue="GreenHouse_AP"
                  className="w-full px-3 py-2 rounded-lg bg-[rgba(255,255,255,0.05)] border border-[rgba(0,255,128,0.15)] text-white text-sm focus:outline-none focus:border-[rgba(0,255,128,0.4)] transition-colors"
                />
              </div>
              <div>
                <label className="text-gray-400 text-xs mb-1 block">Password</label>
                <input 
                  type="password" 
                  defaultValue="********"
                  className="w-full px-3 py-2 rounded-lg bg-[rgba(255,255,255,0.05)] border border-[rgba(0,255,128,0.15)] text-white text-sm focus:outline-none focus:border-[rgba(0,255,128,0.4)] transition-colors"
                />
              </div>
            </div>
          </div>

          {/* Server Settings */}
          <div className="glass-card p-5">
            <h3 className="text-white font-semibold mb-4 flex items-center gap-2">
              <RefreshCw className="w-4 h-4 text-[#00ccff]" /> WebSocket Server
            </h3>
            <div className="grid gap-4">
              <div>
                <label className="text-gray-400 text-xs mb-1 block">Server Host</label>
                <input 
                  type="text" 
                  defaultValue="greenhouse-2026.render.com"
                  className="w-full px-3 py-2 rounded-lg bg-[rgba(255,255,255,0.05)] border border-[rgba(0,255,128,0.15)] text-white text-sm focus:outline-none focus:border-[rgba(0,255,128,0.4)] transition-colors"
                />
              </div>
              <div>
                <label className="text-gray-400 text-xs mb-1 block">Port</label>
                <input 
                  type="number" 
                  defaultValue="8080"
                  className="w-full px-3 py-2 rounded-lg bg-[rgba(255,255,255,0.05)] border border-[rgba(0,255,128,0.15)] text-white text-sm focus:outline-none focus:border-[rgba(0,255,128,0.4)] transition-colors"
                />
              </div>
            </div>
          </div>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="w-full py-3 rounded-xl flex items-center justify-center gap-2 text-sm font-medium text-white transition-all"
            style={{
              background: 'linear-gradient(135deg, rgba(0, 255, 128, 0.2), rgba(0, 204, 255, 0.2))',
              border: '1px solid rgba(0, 255, 128, 0.3)',
            }}
          >
            <Save className="w-4 h-4" />
            Save Configuration
          </motion.button>
        </motion.div>
      )}

      {activeTab === 'esp32' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass-card p-5"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal className="w-5 h-5 text-[#00ff80]" />
              <h3 className="text-white font-semibold">ESP32-S3 Arduino Code</h3>
            </div>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleCopy}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs transition-all"
              style={{
                background: copied ? 'rgba(0, 255, 128, 0.2)' : 'rgba(255, 255, 255, 0.05)',
                border: `1px solid ${copied ? 'rgba(0, 255, 128, 0.3)' : 'rgba(255, 255, 255, 0.1)'}`,
                color: copied ? '#00ff80' : '#aaa',
              }}
            >
              {copied ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
              {copied ? 'Copied!' : 'Copy Code'}
            </motion.button>
          </div>
          
          <div className="relative">
            <div className="absolute top-0 left-0 right-0 h-8 flex items-center gap-2 px-4 rounded-t-lg" style={{ background: 'rgba(0, 0, 0, 0.3)' }}>
              <div className="w-3 h-3 rounded-full bg-[#ff5f56]" />
              <div className="w-3 h-3 rounded-full bg-[#ffbd2e]" />
              <div className="w-3 h-3 rounded-full bg-[#27c93f]" />
              <span className="text-[10px] text-gray-500 ml-2">greenhouse_esp32.ino</span>
            </div>
            <pre 
              className="pt-10 p-4 rounded-lg overflow-x-auto text-xs font-mono leading-relaxed custom-scrollbar"
              style={{ 
                background: 'rgba(0, 0, 0, 0.5)', 
                border: '1px solid rgba(255, 255, 255, 0.05)',
                maxHeight: '600px',
              }}
            >
              <code className="text-gray-300">{esp32Code}</code>
            </pre>
          </div>
        </motion.div>
      )}
    </div>
  );
}
