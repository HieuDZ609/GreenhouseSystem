# GreenHouse Smart 2026 - Deployment Guide

## Project Architecture

```
GreenHouse 2026/
├── app/                    # React 19 Frontend
│   ├── src/
│   │   ├── components/     # UI Components
│   │   ├── data/           # Plant data & expert profiles
│   │   ├── hooks/          # Custom React hooks
│   │   └── App.tsx         # Main application
│   └── dist/               # Production build
├── server/                 # Node.js Backend
│   ├── server.js           # Express + Socket.io
│   ├── package.json
│   └── .env.example
└── esp32/                  # ESP32-S3 Firmware
    └── greenhouse_esp32.ino
```

---

## Phase 1: Deploy Frontend (Vercel)

### 1.1 Build the Frontend

```bash
cd app
npm install
npm run build
```

### 1.2 Deploy to Vercel

Option A: Using Vercel CLI
```bash
npm i -g vercel
cd app/dist
vercel --prod
```

Option B: GitHub Integration
1. Push code to GitHub repository
2. Connect repository on [vercel.com](https://vercel.com)
3. Set build command: `npm run build`
4. Set output directory: `dist`
5. Deploy!

### 1.3 Update Frontend API URL

In `src/hooks/useGreenhouse.ts`, update the WebSocket URL:
```typescript
const WS_URL = 'wss://your-server-url.render.com';
```

---

## Phase 2: Deploy Backend (Render)

### 2.1 Create Web Service on Render

1. Go to [render.com](https://render.com) and create account
2. Click "New" -> "Web Service"
3. Connect your GitHub repository or upload files
4. Configure:
   - **Name**: `greenhouse-server`
   - **Runtime**: `Node`
   - **Build Command**: `cd server && npm install`
   - **Start Command**: `cd server && npm start`
   - **Plan**: Free tier

5. Add Environment Variables:
   ```
   PORT=8080
   FRONTEND_URL=https://your-vercel-url.vercel.app
   ```

6. Click "Create Web Service"

### 2.2 Alternative: Deploy using Render Blueprint

Create `render.yaml` in project root:
```yaml
services:
  - type: web
    name: greenhouse-server
    runtime: node
    plan: free
    buildCommand: cd server && npm install
    startCommand: cd server && npm start
    envVars:
      - key: PORT
        value: 8080
      - key: FRONTEND_URL
        value: https://your-frontend-url.vercel.app
```

---

## Phase 3: ESP32-S3 Setup

### 3.1 Hardware Requirements

| Component | Specification | Pin |
|-----------|--------------|-----|
| Microcontroller | ESP32-S3 DevKitC | - |
| Temp/Humidity Sensor | DHT22 | GPIO 4 |
| Soil Moisture Sensor | Capacitive v1.2 | GPIO 34 (ADC) |
| Light Sensor | BH1750 I2C | SDA: GPIO 8, SCL: GPIO 9 |
| LED Relay | 5V Relay Module | GPIO 12 |
| Fan Relay | 5V Relay Module | GPIO 14 |
| Pump Relay | 5V Relay Module | GPIO 27 |
| Vent Relay | 5V Relay Module | GPIO 26 |

### 3.2 Wiring Diagram

```
ESP32-S3          DHT22          BH1750         Relay Module
--------          -----          ------         ------------
3.3V     -------> VCC           VCC
GND      -------> GND           GND
GPIO 4   -------> DATA
GPIO 8   ----------------------> SDA
GPIO 9   ----------------------> SCL
GPIO 12  -------------------------------------> LED Relay
GPIO 14  -------------------------------------> FAN Relay
GPIO 27  -------------------------------------> PUMP Relay
GPIO 26  -------------------------------------> VENT Relay
```

### 3.3 Arduino IDE Setup

1. Install ESP32 board support:
   - File > Preferences > Additional Board Manager URLs:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
   - Tools > Board > Boards Manager > Search "ESP32" > Install

2. Install required libraries:
   ```
   Sketch > Include Library > Manage Libraries
   - DHT sensor library by Adafruit
   - Adafruit Unified Sensor
   - WebSockets by Markus Sattler
   - ArduinoJSON by Benoit Blanchon
   ```

3. Select board:
   ```
   Tools > Board > ESP32 Arduino > ESP32S3 Dev Module
   Tools > Port > Select your COM port
   ```

### 3.4 Upload Firmware

1. Open `esp32/greenhouse_esp32.ino` in Arduino IDE
2. Update WiFi credentials:
   ```cpp
   const char* ssid = "YOUR_WIFI_SSID";
   const char* password = "YOUR_WIFI_PASSWORD";
   ```
3. Update server URL:
   ```cpp
   const char* ws_host = "your-server.onrender.com";
   const int ws_port = 8080;
   ```
4. Click Upload (Ctrl+U)
5. Open Serial Monitor (Tools > Serial Monitor) to verify connection

---

## Phase 4: System Integration Test

### 4.1 Verify Backend

```bash
# Test API endpoints
curl https://your-server.render.com/api/sensors
curl https://your-server.render.com/api/devices

# Test device control
curl -X POST https://your-server.render.com/api/devices/led \
  -H "Content-Type: application/json" \
  -d '{"state": true}'
```

### 4.2 Verify ESP32 Connection

1. Check Serial Monitor for:
   ```
   WiFi Connected!
   WebSocket connected!
   ```

2. Check Render logs for:
   ```
   ESP32 registered: [socket-id]
   ```

### 4.3 Verify Frontend

1. Open deployed frontend URL
2. Check "System Online" indicator in sidebar
3. IoT Control tab should show real-time data
4. Toggle switches should control ESP32 relays

---

## API Reference

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check |
| GET | `/api/sensors` | Current sensor data |
| GET | `/api/sensors/history?limit=100` | Sensor history |
| GET | `/api/devices` | Device states |
| POST | `/api/devices/:device` | Control device |
| GET | `/api/status` | System status |

### WebSocket Events

**Client -> Server:**
| Event | Data | Description |
|-------|------|-------------|
| `controlDevice` | `{ device, state }` | Control a device |
| `sensorData` | `{ temperature, humidity, ... }` | Send sensor data |
| `esp32_register` | - | Register ESP32 |
| `heartbeat` | - | Keep alive |

**Server -> Client:**
| Event | Data | Description |
|-------|------|-------------|
| `sensorData` | Sensor object | Real-time sensor update |
| `deviceState` | Device states | Current device states |
| `deviceUpdate` | `{ device, state }` | Device state change |
| `esp32Status` | `{ connected }` | ESP32 connection status |

---

## Troubleshooting

### ESP32 won't connect to WiFi
- Check WiFi credentials (case-sensitive)
- Ensure 2.4GHz network (ESP32 doesn't support 5GHz)
- Try closer to router

### WebSocket connection fails
- Verify server URL and port
- Check CORS settings match frontend URL
- Test with `wscat -c ws://your-server:8080`

### Sensor readings incorrect
- Verify DHT22 wiring (10k pull-up resistor on DATA)
- Check sensor power supply (3.3V stable)
- Calibrate soil moisture sensor in dry/wet conditions

### Frontend not updating
- Check browser console for WebSocket errors
- Verify `WS_URL` in useGreenhouse.ts matches server
- Check Network tab for blocked requests

---

## Free Tier Limits

| Service | Limit |
|---------|-------|
| Render Web Service | 512 MB RAM, sleeps after 15 min idle |
| Vercel | 100 GB bandwidth/month |
| ESP32-S3 | 2.4GHz WiFi, ~50m range |

---

## Security Recommendations

1. **Authentication**: Add JWT tokens for API access
2. **HTTPS**: Always use WSS (Secure WebSocket) in production
3. **Rate Limiting**: Add express-rate-limit to prevent abuse
4. **Input Validation**: Validate all sensor data ranges
5. **API Keys**: Use environment variables for secrets

---

## Next Steps

1. [ ] Add database (MongoDB/PostgreSQL) for historical data
2. [ ] Implement user authentication
3. [ ] Add camera module for visual monitoring
4. [ ] Machine learning for disease detection
5. [ ] Mobile app with React Native
6. [ ] Multi-greenhouse support

---

## Support

- **Frontend**: React 19 + TypeScript + Tailwind CSS + Framer Motion
- **Backend**: Node.js + Express + Socket.io
- **Hardware**: ESP32-S3 + DHT22 + Relay Module
- **Deploy**: Vercel (Frontend) + Render (Backend)

---

**Built with by GreenHouse Smart Swarm 2026**
