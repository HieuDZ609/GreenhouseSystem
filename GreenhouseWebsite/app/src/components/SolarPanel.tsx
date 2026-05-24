import { motion } from 'framer-motion';
import { 
  Sun, 
  Battery, 
  BatteryCharging, 
  Zap, 
  TrendingUp, 
  TrendingDown,
  Gauge,
  AlertTriangle
} from 'lucide-react';
import { solarData } from '@/data/plantsData';

export function SolarPanel() {
  const data = solarData;
  const netProduction = data.dailyGeneration - data.dailyConsumption;
  const isPositive = netProduction > 0;

  const metrics = [
    {
      label: 'Panel Voltage',
      value: `${data.panelVoltage}V`,
      icon: Zap,
      color: '#ffd000',
      glowColor: 'rgba(255, 208, 0, 0.2)',
    },
    {
      label: 'Panel Current',
      value: `${data.panelCurrent}A`,
      icon: Gauge,
      color: '#00ccff',
      glowColor: 'rgba(0, 204, 255, 0.2)',
    },
    {
      label: 'Battery Voltage',
      value: `${data.batteryVoltage}V`,
      icon: Battery,
      color: '#00ff80',
      glowColor: 'rgba(0, 255, 128, 0.2)',
    },
    {
      label: 'Efficiency',
      value: `${data.efficiency}%`,
      icon: Sun,
      color: '#ff8833',
      glowColor: 'rgba(255, 136, 51, 0.2)',
    },
  ];

  const batteryColor = data.batteryPercentage > 60 ? '#00ff80' : data.batteryPercentage > 30 ? '#ffd000' : '#ff4444';

  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="glass-card p-5"
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-2">
          <Sun className="w-5 h-5 text-[#ffd000]" style={{ filter: 'drop-shadow(0 0 6px rgba(255, 208, 0, 0.4))' }} />
          <h3 className="text-white font-semibold">Solar Power System</h3>
        </div>
        <div className="flex items-center gap-2 px-3 py-1 rounded-lg bg-[rgba(0,255,128,0.08)] border border-[rgba(0,255,128,0.15)]">
          <div className="w-2 h-2 rounded-full bg-[#00ff80] animate-pulse" style={{ boxShadow: '0 0 8px rgba(0, 255, 128, 0.5)' }} />
          <span className="text-xs text-[#00ff80] font-medium">Charging</span>
        </div>
      </div>

      {/* Battery Status - Large */}
      <div className="flex items-center gap-5 mb-6 p-4 rounded-xl" style={{ background: 'rgba(255, 255, 255, 0.02)', border: '1px solid rgba(255, 255, 255, 0.05)' }}>
        <motion.div
          animate={{ rotate: [0, 5, -5, 0] }}
          transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
        >
          <BatteryCharging 
            className="w-10 h-10" 
            style={{ color: batteryColor, filter: `drop-shadow(0 0 10px ${batteryColor}40)` }} 
          />
        </motion.div>
        <div className="flex-1">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white font-semibold">Battery Level</span>
            <span className="font-bold font-mono" style={{ color: batteryColor, fontSize: '1.5rem' }}>
              {data.batteryPercentage}%
            </span>
          </div>
          {/* Battery bar */}
          <div className="h-3 rounded-full overflow-hidden" style={{ background: 'rgba(255, 255, 255, 0.05)' }}>
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${data.batteryPercentage}%` }}
              transition={{ duration: 1.5, ease: 'easeOut' }}
              className="h-full rounded-full relative"
              style={{ 
                background: `linear-gradient(90deg, ${batteryColor}80, ${batteryColor})`,
                boxShadow: `0 0 20px ${batteryColor}40`,
              }}
            >
              <div 
                className="absolute inset-0"
                style={{
                  background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)',
                  animation: 'shimmer 2s infinite',
                }}
              />
            </motion.div>
          </div>
          <div className="flex items-center justify-between mt-2 text-[10px] text-gray-500">
            <span>Battery Health: <span style={{ color: '#00ff80' }}>{data.batteryHealth}</span></span>
            <span>{data.batteryVoltage}V nominal</span>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
        {metrics.map((m, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 + i * 0.1 }}
            className="p-3 rounded-xl text-center"
            style={{ background: m.glowColor, border: `1px solid ${m.color}30` }}
          >
            <m.icon className="w-5 h-5 mx-auto mb-1" style={{ color: m.color }} />
            <p className="text-white font-mono font-bold text-sm">{m.value}</p>
            <p className="text-gray-400 text-[10px]">{m.label}</p>
          </motion.div>
        ))}
      </div>

      {/* Daily Production/Consumption */}
      <div className="grid grid-cols-2 gap-3">
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.4 }}
          className="p-4 rounded-xl"
          style={{ 
            background: 'rgba(0, 255, 128, 0.05)', 
            border: '1px solid rgba(0, 255, 128, 0.15)' 
          }}
        >
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-4 h-4 text-[#00ff80]" />
            <span className="text-[10px] text-[#00ff80] font-medium">GENERATED</span>
          </div>
          <p className="text-2xl font-bold text-white font-mono">{data.dailyGeneration}<span className="text-sm text-gray-500"> kWh</span></p>
          <p className="text-[10px] text-gray-500 mt-1">Today so far</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.5 }}
          className="p-4 rounded-xl"
          style={{ 
            background: 'rgba(255, 107, 53, 0.05)', 
            border: '1px solid rgba(255, 107, 53, 0.15)' 
          }}
        >
          <div className="flex items-center gap-2 mb-2">
            <TrendingDown className="w-4 h-4 text-[#ff6b35]" />
            <span className="text-[10px] text-[#ff6b35] font-medium">CONSUMED</span>
          </div>
          <p className="text-2xl font-bold text-white font-mono">{data.dailyConsumption}<span className="text-sm text-gray-500"> kWh</span></p>
          <p className="text-[10px] text-gray-500 mt-1">Today so far</p>
        </motion.div>
      </div>

      {/* Net Production */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
        className="mt-3 p-3 rounded-xl flex items-center justify-between"
        style={{
          background: isPositive ? 'rgba(0, 255, 128, 0.05)' : 'rgba(255, 50, 50, 0.05)',
          border: `1px solid ${isPositive ? 'rgba(0, 255, 128, 0.15)' : 'rgba(255, 50, 50, 0.15)'}`,
        }}
      >
        <span className="text-xs text-gray-400">Net Production</span>
        <span className={`font-mono font-bold ${isPositive ? 'text-[#00ff80]' : 'text-[#ff4444]'}`}>
          {isPositive ? '+' : ''}{netProduction.toFixed(1)} kWh
        </span>
      </motion.div>

      {/* Warning if battery low */}
      {data.batteryPercentage < 30 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mt-3 flex items-center gap-2 p-3 rounded-xl bg-[rgba(255,50,50,0.1)] border border-[rgba(255,50,50,0.2)]"
        >
          <AlertTriangle className="w-4 h-4 text-[#ff4444] flex-shrink-0" />
          <span className="text-xs text-[#ff4444]">Battery critically low! Consider reducing power consumption.</span>
        </motion.div>
      )}
    </motion.div>
  );
}
