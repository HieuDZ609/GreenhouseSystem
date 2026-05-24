import { motion } from 'framer-motion';
import { 
  Thermometer, 
  Droplets, 
  Wind, 
  Sun, 
  AlertTriangle, 
  CheckCircle2,
  TrendingUp,
  Activity,
  Zap,
  Clock
} from 'lucide-react';
import type { SensorData, DeviceState } from '@/hooks/useGreenhouse';
import type { Plant } from '@/data/plantsData';

interface DashboardHeroProps {
  latestReading: SensorData | null;
  selectedPlant: Plant;
  insights: { message: string; severity: 'optimal' | 'warning' | 'critical' }[];
  devices: DeviceState;
}

function StatCard({ 
  icon: Icon, 
  label, 
  value, 
  unit, 
  color, 
  delay,
  status
}: { 
  icon: React.ElementType; 
  label: string; 
  value: string | number; 
  unit: string; 
  color: string;
  delay: number;
  status: 'optimal' | 'warning' | 'critical';
}) {
  const statusColors = {
    optimal: { bg: 'rgba(0, 255, 128, 0.1)', border: 'rgba(0, 255, 128, 0.2)', glow: 'rgba(0, 255, 128, 0.1)', text: '#00ff80' },
    warning: { bg: 'rgba(255, 200, 0, 0.1)', border: 'rgba(255, 200, 0, 0.2)', glow: 'rgba(255, 200, 0, 0.1)', text: '#ffd000' },
    critical: { bg: 'rgba(255, 50, 50, 0.1)', border: 'rgba(255, 50, 50, 0.2)', glow: 'rgba(255, 50, 50, 0.1)', text: '#ff4444' },
  };
  
  const s = statusColors[status];
  void color; // Used for icon color theming
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.5, ease: 'easeOut' }}
      whileHover={{ y: -4, transition: { duration: 0.2 } }}
      className="glass-card p-5 cursor-default"
      style={{ borderColor: s.border }}
    >
      <div className="flex items-center justify-between mb-3">
        <div 
          className="w-10 h-10 rounded-lg flex items-center justify-center"
          style={{ background: s.bg, border: `1px solid ${s.border}` }}
        >
          <Icon className="w-5 h-5" style={{ color: s.text, filter: `drop-shadow(0 0 4px ${s.glow})` }} />
        </div>
        <div className={`flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full ${
          status === 'optimal' ? 'status-optimal' : status === 'warning' ? 'status-warning' : 'status-critical'
        }`}>
          {status === 'optimal' ? <CheckCircle2 className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
          {status.toUpperCase()}
        </div>
      </div>
      <div className="space-y-1">
        <p className="text-gray-400 text-xs">{label}</p>
        <p className="text-2xl font-bold text-white font-mono">
          {value}
          <span className="text-sm text-gray-500 ml-1">{unit}</span>
        </p>
      </div>
      {/* Mini sparkline */}
      <div className="mt-3 h-8 flex items-end gap-0.5">
        {Array.from({ length: 12 }).map((_, i) => (
          <motion.div
            key={i}
            className="flex-1 rounded-t-sm"
            style={{ background: s.text, opacity: 0.3 + (i / 12) * 0.7 }}
            initial={{ height: 0 }}
            animate={{ height: `${20 + Math.random() * 60}%` }}
            transition={{ delay: delay + i * 0.05, duration: 0.5 }}
          />
        ))}
      </div>
    </motion.div>
  );
}

function DeviceStatus({ 
  label, 
  active, 
  icon: Icon, 
  delay 
}: { 
  label: string; 
  active: boolean; 
  icon: React.ElementType; 
  delay: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay, duration: 0.4 }}
      className={`flex items-center gap-2 px-3 py-2 rounded-lg ${
        active 
          ? 'bg-[rgba(0,255,128,0.1)] border border-[rgba(0,255,128,0.3)]' 
          : 'bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)]'
      }`}
    >
      <Icon 
        className={`w-4 h-4 ${active ? 'text-[#00ff80]' : 'text-gray-500'}`}
        style={active ? { filter: 'drop-shadow(0 0 4px rgba(0, 255, 128, 0.5))' } : {}}
      />
      <span className={`text-xs font-medium ${active ? 'text-[#00ff80]' : 'text-gray-500'}`}>
        {label}
      </span>
      <div 
        className={`w-1.5 h-1.5 rounded-full ml-auto ${active ? 'bg-[#00ff80]' : 'bg-gray-600'}`}
        style={active ? { boxShadow: '0 0 6px rgba(0, 255, 128, 0.5)' } : {}}
      />
    </motion.div>
  );
}

export function DashboardHero({ latestReading, selectedPlant, insights, devices }: DashboardHeroProps) {
  if (!latestReading) {
    return (
      <div className="flex items-center justify-center h-64">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
          className="w-8 h-8 border-2 border-[#00ff80] border-t-transparent rounded-full"
        />
      </div>
    );
  }

  const t = selectedPlant.thresholds;
  
  const tempStatus = latestReading.temperature > t.tempMax || latestReading.temperature < t.tempMin ? 'critical' : 'optimal';
  const moistureStatus = latestReading.soilMoisture > t.soilMoistureMax || latestReading.soilMoisture < t.soilMoistureMin ? 'warning' : 'optimal';
  const humidityStatus = latestReading.airHumidity > t.airHumidityMax || latestReading.airHumidity < t.airHumidityMin ? 'warning' : 'optimal';

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between"
      >
        <div>
          <h2 className="text-3xl font-bold text-white">
            Dashboard
            <span className="text-[#00ff80] ml-2" style={{ textShadow: '0 0 20px rgba(0, 255, 128, 0.3)' }}>
              {selectedPlant.name}
            </span>
          </h2>
          <p className="text-gray-400 text-sm mt-1">Real-time monitoring & intelligent insights</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[rgba(0,255,128,0.08)] border border-[rgba(0,255,128,0.15)]">
            <Activity className="w-4 h-4 text-[#00ff80]" />
            <span className="text-xs text-[#00ff80] font-medium">LIVE</span>
          </div>
          <div className="flex items-center gap-2 text-gray-500 text-xs">
            <Clock className="w-3.5 h-3.5" />
            <span className="font-mono">{new Date(latestReading.timestamp).toLocaleTimeString()}</span>
          </div>
        </div>
      </motion.div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={Thermometer}
          label="Temperature"
          value={latestReading.temperature.toFixed(1)}
          unit="°C"
          color="#ff6b35"
          delay={0}
          status={tempStatus}
        />
        <StatCard
          icon={Droplets}
          label="Soil Moisture"
          value={latestReading.soilMoisture.toFixed(0)}
          unit="%"
          color="#00ccff"
          delay={0.1}
          status={moistureStatus}
        />
        <StatCard
          icon={Wind}
          label="Air Humidity"
          value={latestReading.airHumidity.toFixed(0)}
          unit="%"
          color="#00ff80"
          delay={0.2}
          status={humidityStatus}
        />
        <StatCard
          icon={Sun}
          label="Light Level"
          value={(latestReading.lightLevel / 1000).toFixed(1)}
          unit="kLux"
          color="#ffd000"
          delay={0.3}
          status="optimal"
        />
      </div>

      {/* Insights & Device Status Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Actionable Insights */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="lg:col-span-2 glass-card p-5"
        >
          <div className="flex items-center gap-2 mb-4">
            <Zap className="w-5 h-5 text-[#00ff80]" style={{ filter: 'drop-shadow(0 0 4px rgba(0, 255, 128, 0.3))' }} />
            <h3 className="text-white font-semibold">Actionable Insights</h3>
            <TrendingUp className="w-4 h-4 text-gray-500 ml-auto" />
          </div>
          <div className="space-y-2">
            {insights.map((insight, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 + i * 0.1 }}
                className={`flex items-center gap-3 p-3 rounded-lg ${
                  insight.severity === 'optimal' 
                    ? 'bg-[rgba(0,255,128,0.05)] border border-[rgba(0,255,128,0.1)]' 
                    : insight.severity === 'warning'
                    ? 'bg-[rgba(255,200,0,0.05)] border border-[rgba(255,200,0,0.1)]'
                    : 'bg-[rgba(255,50,50,0.05)] border border-[rgba(255,50,50,0.1)]'
                }`}
              >
                {insight.severity === 'optimal' ? (
                  <CheckCircle2 className="w-4 h-4 text-[#00ff80] flex-shrink-0" />
                ) : (
                  <AlertTriangle className={`w-4 h-4 flex-shrink-0 ${
                    insight.severity === 'warning' ? 'text-[#ffd000]' : 'text-[#ff4444]'
                  }`} />
                )}
                <span className={`text-sm ${
                  insight.severity === 'optimal' ? 'text-gray-300' : 'text-white'
                }`}>
                  {insight.message}
                </span>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Device Status */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="glass-card p-5"
        >
          <h3 className="text-white font-semibold mb-4">Device Status</h3>
          <div className="space-y-2">
            <DeviceStatus label="LED Grow Light" active={devices.led} icon={Sun} delay={0.6} />
            <DeviceStatus label="Ventilation Fan" active={devices.fan} icon={Wind} delay={0.65} />
            <DeviceStatus label="Water Pump" active={devices.pump} icon={Droplets} delay={0.7} />
            <DeviceStatus label="Ventilation" active={devices.vent} icon={Activity} delay={0.75} />
          </div>
        </motion.div>
      </div>
    </div>
  );
}
