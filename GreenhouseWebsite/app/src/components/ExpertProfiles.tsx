import { motion, useScroll, useTransform } from 'framer-motion';
import { useRef, useState } from 'react';
import { 
  Award, 
  BookOpen, 
  Clock, 
  Star, 
  ChevronRight, 
  X,
  Flame,
  Shield,
  Zap,
  Heart,
  Mail,
  Phone,
  ExternalLink
} from 'lucide-react';
import { experts, type Expert } from '@/data/plantsData';

function ExpertCard({ expert, index }: { expert: Expert; index: number }) {
  const [showModal, setShowModal] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  const { scrollYProgress } = useScroll({
    target: cardRef,
    offset: ['start end', 'center center']
  });

  const opacity = useTransform(scrollYProgress, [0, 0.5, 1], [0, 1, 1]);
  const x = useTransform(scrollYProgress, [0, 0.5, 1], [index % 2 === 0 ? -100 : 100, 0, 0]);
  const scale = useTransform(scrollYProgress, [0, 0.5, 1], [0.8, 1, 1]);

  const skillColors = ['#FF6B35', '#F4A261', '#ffd000', '#E76F51', '#6D597A'];

  return (
    <>
      <motion.div
        ref={cardRef}
        style={{ opacity, x, scale }}
        className="glass-card p-6 cursor-pointer group"
        onClick={() => setShowModal(true)}
        whileHover={{ y: -5 }}
      >
        {/* Top accent */}
        <div 
          className="h-1 w-full mb-5 rounded-full"
          style={{ 
            background: `linear-gradient(90deg, ${skillColors[index % skillColors.length]}, transparent)`,
          }}
        />

        <div className="flex gap-5">
          {/* Avatar */}
          <motion.div
            whileHover={{ scale: 1.05, rotate: 5 }}
            className="w-20 h-20 rounded-2xl flex-shrink-0 relative overflow-hidden"
            style={{
              border: `1px solid ${skillColors[index % skillColors.length]}40`,
              boxShadow: `0 0 15px ${skillColors[index % skillColors.length]}20`,
            }}
          >
            <img 
              src={`/experts/${expert.id.replace('expert-', '')}.jpg`}
              alt={expert.name}
              className="w-full h-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          </motion.div>

          <div className="flex-1">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-white font-bold text-lg group-hover:text-[#FF6B35] transition-colors">
                  {expert.name}
                </h3>
                <p className="text-[#F4A261] text-sm">{expert.title}</p>
                <p className="text-gray-500 text-xs mt-0.5">{expert.specialty}</p>
              </div>
              <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[rgba(255,107,53,0.08)] border border-[rgba(255,107,53,0.15)]">
                <Star className="w-3 h-3 text-[#ffd000]" />
                <span className="text-[10px] text-[#F4A261] font-medium">{expert.yearsExperience} năm</span>
              </div>
            </div>

            <p className="text-gray-400 text-sm mt-3 line-clamp-2">{expert.bio}</p>

            {/* Skills */}
            <div className="flex flex-wrap gap-1.5 mt-3">
              {expert.skills.map((skill, i) => (
                <span
                  key={i}
                  className="text-[10px] px-2 py-0.5 rounded-full border"
                  style={{
                    color: skillColors[i % skillColors.length],
                    borderColor: `${skillColors[i % skillColors.length]}30`,
                    background: `${skillColors[i % skillColors.length]}10`,
                  }}
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Quick tips preview */}
        <div className="mt-4 pt-4 border-t border-[rgba(255,107,53,0.08)]">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-3.5 h-3.5 text-[#ffd000]" />
            <span className="text-xs text-gray-400">Mẹo nhanh</span>
          </div>
          <p className="text-sm text-gray-300 italic">"{expert.quickTips[0].content}"</p>
        </div>
      </motion.div>

      {/* Detail Modal */}
      {showModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[100] flex items-center justify-center p-4 overflow-y-auto"
          style={{ background: 'rgba(0, 0, 0, 0.8)', backdropFilter: 'blur(12px)' }}
          onClick={() => setShowModal(false)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 40 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 40 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            onClick={e => e.stopPropagation()}
            className="glass-card w-full max-w-2xl my-8"
            style={{ border: '1px solid rgba(255, 107, 53, 0.15)' }}
          >
            {/* Header */}
            <div className="relative p-6 border-b border-[rgba(255,107,53,0.1)]">
              <div 
                className="absolute top-0 left-0 right-0 h-1"
                style={{ 
                  background: `linear-gradient(90deg, ${skillColors[index % skillColors.length]}, transparent)`,
                }}
              />
              <button 
                onClick={() => setShowModal(false)}
                className="absolute top-4 right-4 w-8 h-8 rounded-lg flex items-center justify-center hover:bg-[rgba(255,255,255,0.1)] transition-colors"
              >
                <X className="w-4 h-4 text-gray-400" />
              </button>

              <div className="flex items-center gap-4">
                <div 
                  className="w-16 h-16 rounded-2xl overflow-hidden"
                  style={{
                    border: `1px solid ${skillColors[index % skillColors.length]}40`,
                    boxShadow: `0 0 15px ${skillColors[index % skillColors.length]}20`,
                  }}
                >
                  <img 
                    src={`/experts/${expert.id.replace('expert-', '')}.jpg`}
                    alt={expert.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-white">{expert.name}</h2>
                  <p className="text-[#F4A261]">{expert.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <Award className="w-3 h-3 text-[#ffd000]" />
                    <span className="text-[10px] text-gray-400">{expert.yearsExperience} năm kinh nghiệm</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="p-6 space-y-6 max-h-[70vh] overflow-y-auto custom-scrollbar">
              {/* Bio */}
              <div>
                <h4 className="text-white font-semibold mb-2 flex items-center gap-2">
                  <BookOpen className="w-4 h-4 text-[#F4A261]" /> Giới thiệu
                </h4>
                <p className="text-gray-300 text-sm leading-relaxed">{expert.bio}</p>
              </div>

              {/* Skills */}
              <div>
                <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#ffd000]" /> Chuyên môn
                </h4>
                <div className="grid grid-cols-2 gap-2">
                  {expert.skills.map((skill, i) => (
                    <motion.div
                      key={i}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.1 }}
                      className="flex items-center gap-2 p-2 rounded-lg"
                      style={{
                        background: `${skillColors[i % skillColors.length]}08`,
                        border: `1px solid ${skillColors[i % skillColors.length]}20`,
                      }}
                    >
                      <Heart className="w-3 h-3" style={{ color: skillColors[i % skillColors.length] }} />
                      <span className="text-sm text-gray-300">{skill}</span>
                    </motion.div>
                  ))}
                </div>
              </div>

              {/* Quick Tips */}
              <div>
                <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                  <Flame className="w-4 h-4 text-[#E76F51]" /> Mẹo nhanh
                </h4>
                <div className="space-y-2">
                  {expert.quickTips.map((tip, i) => (
                    <motion.div
                      key={i}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.1 }}
                      className="p-3 rounded-lg bg-[rgba(255,107,53,0.05)] border border-[rgba(255,107,53,0.1)]"
                    >
                      <p className="text-[#F4A261] text-xs font-medium mb-1">{tip.title}</p>
                      <p className="text-gray-300 text-sm">{tip.content}</p>
                    </motion.div>
                  ))}
                </div>
              </div>

              {/* Articles with real links */}
              <div>
                <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                  <BookOpen className="w-4 h-4 text-[#FF6B35]" /> Bài viết & Nghiên cứu
                </h4>
                <div className="space-y-2">
                  {expert.articles.map((article, i) => (
                    <motion.a
                      key={i}
                      href={article.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 + i * 0.1 }}
                      className="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)] hover:border-[rgba(255,107,53,0.25)] transition-all cursor-pointer group block"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h5 className="text-white text-sm font-medium group-hover:text-[#F4A261] transition-colors flex items-center gap-2">
                            {article.title}
                            <ExternalLink className="w-3 h-3 text-gray-600 group-hover:text-[#F4A261] opacity-0 group-hover:opacity-100 transition-all" />
                          </h5>
                          <p className="text-gray-400 text-xs mt-1">{article.summary}</p>
                        </div>
                        <ChevronRight className="w-4 h-4 text-gray-600 group-hover:text-[#F4A261] transition-colors flex-shrink-0 mt-0.5" />
                      </div>
                      <div className="flex items-center gap-1 mt-2 text-[10px] text-gray-500">
                        <Clock className="w-3 h-3" />
                        <span>Đọc trong {article.readTime}</span>
                      </div>
                    </motion.a>
                  ))}
                </div>
              </div>

              {/* Contact Info */}
              <div className="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)]">
                <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                  <Mail className="w-4 h-4 text-[#F4A261]" /> Liên hệ
                </h4>
                <div className="space-y-2">
                  <a 
                    href={`mailto:${expert.email}`}
                    className="flex items-center gap-2 text-sm text-gray-300 hover:text-[#F4A261] transition-colors group"
                  >
                    <Mail className="w-4 h-4 text-gray-500 group-hover:text-[#F4A261]" />
                    {expert.email}
                  </a>
                  <a 
                    href={`tel:${expert.phone.replace(/\s/g, '')}`}
                    className="flex items-center gap-2 text-sm text-gray-300 hover:text-[#F4A261] transition-colors group"
                  >
                    <Phone className="w-4 h-4 text-gray-500 group-hover:text-[#F4A261]" />
                    {expert.phone}
                  </a>
                </div>
              </div>

              {/* Contact CTA */}
              <motion.a
                href={`mailto:${expert.email}?subject=Tư vấn nông nghiệp - GreenHouse`}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="w-full py-3 rounded-xl flex items-center justify-center gap-2 text-sm font-medium transition-all"
                style={{
                  background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.15), rgba(244, 162, 97, 0.15))',
                  border: '1px solid rgba(255, 107, 53, 0.3)',
                  color: '#F4A261',
                }}
              >
                <Shield className="w-4 h-4" />
                Tư vấn cùng {expert.name.split(' ')[0]}
              </motion.a>
            </div>
          </motion.div>
        </motion.div>
      )}
    </>
  );
}

export function ExpertProfiles() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h2 className="text-2xl font-bold text-white flex items-center gap-2">
          <Award className="w-6 h-6 text-[#ffd000]" style={{ filter: 'drop-shadow(0 0 4px rgba(255, 208, 0, 0.3))' }} />
          Mạng lưới chuyên gia
        </h2>
        <p className="text-gray-400 text-sm mt-1">
          Kết nối với các chuyên gia nông nghiệp được hỗ trợ bởi AI để nhận hướng dẫn cá nhân hóa
        </p>
      </motion.div>

      {/* Expert Cards */}
      <div className="space-y-4">
        {experts.map((expert, index) => (
          <ExpertCard key={expert.id} expert={expert} index={index} />
        ))}
      </div>
    </div>
  );
}