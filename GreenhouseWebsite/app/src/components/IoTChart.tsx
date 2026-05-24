import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Area,
  AreaChart
} from 'recharts';
import { Activity, Thermometer, Droplets, Wind, Sun } from 'lucide-react';
import type { SensorData } from '@/hooks/useGreenhouse';
import type { Plant } from '@/data/plantsData';

type MetricKey = 'temperature' | 'soilMoisture' | 'airHumidity' | 'lightLevel';

interface MetricConfig {
  key: MetricKey;
  label: string;
  color: string;
  glowColor: string;
  icon: React.ElementType;
  unit: string;
}

const metrics: MetricConfig[] = [
  { key: 'temperature', label: 'Temperature', color: '#ff6b35', glowColor: 'rgba(255, 107, 53, 0.3)', icon: Thermometer, unit: '°C' },
  { key: 'soilMoisture', label: 'Soil Moisture', color: '#00ccff', glowColor: 'rgba(0, 204, 255, 0.3)', icon: Droplets, unit: '%' },
  { key: 'airHumidity', label: 'Air Humidity', color: '#00ff80', glowColor: 'rgba(0, 255, 128, 0.3)', icon: Wind, unit: '%' },
  { key: 'lightLevel', label: 'Light Level', color: '#ffd000', glowColor: 'rgba(255, 208, 0, 0.3)', icon: Sun, unit: 'lux' },
];

interface CustomTooltipProps {
  active?: boolean;
  payload?: Array<{ value: number; dataKey: string }>;
  label?: number;
  activeMetrics: MetricKey[];
}

function CustomTooltip({ active, payload, label, activeMetrics }: CustomTooltipProps) {
  if (!active || !payload) return null;
  
  return (
    <div 
      className="px-4 py-3 rounded-xl"
      style={{
        background: 'rgba(10, 15, 20, 0.95)',
        border: '1px solid rgba(0, 255, 128, 0.2)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(12px)',
      }}
    >
      <p className="text-gray-400 text-[10px] mb-2 font-mono">
        {label ? new Date(label).toLocaleTimeString() : ''}
      </p>
      <div className="space-y-1">
        {payload.map((entry, i) => {
          const metric = metrics.find(m => m.key === entry.dataKey);
          if (!metric || !activeMetrics.includes(entry.dataKey as MetricKey)) return null;
          const val = entry.dataKey === 'lightLevel' ? (entry.value / 1000).toFixed(1) + 'k' : entry.value.toFixed(1);
          return (
            <div key={i} className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full" style={{ background: metric.color, boxShadow: `0 0 6px ${metric.glowColor}` }} />
              <span className="text-gray-400 text-xs">{metric.label}:</span>
              <span className="text-white text-xs font-mono font-semibold">{val} {metric.unit}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

interface IoTChartProps {
  sensorHistory: SensorData[];
  selectedPlant: Plant;
}

export function IoTChart({ sensorHistory, selectedPlant }: IoTChartProps) {
  const [activeMetrics, setActiveMetrics] = useState<MetricKey[]>(['temperature', 'soilMoisture', 'airHumidity']);
  const [chartType, setChartType] = useState<'line' | 'area'>('area');

  const toggleMetric = (key: MetricKey) => {
    setActiveMetrics(prev => 
      prev.includes(key) ? prev.filter(m => m !== key) : [...prev, key]
    );
  };

  // Format data for chart
  const chartData = sensorHistory.map(d => ({
    ...d,
    lightLevel: d.lightLevel / 1000, // Convert to kLux for display
  }));

  const thresholds = selectedPlant.thresholds;

  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="glass-card p-5"
    >
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-5">
        <div className="flex items-center gap-2">
          <Activity className="w-5 h-5 text-[#00ff80]" style={{ filter: 'drop-shadow(0 0 4px rgba(0, 255, 128, 0.3))' }} />
          <h3 className="text-white font-semibold">Real-time Sensor Data</h3>
          <span className="text-[10px] text-gray-500 ml-2 font-mono">{sensorHistory.length} points</span>
        </div>
        
        <div className="flex items-center gap-2">
          {/* Chart type toggle */}
          <div className="flex rounded-lg overflow-hidden border border-[rgba(0,255,128,0.2)]">
            <button
              onClick={() => setChartType('line')}
              className={`px-3 py-1 text-xs transition-all ${chartType === 'line' ? 'bg-[rgba(0,255,128,0.2)] text-[#00ff80]' : 'text-gray-400 hover:text-white'}`}
            >
              Line
            </button>
            <button
              onClick={() => setChartType('area')}
              className={`px-3 py-1 text-xs transition-all ${chartType === 'area' ? 'bg-[rgba(0,255,128,0.2)] text-[#00ff80]' : 'text-gray-400 hover:text-white'}`}
            >
              Area
            </button>
          </div>
        </div>
      </div>

      {/* Metric Toggles */}
      <div className="flex flex-wrap gap-2 mb-5">
        {metrics.map(metric => {
          const isActive = activeMetrics.includes(metric.key);
          const Icon = metric.icon;
          return (
            <motion.button
              key={metric.key}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => toggleMetric(metric.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs transition-all ${
                isActive 
                  ? 'text-white border' 
                  : 'text-gray-500 bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)] hover:text-gray-300'
              }`}
              style={isActive ? {
                background: `${metric.glowColor}`,
                borderColor: `${metric.color}40`,
              } : {}}
            >
              <Icon className="w-3.5 h-3.5" style={{ color: isActive ? metric.color : 'currentColor' }} />
              {metric.label}
            </motion.button>
          );
        })}
      </div>

      {/* Chart */}
      <div className="h-[350px] w-full">
        <ResponsiveContainer width="100%" height="100%">
          {chartType === 'area' ? (
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <defs>
                {metrics.map(m => (
                  <linearGradient key={m.key} id={`gradient-${m.key}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={m.color} stopOpacity={0.3} />
                    <stop offset="95%" stopColor={m.color} stopOpacity={0} />
                  </linearGradient>
                ))}
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0, 255, 128, 0.05)" />
              <XAxis 
                dataKey="timestamp" 
                tickFormatter={(v) => new Date(v).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                stroke="rgba(255, 255, 255, 0.1)"
                tick={{ fill: 'rgba(255, 255, 255, 0.4)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              />
              <YAxis 
                stroke="rgba(255, 255, 255, 0.1)"
                tick={{ fill: 'rgba(255, 255, 255, 0.4)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              />
              <Tooltip content={<CustomTooltip activeMetrics={activeMetrics} />} />
              {metrics.map(m => activeMetrics.includes(m.key) && (
                <Area
                  key={m.key}
                  type="monotone"
                  dataKey={m.key === 'lightLevel' ? 'lightLevel' : m.key}
                  stroke={m.color}
                  fill={`url(#gradient-${m.key})`}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ 
                    r: 4, 
                    fill: m.color, 
                    stroke: 'rgba(10, 15, 20, 0.8)', 
                    strokeWidth: 2,
                  }}
                  style={{
                    filter: `drop-shadow(0 0 6px ${m.glowColor})`,
                  }}
                />
              ))}
            </AreaChart>
          ) : (
            <LineChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(0, 255, 128, 0.05)" />
              <XAxis 
                dataKey="timestamp" 
                tickFormatter={(v) => new Date(v).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                stroke="rgba(255, 255, 255, 0.1)"
                tick={{ fill: 'rgba(255, 255, 255, 0.4)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              />
              <YAxis 
                stroke="rgba(255, 255, 255, 0.1)"
                tick={{ fill: 'rgba(255, 255, 255, 0.4)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              />
              <Tooltip content={<CustomTooltip activeMetrics={activeMetrics} />} />
              {metrics.map(m => activeMetrics.includes(m.key) && (
                <Line
                  key={m.key}
                  type="monotone"
                  dataKey={m.key === 'lightLevel' ? 'lightLevel' : m.key}
                  stroke={m.color}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ 
                    r: 4, 
                    fill: m.color, 
                    stroke: 'rgba(10, 15, 20, 0.8)', 
                    strokeWidth: 2,
                  }}
                  style={{
                    filter: `drop-shadow(0 0 6px ${m.glowColor})`,
                  }}
                />
              ))}
            </LineChart>
          )}
        </ResponsiveContainer>
      </div>

      {/* Threshold Indicators */}
      <div className="mt-4 flex flex-wrap gap-3 text-[10px] text-gray-500">
        <span>Optimal ranges for {selectedPlant.name}:</span>
        <span className="flex items-center gap-1">
          <div className="w-1.5 h-1.5 rounded-full bg-[#ff6b35]" />
          {thresholds.tempOptimal}°C
        </span>
        <span className="flex items-center gap-1">
          <div className="w-1.5 h-1.5 rounded-full bg-[#00ccff]" />
          {thresholds.soilMoistureOptimal}%
        </span>
        <span className="flex items-center gap-1">
          <div className="w-1.5 h-1.5 rounded-full bg-[#00ff80]" />
          {thresholds.airHumidityOptimal}%
        </span>
      </div>
    </motion.div>
  );
}
