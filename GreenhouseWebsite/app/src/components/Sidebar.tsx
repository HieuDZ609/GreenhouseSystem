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
        background: 'linear-gradient(180deg, rgba(255, 250, 245, 0.98) 0%, rgba(255, 248, 240, 0.99) 100%)',
        borderRight: '1px solid rgba(232, 93, 43, 0.1)',
      }}
    >
      {/* Logo Area */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-[rgba(232,93,43,0.08)]">
        <motion.div
          whileHover={{ rotate: 15, scale: 1.1 }}
          transition={{ type: 'spring', stiffness: 300 }}
          className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{
            background: 'linear-gradient(135deg, rgba(232, 93, 43, 0.15), rgba(212, 137, 63, 0.12))',
            border: '1px solid rgba(232, 93, 43, 0.25)',
            boxShadow: '0 0 15px rgba(232, 93, 43, 0.1)',
          }}
        >
          <Sprout className="w-5 h-5 text-[#E85D2B]" />
        </motion.div>
        {!collapsed && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
          >
            <h1 className="text-lg font-bold text-[#2D1F1A] leading-tight">
              GreenHouse
              <span className="block text-[10px] font-medium text-[#D4893F] tracking-[0.2em] uppercase">Nhà Kính Thông Minh</span>
            </h1>
          </motion.div>
        )}
      </div>

      {/* Collapse Button */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-20 w-6 h-6 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
        style={{
          background: 'rgba(232, 93, 43, 0.15)',
          border: '1px solid rgba(232, 93, 43, 0.3)',
          boxShadow: '0 0 10px rgba(232, 93, 43, 0.08)',
        }}
      >
        {collapsed ? (
          <ChevronRight className="w-3 h-3 text-[#D4893F]" />
        ) : (
          <ChevronLeft className="w-3 h-3 text-[#D4893F]" />
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
                  ? 'linear-gradient(135deg, rgba(232, 93, 43, 0.1), rgba(212, 137, 63, 0.06))' 
                  : 'transparent',
                border: isActive 
                  ? '1px solid rgba(232, 93, 43, 0.25)' 
                  : '1px solid transparent',
              }}
            >
              {isActive && (
                <motion.div
                  layoutId="activeIndicator"
                  className="absolute left-0 top-2 -translate-y-1/2 w-1 h-8 rounded-full bg-[#E85D2B]"
                  style={{ boxShadow: '0 0 10px rgba(232, 93, 43, 0.4)' }}
                />
              )}

              <Icon 
                className={`w-5 h-5 transition-all duration-300 ${
                  isActive 
                    ? 'text-[#E85D2B]' 
                    : 'text-[#A89080] group-hover:text-[#D4893F]'
                }`}
                style={isActive ? { filter: 'drop-shadow(0 0 4px rgba(232, 93, 43, 0.3))' } : {}}
              />

              {!collapsed && (
                <span className={`text-sm font-medium transition-all duration-300 ${
                  isActive 
                    ? 'text-[#2D1F1A]' 
                    : 'text-[#8B6F5E] group-hover:text-[#2D1F1A]'
                }`}>
                  {item.label}
                </span>
              )}

              {!isActive && (
                <div 
                  className="absolute inset-0 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity duration-300"
                  style={{
                    background: 'linear-gradient(135deg, rgba(232, 93, 43, 0.04), rgba(212, 137, 63, 0.03))',
                    border: '1px solid rgba(232, 93, 43, 0.1)',
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