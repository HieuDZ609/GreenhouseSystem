import { motion } from 'framer-motion';
import { Sprout, Check } from 'lucide-react';
import type { Plant } from '@/data/plantsData';

interface PlantSelectorProps {
  plants: Plant[];
  selectedPlantId: string;
  onSelect: (id: string) => void;
}

export function PlantSelector({ plants, selectedPlantId, onSelect }: PlantSelectorProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-card p-4"
    >
      <div className="flex items-center gap-2 mb-3">
        <Sprout className="w-4 h-4 text-[#00ff80]" />
        <span className="text-sm text-gray-400">Active Crop</span>
      </div>
      <div className="flex flex-wrap gap-2">
        {plants.map((plant, i) => {
          const isSelected = plant.id === selectedPlantId;
          return (
            <motion.button
              key={plant.id}
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: i * 0.05 }}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onSelect(plant.id)}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                isSelected 
                  ? 'text-white border' 
                  : 'text-gray-400 bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)] hover:text-white hover:bg-[rgba(255,255,255,0.05)]'
              }`}
              style={isSelected ? {
                background: `${plant.neonColor}15`,
                borderColor: `${plant.neonColor}50`,
                boxShadow: `0 0 15px ${plant.neonColor}15`,
              } : {}}
            >
              {isSelected && (
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', stiffness: 500 }}
                >
                  <Check className="w-3 h-3" style={{ color: plant.neonColor }} />
                </motion.div>
              )}
              <span style={isSelected ? { color: plant.neonColor } : {}}>{plant.name}</span>
            </motion.button>
          );
        })}
      </div>
    </motion.div>
  );
}
