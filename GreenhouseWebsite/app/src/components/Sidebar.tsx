import { motion } from 'framer-motion';
import {  
  Leaf, 
  Users, 
  Sprout,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import { useState } from 'react';
import type { ViewType } from '@/hooks/useGreenhouse';

interface SidebarProps {
  currentView: ViewType;
  setCurrentView: (view: ViewType) => void;
}

const menuItems: { icon: React.ElementType; label: string; view: ViewType }[] = [
  { icon: Leaf, label: 'Cây Trồng', view: 'plants' },
  { icon: Users, label: 'Chuyên Gia', view: 'experts' },
];

export function Sidebar({ currentView, setCurrentView }: SidebarProps) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <motion.aside
      initial={{ x: -80, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      className={`fixed left-0 top-0 h-screen z-50 flex flex-col transition-all duration-300 ${
        collapsed ? 'w-16' : 'w-64'
      }`}
      style={{
        background: 'linear-gradient(180deg, rgba(26, 18, 30, 0.98) 0%, rgba(20, 12, 25, 0.99) 100%)',
        borderRight: '1px solid rgba(255, 107, 53, 0.12)',
      }}
    >
      {/* Logo Area */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-[rgba(255,107,53,0.1)]">
        <motion.div
          whileHover={{ rotate: 15, scale: 1.1 }}
          transition={{ type: 'spring', stiffness: 300 }}
          className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.25), rgba(244, 162, 97, 0.2))',
            border: '1px solid rgba(255, 107, 53, 0.35)',
            boxShadow: '0 0 15px rgba(255, 107, 53, 0.2)',
          }}
        >
          <Sprout className="w-5 h-5 text-[#FF6B35]" />
        </motion.div>
        {!collapsed && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
          >
            <h1 className="text-lg font-bold text-white leading-tight">
              GreenHouse
              <span className="block text-[10px] font-medium text-[#F4A261] tracking-[0.2em] uppercase">Nhà Kính Thông Minh</span>
            </h1>
          </motion.div>
        )}
      </div>

      {/* Collapse Button */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-20 w-6 h-6 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
        style={{
          background: 'rgba(255, 107, 53, 0.25)',
          border: '1px solid rgba(255, 107, 53, 0.4)',
          boxShadow: '0 0 10px rgba(255, 107, 53, 0.15)',
        }}
      >
        {collapsed ? (
          <ChevronRight className="w-3 h-3 text-[#F4A261]" />
        ) : (
          <ChevronLeft className="w-3 h-3 text-[#F4A261]" />
        )}
      </button>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-6 space-y-2">
        {menuItems.map((item, index) => {
          const Icon = item.icon;
          const isActive = currentView === item.view;

          return (
            <motion.button
              key={item.view}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.1 * index, duration: 0.3 }}
              onClick={() => setCurrentView(item.view)}
              whileHover={{ x: 4 }}
              whileTap={{ scale: 0.98 }}
              className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-300 group relative ${
                collapsed ? 'justify-center' : ''
              }`}
              style={{
                background: isActive 
                  ? 'linear-gradient(135deg, rgba(255, 107, 53, 0.18), rgba(244, 162, 97, 0.1))' 
                  : 'transparent',
                border: isActive 
                  ? '1px solid rgba(255, 107, 53, 0.35)' 
                  : '1px solid transparent',
              }}
            >
              {/* Active indicator line */}
              {isActive && (
                <motion.div
                  layoutId="activeIndicator"
                  className="absolute left-0 top-1.3 -translate-y-1/2 w-1 h-8 rounded-full bg-[#FF6B35]"
                  style={{ boxShadow: '0 0 10px rgba(255, 107, 53, 0.5)' }}
                />
              )}

              <Icon 
                className={`w-5 h-5 transition-all duration-300 ${
                  isActive 
                    ? 'text-[#FF6B35]' 
                    : 'text-gray-400 group-hover:text-[#F4A261]'
                }`}
                style={isActive ? { filter: 'drop-shadow(0 0 6px rgba(255, 107, 53, 0.5))' } : {}}
              />

              {!collapsed && (
                <span className={`text-sm font-medium transition-all duration-300 ${
                  isActive 
                    ? 'text-white' 
                    : 'text-gray-400 group-hover:text-white'
                }`}>
                  {item.label}
                </span>
              )}

              {/* Sunset glow on hover */}
              {!isActive && (
                <div 
                  className="absolute inset-0 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity duration-300"
                  style={{
                    background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.06), rgba(244, 162, 97, 0.04))',
                    border: '1px solid rgba(255, 107, 53, 0.15)',
                  }}
                />
              )}
            </motion.button>
          );
        })}
      </nav>

    </motion.aside>
  );
}