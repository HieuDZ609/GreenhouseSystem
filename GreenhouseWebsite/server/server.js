/**
 * GreenHouse Smart 2026 - IoT Backend Server
 * Features: Express REST API, Socket.io WebSocket, ESP32-S3 Integration
 * Deploy: Render.com, Railway, or VPS
 */

const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
require('dotenv').config();

const app = express();
const httpServer = createServer(app);

// CORS configuration - allow your frontend domain
const io = new Server(httpServer, {
  cors: {
    origin: process.env.FRONTEND_URL || "*",
    methods: ["GET", "POST"],
    credentials: true
  }
});

app.use(cors());
app.use(express.json());

// ===== STATE MANAGEMENT =====
const state = {
  devices: {
    led: false,
    fan: false,
    pump: false,
    vent: false
  },
  sensors: {
    temperature: 22.5,
    humidity: 65,
    soilMoisture: 70,
    lightLevel: 35000,
    co2Level: 420,
    timestamp: Date.now()
  },
  history: [],
  esp32Connected: false,
  lastEsp32Ping: null
};

const MAX_HISTORY = 1000;

// Keep last 50 readings in memory
function addSensorReading(data) {
  const reading = {
    timestamp: Date.now(),
    temperature: data.temperature || state.sensors.temperature,
    soilMoisture: data.soilMoisture || data.humidity || state.sensors.soilMoisture,
    airHumidity: data.humidity || state.sensors.humidity,
    lightLevel: data.lightLevel || state.sensors.lightLevel,
    co2Level: data.co2Level || state.sensors.co2Level
  };
  
  state.sensors = reading;
  state.history.push(reading);
  
  if (state.history.length > MAX_HISTORY) {
    state.history = state.history.slice(-MAX_HISTORY);
  }
  
  return reading;
}

// ===== REST API ROUTES =====

// Health check
app.get('/', (req, res) => {
  res.json({
    status: 'online',
    name: 'GreenHouse Smart 2026 API',
    version: '2.0.0',
    esp32Connected: state.esp32Connected,
    uptime: process.uptime()
  });
});

// Get current sensor data
app.get('/api/sensors', (req, res) => {
  res.json(state.sensors);
});

// Get sensor history
app.get('/api/sensors/history', (req, res) => {
  const limit = parseInt(req.query.limit) || 100;
  res.json(state.history.slice(-limit));
});

// Get device states
app.get('/api/devices', (req, res) => {
  res.json(state.devices);
});

// Control device via REST
app.post('/api/devices/:device', (req, res) => {
  const { device } = req.params;
  const { state: deviceState } = req.body;
  
  if (!['led', 'fan', 'pump', 'vent'].includes(device)) {
    return res.status(400).json({ error: 'Unknown device' });
  }
  
  state.devices[device] = !!deviceState;
  
  // Notify all WebSocket clients
  io.emit('deviceUpdate', { device, state: state.devices[device] });
  
  // Notify ESP32 if connected
  io.to('esp32').emit('control', { device, state: state.devices[device] });
  
  res.json({ success: true, devices: state.devices });
});

// Get ESP32 status
app.get('/api/status', (req, res) => {
  res.json({
    esp32Connected: state.esp32Connected,
    lastPing: state.lastEsp32Ping,
    devices: state.devices,
    sensorCount: state.history.length
  });
});

// ===== SOCKET.IO EVENTS =====

io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);
  
  // Send current state to new client
  socket.emit('sensorData', state.sensors);
  socket.emit('deviceState', state.devices);
  
  // Handle device control from web client
  socket.on('controlDevice', ({ device, state: deviceState }) => {
    if (!['led', 'fan', 'pump', 'vent'].includes(device)) return;
    
    state.devices[device] = deviceState;
    
    // Broadcast to all clients
    io.emit('deviceUpdate', { device, state: deviceState });
    
    // Send to ESP32
    io.to('esp32').emit('control', { device, state: deviceState });
    
    console.log(`Device ${device} set to ${deviceState}`);
  });
  
  // Handle ESP32 sensor data
  socket.on('sensorData', (data) => {
    const reading = addSensorReading(data);
    
    // Broadcast to all web clients
    socket.broadcast.emit('sensorData', reading);
    
    console.log('Sensor data received:', {
      temp: reading.temperature.toFixed(1),
      humidity: reading.airHumidity.toFixed(0),
      soil: reading.soilMoisture.toFixed(0)
    });
  });
  
  // ESP32 registration
  socket.on('esp32_register', () => {
    socket.join('esp32');
    state.esp32Connected = true;
    state.lastEsp32Ping = Date.now();
    
    // Send current device states to ESP32
    socket.emit('deviceState', state.devices);
    
    console.log('ESP32 registered:', socket.id);
    io.emit('esp32Status', { connected: true });
  });
  
  // ESP32 heartbeat
  socket.on('heartbeat', () => {
    state.lastEsp32Ping = Date.now();
    if (!state.esp32Connected) {
      state.esp32Connected = true;
      io.emit('esp32Status', { connected: true });
    }
  });
  
  // Device status update from ESP32
  socket.on('deviceStatus', (devices) => {
    state.devices = { ...state.devices, ...devices };
    io.emit('deviceState', state.devices);
  });
  
  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
    
    // Check if ESP32 disconnected
    if (socket.rooms.has('esp32')) {
      state.esp32Connected = false;
      io.emit('esp32Status', { connected: false });
      console.log('ESP32 disconnected');
    }
  });
});

// Check ESP32 connection every 30 seconds
setInterval(() => {
  if (state.lastEsp32Ping && Date.now() - state.lastEsp32Ping > 60000) {
    state.esp32Connected = false;
    io.emit('esp32Status', { connected: false });
  }
}, 30000);

// ===== START SERVER =====
const PORT = process.env.PORT || 8080;

httpServer.listen(PORT, () => {
  console.log(`
  ╔══════════════════════════════════════════╗
  ║     GreenHouse Smart 2026 Server         ║
  ║                                          ║
  ║   WebSocket: ws://localhost:${PORT}        ║
  ║   REST API:  http://localhost:${PORT}/api  ║
  ╚══════════════════════════════════════════╝
  `);
});

module.exports = { app, io, state };
