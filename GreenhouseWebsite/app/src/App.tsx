import { motion, AnimatePresence } from 'framer-motion';
import { useEffect } from 'react';
import { Sidebar } from '@/components/Sidebar';
import { PlantCard } from '@/components/PlantCard';
import { ExpertProfiles } from '@/components/ExpertProfiles';
import { useGreenhouse } from '@/hooks/useGreenhouse';
import { Activity, Sprout } from 'lucide-react';

function App() {
  const {
    currentView,
    setCurrentView,
    selectedPlantId,
    setSelectedPlantId,
    selectedPlant,
    latestReading,  
    plants,
  } = useGreenhouse();
  
  useEffect(() => {
    setCurrentView('plants');
  }, [setCurrentView]);

  const renderContent = () => {
    switch (currentView) {
      case 'plants':
        return (
          <motion.div
            key="plants"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-6"
          >
            <motion.div
              initial={{ opacity: 0, y: -20 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <h2 className="text-3xl font-bold text-white flex items-center gap-2">
                <Sprout className="w-7 h-7 text-[#00ff80]" style={{ filter: 'drop-shadow(0 0 6px rgba(0, 255, 128, 0.4))' }} />
                Dữ liệu Cây trồng
              </h2>
              <p className="text-gray-400 text-sm mt-1">
                Chọn và khám phá cây trồng trong nhà kính của bạn. Nhấn vào bất kỳ cây nào để xem thông tin chi tiết.

              </p>
            </motion.div>

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {plants.map((plant, index) => (
                <PlantCard
                  key={plant.id}
                  plant={plant}
                  isSelected={plant.id === selectedPlantId}
                  onSelect={() => setSelectedPlantId(plant.id)}
                  index={index}
                />
              ))}
            </div>

            {/* Selected Plant Detailed Info */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="glass-card p-6"
            >
              <h3 className="text-xl font-bold text-white mb-4">
                Đang theo dõi: <span style={{ color: selectedPlant.neonColor }}>{selectedPlant.name}</span>
              </h3>
              <p className="text-gray-300 mb-4">{selectedPlant.description}</p>
              
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                {[
                  { label: 'Optimal Temp', value: `${selectedPlant.thresholds.tempOptimal}°C`, range: `${selectedPlant.thresholds.tempMin}-${selectedPlant.thresholds.tempMax}°C`, color: '#ff6b35' },
                  { label: 'Optimal Soil', value: `${selectedPlant.thresholds.soilMoistureOptimal}%`, range: `${selectedPlant.thresholds.soilMoistureMin}-${selectedPlant.thresholds.soilMoistureMax}%`, color: '#00ccff' },
                  { label: 'Optimal Humidity', value: `${selectedPlant.thresholds.airHumidityOptimal}%`, range: `${selectedPlant.thresholds.airHumidityMin}-${selectedPlant.thresholds.airHumidityMax}%`, color: '#00ff80' },
                  { label: 'Optimal Light', value: `${(selectedPlant.thresholds.lightOptimal / 1000).toFixed(0)}k lux`, range: `${(selectedPlant.thresholds.lightMin / 1000).toFixed(0)}-${(selectedPlant.thresholds.lightMax / 1000).toFixed(0)}k`, color: '#ffd000' },
                ].map((item, i) => (
                  <div key={i} className="p-3 rounded-lg" style={{ background: 'rgba(255, 255, 255, 0.03)', border: `1px solid ${item.color}20` }}>
                    <p className="text-gray-500 text-[10px]">{item.label}</p>
                    <p className="text-white font-mono font-bold" style={{ color: item.color }}>{item.value}</p>
                    <p className="text-gray-500 text-[10px]">Range: {item.range}</p>
                  </div>
                ))}
              </div>

              {/* Actionable Insights for selected plant */}
              <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                <Activity className="w-4 h-4 text-[#00ff80]" /> Smart Alerts
              </h4>
              <div className="grid gap-2">
                {selectedPlant.actionableInsights.map((insight, i) => (
                  <motion.div
                    key={i}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.1 }}
                    className={`flex items-center gap-3 p-3 rounded-lg ${
                      insight.severity === 'critical' 
                        ? 'bg-[rgba(255,50,50,0.05)] border border-[rgba(255,50,50,0.15)]' 
                        : insight.severity === 'high'
                        ? 'bg-[rgba(255,200,0,0.05)] border border-[rgba(255,200,0,0.15)]'
                        : 'bg-[rgba(0,255,128,0.05)] border border-[rgba(0,255,128,0.1)]'
                    }`}
                  >
                    <div className={`w-2 h-2 rounded-full ${
                      insight.severity === 'critical' ? 'bg-[#ff4444]' : 
                      insight.severity === 'high' ? 'bg-[#ffd000]' : 'bg-[#00ff80]'
                    }`} style={{ 
                      boxShadow: insight.severity === 'critical' ? '0 0 8px rgba(255, 68, 68, 0.5)' : 
                                 insight.severity === 'high' ? '0 0 8px rgba(255, 208, 0, 0.5)' : 
                                 '0 0 8px rgba(0, 255, 128, 0.5)'
                    }} />
                    <span className="text-sm text-gray-300">{insight.action}</span>
                    <span className={`text-[10px] px-2 py-0.5 rounded-full ml-auto ${
                      insight.severity === 'critical' ? 'bg-[rgba(255,50,50,0.15)] text-[#ff4444]' :
                      insight.severity === 'high' ? 'bg-[rgba(255,200,0,0.15)] text-[#ffd000]' :
                      'bg-[rgba(0,255,128,0.15)] text-[#00ff80]'
                    }`}>
                      {insight.severity.toUpperCase()}
                    </span>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        );

      case 'experts':
        return (
          <motion.div
            key="experts"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <ExpertProfiles />
          </motion.div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="flex min-h-screen bg-grid-pattern" style={{ backgroundColor: '#0a0f14' }}>
      <Sidebar currentView={currentView} setCurrentView={setCurrentView} />
      
      <main className="flex-1 ml-64 p-6 lg:p-8">
        {/* Ambient background glow */}
        <div className="fixed top-0 right-0 w-[500px] h-[500px] pointer-events-none opacity-30"
          style={{
            background: 'radial-gradient(circle, rgba(0, 255, 128, 0.05) 0%, transparent 70%)',
          }}
        />
        <div className="fixed bottom-0 left-[300px] w-[400px] h-[400px] pointer-events-none opacity-20"
          style={{
            background: 'radial-gradient(circle, rgba(0, 204, 255, 0.05) 0%, transparent 70%)',
          }}
        />

        <AnimatePresence mode="wait">
          {renderContent()}
        </AnimatePresence>

        {/* Footer */}
        <motion.footer
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1 }}
          className="mt-12 pt-6 border-t border-[rgba(0,255,128,0.08)]"
        >
          <div className="flex flex-wrap items-center justify-between gap-4 text-[10px] text-gray-600">
            <div className="flex items-center gap-2">
              <Sprout className="w-3 h-3 text-[#00ff80]" />
              <span>GreenHouse_HK</span>
            </div>
            <div className="flex items-center gap-4">
              <span>ESP32-S3 Connected</span>
              <span>WebSocket: Active</span>
              <span>Last Sync: {latestReading ? new Date(latestReading.timestamp).toLocaleTimeString() : 'Never'}</span>
            </div>
          </div>
        </motion.footer>
      </main>
    </div>
  );
}

export default App;
