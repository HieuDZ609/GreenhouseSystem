import { motion } from 'framer-motion';
import { 
  Sun, 
  Wind, 
  Droplets, 
  AirVent, 
  Power,
  Zap,
  Timer,
  Gauge
} from 'lucide-react';
import type { DeviceState } from '@/hooks/useGreenhouse';

interface RemoteControlProps {
  devices: DeviceState;
  toggleDevice: (device: keyof DeviceState) => void;
}

interface DeviceConfig {
  key: keyof DeviceState;
  label: string;
  description: string;
  icon: React.ElementType;
  color: string;
  glowColor: string;
  activeLabel: string;
  inactiveLabel: string;
  powerDraw: string;
}

const deviceConfigs: DeviceConfig[] = [
  {
    key: 'led',
    label: 'LED Grow Light',
    description: 'Full spectrum LED panel for optimal photosynthesis',
    icon: Sun,
    color: '#ffd000',
    glowColor: 'rgba(255, 208, 0, 0.3)',
    activeLabel: 'ON - Growing',
    inactiveLabel: 'OFF - Standby',
    powerDraw: '45W',
  },
  {
    key: 'fan',
    label: 'Ventilation Fan',
    description: 'Air circulation and temperature control',
    icon: Wind,
    color: '#00ccff',
    glowColor: 'rgba(0, 204, 255, 0.3)',
    activeLabel: 'ON - Active',
    inactiveLabel: 'OFF - Standby',
    powerDraw: '25W',
  },
  {
    key: 'pump',
    label: 'Water Pump',
    description: 'Nutrient delivery and irrigation system',
    icon: Droplets,
    color: '#00ff80',
    glowColor: 'rgba(0, 255, 128, 0.3)',
    activeLabel: 'ON - Circulating',
    inactiveLabel: 'OFF - Standby',
    powerDraw: '15W',
  },
  {
    key: 'vent',
    label: 'Air Vent',
    description: 'Humidity and CO2 exchange control',
    icon: AirVent,
    color: '#ff8833',
    glowColor: 'rgba(255, 136, 51, 0.3)',
    activeLabel: 'OPEN - Ventilating',
    inactiveLabel: 'CLOSED - Sealed',
    powerDraw: '8W',
  },
];

function DeviceCard({ 
  config, 
  isActive, 
  onToggle, 
  index 
}: { 
  config: DeviceConfig; 
  isActive: boolean; 
  onToggle: () => void;
  index: number;
}) {
  const Icon = config.icon;
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.5 }}
      className="glass-card p-5 relative overflow-hidden"
      style={{
        borderColor: isActive ? `${config.color}40` : 'rgba(0, 255, 128, 0.08)',
      }}
    >
      {/* Active glow effect */}
      {isActive && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="absolute inset-0 pointer-events-none"
          style={{
            background: `radial-gradient(circle at 50% 0%, ${config.glowColor} 0%, transparent 60%)`,
          }}
        />
      )}
      
      {/* Top accent line */}
      <div 
        className="absolute top-0 left-0 right-0 h-0.5"
        style={{
          background: isActive ? `linear-gradient(90deg, transparent, ${config.color}, transparent)` : 'transparent',
          boxShadow: isActive ? `0 0 10px ${config.glowColor}` : 'none',
        }}
      />

      <div className="relative z-10">
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <motion.div
              animate={isActive ? { 
                boxShadow: [`0 0 10px ${config.glowColor}`, `0 0 20px ${config.glowColor}`, `0 0 10px ${config.glowColor}`],
              } : {}}
              transition={{ duration: 2, repeat: Infinity }}
              className="w-12 h-12 rounded-xl flex items-center justify-center"
              style={{
                background: isActive ? `${config.glowColor}` : 'rgba(255, 255, 255, 0.03)',
                border: `1px solid ${isActive ? `${config.color}50` : 'rgba(255, 255, 255, 0.05)'}`,
              }}
            >
              <Icon 
                className="w-6 h-6" 
                style={{ color: isActive ? config.color : '#666' }} 
              />
            </motion.div>
            <div>
              <h4 className="text-white font-semibold text-sm">{config.label}</h4>
              <p className="text-gray-500 text-[10px]">{config.description}</p>
            </div>
          </div>
        </div>

        {/* Status */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <motion.div
              animate={isActive ? { scale: [1, 1.2, 1] } : {}}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="w-2 h-2 rounded-full"
              style={{ 
                background: isActive ? config.color : '#444',
                boxShadow: isActive ? `0 0 8px ${config.color}` : 'none',
              }}
            />
            <span 
              className="text-xs font-medium"
              style={{ color: isActive ? config.color : '#666' }}
            >
              {isActive ? config.activeLabel : config.inactiveLabel}
            </span>
          </div>
          <div className="flex items-center gap-1 text-gray-500">
            <Zap className="w-3 h-3" />
            <span className="text-[10px] font-mono">{config.powerDraw}</span>
          </div>
        </div>

        {/* Toggle Switch */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onToggle}
          className="w-full relative h-12 rounded-xl flex items-center justify-center gap-2 transition-all duration-300"
          style={{
            background: isActive 
              ? `linear-gradient(135deg, ${config.glowColor}, rgba(0, 0, 0, 0.2))` 
              : 'rgba(255, 255, 255, 0.03)',
            border: `1px solid ${isActive ? `${config.color}40` : 'rgba(255, 255, 255, 0.05)'}`,
          }}
        >
          <Power 
            className="w-4 h-4" 
            style={{ color: isActive ? config.color : '#666' }} 
          />
          <span 
            className="text-sm font-medium"
            style={{ color: isActive ? config.color : '#888' }}
          >
            {isActive ? 'Turn OFF' : 'Turn ON'}
          </span>
        </motion.button>

        {/* Timer indicator */}
        {isActive && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-3 flex items-center gap-2 text-[10px] text-gray-500"
          >
            <Timer className="w-3 h-3" />
            <span>Running for {Math.floor(Math.random() * 4 + 1)}h {Math.floor(Math.random() * 59)}m</span>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}

export function RemoteControl({ devices, toggleDevice }: RemoteControlProps) {
  const totalPower = Object.entries(devices).reduce((sum, [key, active]) => {
    if (!active) return sum;
    const config = deviceConfigs.find(d => d.key === key);
    return sum + parseInt(config?.powerDraw || '0');
  }, 0);

  const activeCount = Object.values(devices).filter(Boolean).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-wrap items-center justify-between gap-4"
      >
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Gauge className="w-6 h-6 text-[#00ff80]" style={{ filter: 'drop-shadow(0 0 4px rgba(0, 255, 128, 0.3))' }} />
            Remote Control
          </h2>
          <p className="text-gray-400 text-sm mt-1">Control your greenhouse devices remotely</p>
        </div>
        
        {/* Power summary */}
        <div className="flex gap-3">
          <div 
            className="px-4 py-2 rounded-lg flex items-center gap-2"
            style={{
              background: 'rgba(0, 255, 128, 0.05)',
              border: '1px solid rgba(0, 255, 128, 0.15)',
            }}
          >
            <Power className="w-4 h-4 text-[#00ff80]" />
            <span className="text-xs text-gray-300">{activeCount} Active</span>
          </div>
          <div 
            className="px-4 py-2 rounded-lg flex items-center gap-2"
            style={{
              background: 'rgba(255, 208, 0, 0.05)',
              border: '1px solid rgba(255, 208, 0, 0.15)',
            }}
          >
            <Zap className="w-4 h-4 text-[#ffd000]" />
            <span className="text-xs text-gray-300 font-mono">{totalPower}W</span>
          </div>
        </div>
      </motion.div>

      {/* Device Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {deviceConfigs.map((config, i) => (
          <DeviceCard
            key={config.key}
            config={config}
            isActive={devices[config.key]}
            onToggle={() => toggleDevice(config.key)}
            index={i}
          />
        ))}
      </div>

      {/* Quick Actions */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="glass-card p-5"
      >
        <h3 className="text-white font-semibold mb-4">Quick Scenes</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'Morning Routine', desc: 'LEDs + Fan', color: '#ffd000', devices: ['led', 'fan'] as const },
            { label: 'Hot Day', desc: 'Fan + Vent', color: '#00ccff', devices: ['fan', 'vent'] as const },
            { label: 'Watering', desc: 'Pump ON', color: '#00ff80', devices: ['pump'] as const },
            { label: 'Night Mode', desc: 'All OFF', color: '#ff4444', devices: [] as const },
          ].map((scene, i) => (
            <motion.button
              key={i}
              whileHover={{ scale: 1.03, y: -2 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => {
                // Activate scene devices
                scene.devices.forEach(d => {
                  if (!devices[d]) toggleDevice(d);
                });
              }}
              className="p-3 rounded-xl text-left transition-all"
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                border: `1px solid ${scene.color}20`,
              }}
            >
              <div className="w-8 h-8 rounded-lg mb-2 flex items-center justify-center" style={{ background: `${scene.color}15` }}>
                <Power className="w-4 h-4" style={{ color: scene.color }} />
              </div>
              <p className="text-white text-xs font-medium">{scene.label}</p>
              <p className="text-gray-500 text-[10px]">{scene.desc}</p>
            </motion.button>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
