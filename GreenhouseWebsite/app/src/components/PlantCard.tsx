import { motion, useMotionValue, useTransform, AnimatePresence } from 'framer-motion';
import { useState, useRef } from 'react';
import { 
  Thermometer, 
  Droplets, 
  Wind, 
  Sun, 
  ChevronRight, 
  Sprout, 
  ShieldAlert,
  BookOpen,
  Stethoscope,
  X
} from 'lucide-react';
import type { Plant } from '@/data/plantsData';

interface PlantCardProps {
  plant: Plant;
  isSelected: boolean;
  onSelect: () => void;
  index: number;
}

export function PlantCard({ plant, isSelected, onSelect, index }: PlantCardProps) {
  const [showDetails, setShowDetails] = useState(false);
  const [activeTab, setActiveTab] = useState<'overview' | 'diseases' | 'stages'>('overview');
  const cardRef = useRef<HTMLDivElement>(null);

  // 3D tilt effect
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const rotateX = useTransform(y, [-100, 100], [8, -8]);
  const rotateY = useTransform(x, [-100, 100], [-8, 8]);
  const glowX = useTransform(x, [-100, 100], [-20, 20]);
  const glowY = useTransform(y, [-100, 100], [-20, 20]);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    x.set(e.clientX - centerX);
    y.set(e.clientY - centerY);
  };

  const handleMouseLeave = () => {
    x.set(0);
    y.set(0);
  };

  const thresholds = [
    { icon: Thermometer, label: 'Nhiệt độ', value: `${plant.thresholds.tempMin}-${plant.thresholds.tempMax}°C`, optimal: `${plant.thresholds.tempOptimal}°C`, color: '#FF6B35' },
    { icon: Droplets, label: 'Độ ẩm đất', value: `${plant.thresholds.soilMoistureMin}-${plant.thresholds.soilMoistureMax}%`, optimal: `${plant.thresholds.soilMoistureOptimal}%`, color: '#00ccff' },
    { icon: Wind, label: 'Độ ẩm kk', value: `${plant.thresholds.airHumidityMin}-${plant.thresholds.airHumidityMax}%`, optimal: `${plant.thresholds.airHumidityOptimal}%`, color: '#F4A261' },
    { icon: Sun, label: 'Ánh sáng', value: `${(plant.thresholds.lightMin / 1000).toFixed(0)}-${(plant.thresholds.lightMax / 1000).toFixed(0)}k`, optimal: `${(plant.thresholds.lightOptimal / 1000).toFixed(0)}k`, color: '#ffd000' },
  ];

  return (
    <>
      <motion.div
        ref={cardRef}
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.1, duration: 0.5 }}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        onClick={() => onSelect()}
        style={{
          rotateX: isSelected ? 0 : rotateX,
          rotateY: isSelected ? 0 : rotateY,
          transformStyle: 'preserve-3d',
          perspective: 1000,
        }}
        whileHover={{ scale: 1.02, z: 50 }}
        whileTap={{ scale: 0.98 }}
        className={`glass-card cursor-pointer group relative overflow-hidden ${
          isSelected ? 'ring-2 ring-[#FF6B35]' : ''
        }`}
      >
        {/* Sunset glow effect on hover */}
        <motion.div
          className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"
          style={{
            background: `radial-gradient(circle at ${glowX.get() + 50}% ${glowY.get() + 50}%, ${plant.neonColor}15 0%, transparent 50%)`,
          }}
        />

        {/* Top gradient bar */}
        <div 
          className="h-1 w-full"
          style={{ 
            background: `linear-gradient(90deg, ${plant.gradientFrom}, ${plant.neonColor}, ${plant.gradientTo})`,
            boxShadow: `0 0 20px ${plant.neonColor}40`
          }}
        />

        <div className="p-5">
          {/* Header */}
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              <motion.div
                whileHover={{ rotate: 15, scale: 1.1 }}
                className="w-12 h-12 rounded-xl flex items-center justify-center"
                style={{
                  background: `linear-gradient(135deg, ${plant.gradientFrom}80, ${plant.gradientTo}60)`,
                  border: `1px solid ${plant.neonColor}40`,
                  boxShadow: `0 0 20px ${plant.neonColor}20`,
                }}
              >
                <Sprout className="w-6 h-6" style={{ color: plant.neonColor }} />
              </motion.div>
              <div>
                <h3 className="text-white font-bold text-lg">{plant.name}</h3>
                <p className="text-gray-500 text-xs italic">{plant.scientificName}</p>
              </div>
            </div>
            {isSelected && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center"
                style={{ boxShadow: '0 0 10px rgba(255, 107, 53, 0.5)' }}
              >
                <ChevronRight className="w-3 h-3 text-white" />
              </motion.div>
            )}
          </div>

          {/* Description */}
          <p className="text-gray-400 text-sm mb-4 line-clamp-2">{plant.description}</p>

          {/* Thresholds Grid */}
          <div className="grid grid-cols-2 gap-2 mb-4">
            {thresholds.map((t, i) => (
              <motion.div
                key={t.label}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.05 }}
                className="flex items-center gap-2 p-2 rounded-lg bg-[rgba(255,255,255,0.03)]"
              >
                <t.icon className="w-3.5 h-3.5 flex-shrink-0" style={{ color: t.color }} />
                <div>
                  <p className="text-[10px] text-gray-500">{t.label}</p>
                  <p className="text-xs text-white font-mono">{t.value}</p>
                </div>
              </motion.div>
            ))}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-gray-500" />
              <span className="text-[10px] text-gray-500">{plant.diseases.length} bệnh đang theo dõi</span>
            </div>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={(e) => { e.stopPropagation(); setShowDetails(true); }}
              className="text-xs text-[#F4A261] hover:underline flex items-center gap-1"
            >
              Chi tiết <ChevronRight className="w-3 h-3" />
            </motion.button>
          </div>
        </div>
      </motion.div>

      {/* Detail Modal */}
      <AnimatePresence>
        {showDetails && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center p-4"
            style={{ background: 'rgba(0, 0, 0, 0.7)', backdropFilter: 'blur(8px)' }}
            onClick={() => setShowDetails(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 30 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 30 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="glass-card w-full max-w-2xl max-h-[85vh] overflow-hidden"
              style={{ border: `1px solid ${plant.neonColor}30` }}
            >
              {/* Header */}
              <div className="flex items-center justify-between p-5 border-b border-[rgba(255,107,53,0.1)]">
                <div className="flex items-center gap-3">
                  <div 
                    className="w-10 h-10 rounded-lg flex items-center justify-center"
                    style={{ 
                      background: `linear-gradient(135deg, ${plant.gradientFrom}80, ${plant.gradientTo}60)`,
                      border: `1px solid ${plant.neonColor}40`,
                    }}
                  >
                    <Sprout className="w-5 h-5" style={{ color: plant.neonColor }} />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">{plant.name}</h2>
                    <p className="text-gray-500 text-xs">{plant.scientificName}</p>
                  </div>
                </div>
                <button 
                  onClick={() => setShowDetails(false)}
                  className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-[rgba(255,255,255,0.1)] transition-colors"
                >
                  <X className="w-4 h-4 text-gray-400" />
                </button>
              </div>

              {/* Tabs */}
              <div className="flex gap-1 p-3 border-b border-[rgba(255,107,53,0.1)]">
                {[
                  { id: 'overview' as const, label: 'Tổng quan', icon: BookOpen },
                  { id: 'diseases' as const, label: 'Bệnh hại', icon: Stethoscope },
                  { id: 'stages' as const, label: 'Giai đoạn sinh trưởng', icon: Sprout },
                ].map(tab => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition-all ${
                      activeTab === tab.id 
                        ? 'bg-[rgba(255,107,53,0.12)] text-[#FF6B35] border border-[rgba(255,107,53,0.25)]' 
                        : 'text-gray-400 hover:text-white hover:bg-[rgba(255,255,255,0.05)]'
                    }`}
                  >
                    <tab.icon className="w-4 h-4" />
                    {tab.label}
                  </button>
                ))}
              </div>

              {/* Content */}
              <div className="p-5 overflow-y-auto max-h-[60vh] custom-scrollbar">
                {activeTab === 'overview' && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                    <p className="text-gray-300">{plant.description}</p>

                    <h4 className="text-white font-semibold flex items-center gap-2">
                      <Sprout className="w-4 h-4 text-[#F4A261]" /> Mẹo nhanh
                    </h4>
                    <div className="grid gap-2">
                      {plant.quickTips.map((tip, i) => (
                        <motion.div
                          key={i}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.1 }}
                          className="flex items-start gap-3 p-3 rounded-lg bg-[rgba(255,107,53,0.05)] border border-[rgba(255,107,53,0.1)]"
                        >
                          <div className="w-8 h-8 rounded-lg bg-[rgba(255,107,53,0.1)] flex items-center justify-center flex-shrink-0">
                            <Sprout className="w-4 h-4 text-[#F4A261]" />
                          </div>
                          <div>
                            <p className="text-white text-sm font-medium">{tip.title}</p>
                            <p className="text-gray-400 text-xs">{tip.content}</p>
                          </div>
                        </motion.div>
                      ))}
                    </div>

                    <h4 className="text-white font-semibold mt-4">Điều kiện tối ưu</h4>
                    <div className="grid grid-cols-2 gap-3">
                      {thresholds.map((t, i) => (
                        <div key={i} className="p-3 rounded-lg bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)]">
                          <div className="flex items-center gap-2 mb-1">
                            <t.icon className="w-4 h-4" style={{ color: t.color }} />
                            <span className="text-gray-400 text-xs">{t.label}</span>
                          </div>
                          <p className="text-white font-mono text-sm">{t.value}</p>
                          <p className="text-gray-500 text-[10px]">Tối ưu: {t.optimal}</p>
                        </div>
                      ))}
                    </div>
                  </motion.div>
                )}

                {activeTab === 'diseases' && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
                    {plant.diseases.map((disease, i) => (
                      <motion.div
                        key={i}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.1 }}
                        className="p-4 rounded-xl bg-[rgba(231,111,81,0.05)] border border-[rgba(231,111,81,0.12)]"
                      >
                        <h4 className="text-white font-semibold flex items-center gap-2 mb-2">
                          <Stethoscope className="w-4 h-4 text-[#E76F51]" />
                          {disease.name}
                        </h4>
                        <div className="space-y-2 text-sm">
                          <p><span className="text-gray-500">Triệu chứng:</span> <span className="text-gray-300">{disease.symptoms}</span></p>
                          <p><span className="text-gray-500">Nguyên nhân:</span> <span className="text-gray-300">{disease.cause}</span></p>
                          <div>
                            <p className="text-[#F4A261] text-xs font-medium mb-1">Cách điều trị:</p>
                            <ul className="space-y-1">
                              {disease.treatment.map((t, j) => (
                                <li key={j} className="text-gray-300 text-xs flex items-start gap-2">
                                  <span className="text-[#F4A261] mt-0.5">-</span>{t}
                                </li>
                              ))}
                            </ul>
                          </div>
                          <div>
                            <p className="text-[#00ccff] text-xs font-medium mb-1">Phòng ngừa:</p>
                            <ul className="space-y-1">
                              {disease.prevention.map((p, j) => (
                                <li key={j} className="text-gray-300 text-xs flex items-start gap-2">
                                  <span className="text-[#00ccff] mt-0.5">-</span>{p}
                                </li>
                              ))}
                            </ul>
                          </div>
                        </div>
                      </motion.div>
                    ))}
                  </motion.div>
                )}

                {activeTab === 'stages' && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
                    <div className="relative">
                      {/* Timeline line */}
                      <div className="absolute left-4 top-0 bottom-0 w-px bg-[rgba(255,107,53,0.2)]" />

                      {plant.growthStages.map((stage, i) => (
                        <motion.div
                          key={i}
                          initial={{ opacity: 0, x: -20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.1 }}
                          className="relative pl-10 pb-6"
                        >
                          {/* Timeline dot */}
                          <div 
                            className="absolute left-2 top-1 w-5 h-5 rounded-full border-2 flex items-center justify-center"
                            style={{ 
                              borderColor: plant.neonColor,
                              background: 'rgba(26, 18, 30, 0.9)',
                              boxShadow: `0 0 10px ${plant.neonColor}40`
                            }}
                          >
                            <div className="w-1.5 h-1.5 rounded-full" style={{ background: plant.neonColor }} />
                          </div>

                          <div className="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)]">
                            <div className="flex items-center justify-between mb-2">
                              <h4 className="text-white font-semibold">{stage.stage}</h4>
                              <span className="text-[10px] text-gray-500 bg-[rgba(255,255,255,0.05)] px-2 py-0.5 rounded-full">
                                {stage.duration}
                              </span>
                            </div>
                            <p className="text-gray-400 text-xs mb-2">{stage.description}</p>
                            <div className="flex flex-wrap gap-2">
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(255,107,53,0.1)] text-[#FF6B35] border border-[rgba(255,107,53,0.2)]">
                                {stage.tempRange}
                              </span>
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(0,204,255,0.1)] text-[#00ccff] border border-[rgba(0,204,255,0.2)]">
                                {stage.humidityRange}
                              </span>
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(255,208,0,0.1)] text-[#ffd000] border border-[rgba(255,208,0,0.2)]">
                                {stage.lightRequirement}
                              </span>
                            </div>
                          </div>
                        </motion.div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}