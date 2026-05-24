import { useState, useCallback, useEffect, useRef } from 'react';
import { plants, type Plant } from '@/data/plantsData';

export interface SensorData {
  timestamp: number;
  temperature: number;
  soilMoisture: number;
  airHumidity: number;
  lightLevel: number;
  co2Level: number;
}

export interface DeviceState {
  led: boolean;
  fan: boolean;
  pump: boolean;
  vent: boolean;
}

export type ViewType = 'dashboard' | 'plants' | 'iot' | 'experts' | 'settings';

// Generate realistic mock sensor data based on plant requirements
function generateSensorData(plant: Plant): SensorData {
  const now = Date.now();
  const t = now / 1000;
  const optimal = plant.thresholds;
  
  return {
    timestamp: now,
    temperature: optimal.tempOptimal + Math.sin(t * 0.1) * 2 + (Math.random() - 0.5) * 1.5,
    soilMoisture: optimal.soilMoistureOptimal + Math.sin(t * 0.05) * 5 + (Math.random() - 0.5) * 3,
    airHumidity: optimal.airHumidityOptimal + Math.cos(t * 0.08) * 3 + (Math.random() - 0.5) * 2,
    lightLevel: optimal.lightOptimal * (0.7 + Math.sin(t * 0.02) * 0.3) + (Math.random() - 0.5) * 2000,
    co2Level: 400 + Math.sin(t * 0.03) * 50 + (Math.random() - 0.5) * 30,
  };
}

// Generate historical data for charts
function generateHistoricalData(plant: Plant, points: number = 50): SensorData[] {
  const data: SensorData[] = [];
  const now = Date.now();
  const optimal = plant.thresholds;
  
  for (let i = points; i >= 0; i--) {
    const t = (now - i * 5000) / 1000;
    data.push({
      timestamp: now - i * 5000,
      temperature: optimal.tempOptimal + Math.sin(t * 0.1) * 2.5 + (Math.random() - 0.5) * 2,
      soilMoisture: optimal.soilMoistureOptimal + Math.sin(t * 0.05) * 8 + (Math.random() - 0.5) * 5,
      airHumidity: optimal.airHumidityOptimal + Math.cos(t * 0.08) * 5 + (Math.random() - 0.5) * 3,
      lightLevel: optimal.lightOptimal * (0.6 + Math.sin(t * 0.02) * 0.4) + (Math.random() - 0.5) * 3000,
      co2Level: 400 + Math.sin(t * 0.03) * 60 + (Math.random() - 0.5) * 40,
    });
  }
  return data;
}

// Get actionable insights based on current sensor data
function getInsights(plant: Plant, data: SensorData) {
  const insights: { message: string; severity: 'optimal' | 'warning' | 'critical' }[] = [];
  const t = plant.thresholds;
  
  if (data.temperature > t.tempMax) insights.push({ message: `Temperature HIGH (${data.temperature.toFixed(1)}°C > ${t.tempMax}°C) - Turn ON fan!`, severity: 'critical' });
  else if (data.temperature < t.tempMin) insights.push({ message: `Temperature LOW (${data.temperature.toFixed(1)}°C < ${t.tempMin}°C) - Activate heating!`, severity: 'critical' });
  else insights.push({ message: `Temperature optimal (${data.temperature.toFixed(1)}°C)`, severity: 'optimal' });
  
  if (data.soilMoisture < t.soilMoistureMin) insights.push({ message: `Soil moisture LOW (${data.soilMoisture.toFixed(0)}% < ${t.soilMoistureMin}%) - Water now!`, severity: 'warning' });
  else if (data.soilMoisture > t.soilMoistureMax) insights.push({ message: `Soil moisture HIGH (${data.soilMoisture.toFixed(0)}% > ${t.soilMoistureMax}%) - Risk of root rot!`, severity: 'critical' });
  else insights.push({ message: `Soil moisture optimal (${data.soilMoisture.toFixed(0)}%)`, severity: 'optimal' });
  
  if (data.airHumidity < t.airHumidityMin) insights.push({ message: `Humidity LOW (${data.airHumidity.toFixed(0)}% < ${t.airHumidityMin}%) - Mist plants!`, severity: 'warning' });
  else if (data.airHumidity > t.airHumidityMax) insights.push({ message: `Humidity HIGH (${data.airHumidity.toFixed(0)}% > ${t.airHumidityMax}%) - Increase ventilation!`, severity: 'warning' });
  else insights.push({ message: `Air humidity optimal (${data.airHumidity.toFixed(0)}%)`, severity: 'optimal' });
  
  return insights;
}

export function useGreenhouse() {
  const [currentView, setCurrentView] = useState<ViewType>('dashboard');
  const [selectedPlantId, setSelectedPlantId] = useState<string>('chili');
  const [devices, setDevices] = useState<DeviceState>({ led: false, fan: false, pump: false, vent: false });
  const [sensorHistory, setSensorHistory] = useState<SensorData[]>([]);
  const [latestReading, setLatestReading] = useState<SensorData | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const selectedPlant = plants.find(p => p.id === selectedPlantId) || plants[0];

  // Initialize historical data
  useEffect(() => {
    const history = generateHistoricalData(selectedPlant);
    setSensorHistory(history);
    setLatestReading(history[history.length - 1]);
  }, [selectedPlantId]);

  // Start sensor simulation
  useEffect(() => {
    if (intervalRef.current) clearInterval(intervalRef.current);
    
    intervalRef.current = setInterval(() => {
      const newData = generateSensorData(selectedPlant);
      setLatestReading(newData);
      setSensorHistory(prev => {
        const updated = [...prev.slice(-49), newData];
        return updated;
      });
    }, 5000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [selectedPlantId, selectedPlant]);

  const toggleDevice = useCallback((device: keyof DeviceState) => {
    setDevices(prev => ({ ...prev, [device]: !prev[device] }));
  }, []);

  const insights = latestReading ? getInsights(selectedPlant, latestReading) : [];

  return {
    currentView,
    setCurrentView,
    selectedPlantId,
    setSelectedPlantId,
    selectedPlant,
    devices,
    toggleDevice,
    sensorHistory,
    latestReading,
    insights,
    plants,
  };
}
