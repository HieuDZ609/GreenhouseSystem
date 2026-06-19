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
    { icon: Thermometer, label: 'Nhiệt độ', value: `${plant.thresholds.tempMin}-${plant.thresholds.tempMax}°C`, optimal: `${plant.thresholds.tempOptimal}°C`, color: '#E85D2B' },
    { icon: Droplets, label: 'Độ ẩm đất', value: `${plant.thresholds.soilMoistureMin}-${plant.thresholds.soilMoistureMax}%`, optimal: `${plant.thresholds.soilMoistureOptimal}%`, color: '#2B8A9E' },
    { icon: Wind, label: 'Độ ẩm kk', value: `${plant.thresholds.airHumidityMin}-${plant.thresholds.airHumidityMax}%`, optimal: `${plant.thresholds.airHumidityOptimal}%`, color: '#D4893F' },
    { icon: Sun, label: 'Ánh sáng', value: `${(plant.thresholds.lightMin / 1000).toFixed(0)}-${(plant.thresholds.lightMax / 1000).toFixed(0)}k`, optimal: `${(plant.thresholds.lightOptimal / 1000).toFixed(0)}k`, color: '#B8892D' },
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
          isSelected ? 'ring-2 ring-[#E85D2B]' : ''
        }`}
      >
        <motion.div
          className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"
          style={{
            background: `radial-gradient(circle at ${glowX.get() + 50}% ${glowY.get() + 50}%, ${plant.neonColor}10 0%, transparent 50%)`,
          }}
        />

        <div 
          className="h-1 w-full"
          style={{ 
            background: `linear-gradient(90deg, ${plant.gradientFrom}, ${plant.neonColor}, ${plant.gradientTo})`,
            boxShadow: `0 0 20px ${plant.neonColor}30`
          }}
        />

        <div className="p-5">
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              <motion.div
                whileHover={{ rotate: 15, scale: 1.1 }}
                className="w-12 h-12 rounded-xl flex items-center justify-center"
                style={{
                  background: `linear-gradient(135deg, ${plant.gradientFrom}60, ${plant.gradientTo}40)`,
                  border: `1px solid ${plant.neonColor}35`,
                  boxShadow: `0 0 20px ${plant.neonColor}15`,
                }}
              >
                <Sprout className="w-6 h-6" style={{ color: plant.neonColor }} />
              </motion.div>
              <div>
                <h3 className="text-[#2D1F1A] font-bold text-lg">{plant.name}</h3>
                <p className="text-[#A89080] text-xs italic">{plant.scientificName}</p>
              </div>
            </div>
            {isSelected && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="w-6 h-6 rounded-full bg-[#E85D2B] flex items-center justify-center"
                style={{ boxShadow: '0 0 10px rgba(232, 93, 43, 0.4)' }}
              >
                <ChevronRight className="w-3 h-3 text-white" />
              </motion.div>
            )}
          </div>

          <p className="text-[#6B5B4F] text-sm mb-4 line-clamp-2">{plant.description}</p>

          <div className="grid grid-cols-2 gap-2 mb-4">
            {thresholds.map((t, i) => (
              <motion.div
                key={t.label}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.05 }}
                className="flex items-center gap-2 p-2 rounded-lg bg-[rgba(139,69,19,0.03)]"
              >
                <t.icon className="w-3.5 h-3.5 flex-shrink-0" style={{ color: t.color }} />
                <div>
                  <p className="text-[10px] text-[#A89080]">{t.label}</p>
                  <p className="text-xs text-[#2D1F1A] font-mono font-medium">{t.value}</p>
                </div>
              </motion.div>
            ))}
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-[#A89080]" />
              <span className="text-[10px] text-[#A89080]">{plant.diseases.length} bệnh đang theo dõi</span>
            </div>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={(e) => { e.stopPropagation(); setShowDetails(true); }}
              className="text-xs text-[#D4893F] hover:text-[#E85D2B] hover:underline flex items-center gap-1 font-medium"
            >
              Chi tiết <ChevronRight className="w-3 h-3" />
            </motion.button>
          </div>
        </div>
      </motion.div>

      <AnimatePresence>
        {showDetails && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center p-4"
            style={{ background: 'rgba(255, 248, 240, 0.85)', backdropFilter: 'blur(8px)' }}
            onClick={() => setShowDetails(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 30 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 30 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="glass-card w-full max-w-2xl max-h-[85vh] overflow-hidden"
              style={{ border: `1px solid ${plant.neonColor}25` }}
            >
              <div className="flex items-center justify-between p-5 border-b border-[rgba(255,107,53,0.1)]">
                <div className="flex items-center gap-3">
                  <div 
                    className="w-10 h-10 rounded-lg flex items-center justify-center"
                    style={{ 
                      background: `linear-gradient(135deg, ${plant.gradientFrom}60, ${plant.gradientTo}40)`,
                      border: `1px solid ${plant.neonColor}35`,
                    }}
                  >
                    <Sprout className="w-5 h-5" style={{ color: plant.neonColor }} />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-[#2D1F1A]">{plant.name}</h2>
                    <p className="text-[#A89080] text-xs">{plant.scientificName}</p>
                  </div>
                </div>
                <button 
                  onClick={() => setShowDetails(false)}
                  className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-[rgba(139,69,19,0.08)] transition-colors"
                >
                  <X className="w-4 h-4 text-[#8B6F5E]" />
                </button>
              </div>

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
                        ? 'bg-[rgba(232,93,43,0.1)] text-[#E85D2B] border border-[rgba(232,93,43,0.2)]' 
                        : 'text-[#8B6F5E] hover:text-[#2D1F1A] hover:bg-[rgba(139,69,19,0.05)]'
                    }`}
                  >
                    <tab.icon className="w-4 h-4" />
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="p-5 overflow-y-auto max-h-[60vh] custom-scrollbar">
                {activeTab === 'overview' && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                    <p className="text-[#5C4A3D]">{plant.description}</p>

                    <h4 className="text-[#2D1F1A] font-semibold flex items-center gap-2">
                      <Sprout className="w-4 h-4 text-[#D4893F]" /> Mẹo nhanh
                    </h4>
                    <div className="grid gap-2">
                      {plant.quickTips.map((tip, i) => (
                        <motion.div
                          key={i}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.1 }}
                          className="flex items-start gap-3 p-3 rounded-lg bg-[rgba(232,93,43,0.04)] border border-[rgba(232,93,43,0.1)]"
                        >
                          <div className="w-8 h-8 rounded-lg bg-[rgba(232,93,43,0.08)] flex items-center justify-center flex-shrink-0">
                            <Sprout className="w-4 h-4 text-[#D4893F]" />
                          </div>
                          <div>
                            <p className="text-[#2D1F1A] text-sm font-medium">{tip.title}</p>
                            <p className="text-[#6B5B4F] text-xs">{tip.content}</p>
                          </div>
                        </motion.div>
                      ))}
                    </div>

                    <h4 className="text-[#2D1F1A] font-semibold mt-4">Điều kiện tối ưu</h4>
                    <div className="grid grid-cols-2 gap-3">
                      {thresholds.map((t, i) => (
                        <div key={i} className="p-3 rounded-lg bg-[rgba(139,69,19,0.03)] border border-[rgba(139,69,19,0.06)]">
                          <div className="flex items-center gap-2 mb-1">
                            <t.icon className="w-4 h-4" style={{ color: t.color }} />
                            <span className="text-[#A89080] text-xs">{t.label}</span>
                          </div>
                          <p className="text-[#2D1F1A] font-mono text-sm font-medium">{t.value}</p>
                          <p className="text-[#A89080] text-[10px]">Tối ưu: {t.optimal}</p>
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
                        className="p-4 rounded-xl bg-[rgba(200,80,50,0.04)] border border-[rgba(200,80,50,0.1)]"
                      >
                        <h4 className="text-[#2D1F1A] font-semibold flex items-center gap-2 mb-2">
                          <Stethoscope className="w-4 h-4 text-[#C75A3E]" />
                          {disease.name}
                        </h4>
                        <div className="space-y-2 text-sm">
                          <p><span className="text-[#A89080]">Triệu chứng:</span> <span className="text-[#5C4A3D]">{disease.symptoms}</span></p>
                          <p><span className="text-[#A89080]">Nguyên nhân:</span> <span className="text-[#5C4A3D]">{disease.cause}</span></p>
                          <div>
                            <p className="text-[#D4893F] text-xs font-medium mb-1">Cách điều trị:</p>
                            <ul className="space-y-1">
                              {disease.treatment.map((t, j) => (
                                <li key={j} className="text-[#5C4A3D] text-xs flex items-start gap-2">
                                  <span className="text-[#D4893F] mt-0.5">-</span>{t}
                                </li>
                              ))}
                            </ul>
                          </div>
                          <div>
                            <p className="text-[#2B8A9E] text-xs font-medium mb-1">Phòng ngừa:</p>
                            <ul className="space-y-1">
                              {disease.prevention.map((p, j) => (
                                <li key={j} className="text-[#5C4A3D] text-xs flex items-start gap-2">
                                  <span className="text-[#2B8A9E] mt-0.5">-</span>{p}
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
                      <div className="absolute left-4 top-0 bottom-0 w-px bg-[rgba(232,93,43,0.15)]" />

                      {plant.growthStages.map((stage, i) => (
                        <motion.div
                          key={i}
                          initial={{ opacity: 0, x: -20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.1 }}
                          className="relative pl-10 pb-6"
                        >
                          <div 
                            className="absolute left-2 top-1 w-5 h-5 rounded-full border-2 flex items-center justify-center"
                            style={{ 
                              borderColor: plant.neonColor,
                              background: '#FFF8F0',
                              boxShadow: `0 0 10px ${plant.neonColor}30`
                            }}
                          >
                            <div className="w-1.5 h-1.5 rounded-full" style={{ background: plant.neonColor }} />
                          </div>

                          <div className="p-4 rounded-xl bg-[rgba(139,69,19,0.03)] border border-[rgba(139,69,19,0.06)]">
                            <div className="flex items-center justify-between mb-2">
                              <h4 className="text-[#2D1F1A] font-semibold">{stage.stage}</h4>
                              <span className="text-[10px] text-[#A89080] bg-[rgba(139,69,19,0.05)] px-2 py-0.5 rounded-full">
                                {stage.duration}
                              </span>
                            </div>
                            <p className="text-[#6B5B4F] text-xs mb-2">{stage.description}</p>
                            <div className="flex flex-wrap gap-2">
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(232,93,43,0.08)] text-[#E85D2B] border border-[rgba(232,93,43,0.15)]">
                                {stage.tempRange}
                              </span>
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(43,138,158,0.08)] text-[#2B8A9E] border border-[rgba(43,138,158,0.15)]">
                                {stage.humidityRange}
                              </span>
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[rgba(184,137,45,0.08)] text-[#B8892D] border border-[rgba(184,137,45,0.15)]">
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