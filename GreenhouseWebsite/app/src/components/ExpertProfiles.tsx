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

  const skillColors = ['#E85D2B', '#D4893F', '#B8892D', '#C75A3E', '#8B6F5E'];

  return (
    <>
      <motion.div
        ref={cardRef}
        style={{ opacity, x, scale }}
        className="glass-card p-6 cursor-pointer group"
        onClick={() => setShowModal(true)}
        whileHover={{ y: -5 }}
      >
        <div 
          className="h-1 w-full mb-5 rounded-full"
          style={{ 
            background: `linear-gradient(90deg, ${skillColors[index % skillColors.length]}, transparent)`,
          }}
        />

        <div className="flex gap-5">
          <motion.div
            whileHover={{ scale: 1.05, rotate: 5 }}
            className="w-20 h-20 rounded-2xl flex-shrink-0 relative overflow-hidden"
            style={{
              border: `1px solid ${skillColors[index % skillColors.length]}40`,
              boxShadow: `0 0 15px ${skillColors[index % skillColors.length]}15`,
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
                <h3 className="text-[#2D1F1A] font-bold text-lg group-hover:text-[#E85D2B] transition-colors">
                  {expert.name}
                </h3>
                <p className="text-[#D4893F] text-sm">{expert.title}</p>
                <p className="text-[#A89080] text-xs mt-0.5">{expert.specialty}</p>
              </div>
              <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[rgba(232,93,43,0.06)] border border-[rgba(232,93,43,0.12)]">
                <Star className="w-3 h-3 text-[#B8892D]" />
                <span className="text-[10px] text-[#D4893F] font-medium">{expert.yearsExperience} năm</span>
              </div>
            </div>

            <p className="text-[#6B5B4F] text-sm mt-3 line-clamp-2">{expert.bio}</p>

            <div className="flex flex-wrap gap-1.5 mt-3">
              {expert.skills.map((skill, i) => (
                <span
                  key={i}
                  className="text-[10px] px-2 py-0.5 rounded-full border"
                  style={{
                    color: skillColors[i % skillColors.length],
                    borderColor: `${skillColors[i % skillColors.length]}30`,
                    background: `${skillColors[i % skillColors.length]}08`,
                  }}
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-[rgba(232,93,43,0.06)]">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-3.5 h-3.5 text-[#B8892D]" />
            <span className="text-xs text-[#A89080]">Mẹo nhanh</span>
          </div>
          <p className="text-sm text-[#5C4A3D] italic">"{expert.quickTips[0].content}"</p>
        </div>
      </motion.div>

      {showModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[100] flex items-center justify-center p-4 overflow-y-auto"
          style={{ background: 'rgba(255, 248, 240, 0.9)', backdropFilter: 'blur(12px)' }}
          onClick={() => setShowModal(false)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 40 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 40 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            onClick={e => e.stopPropagation()}
            className="glass-card w-full max-w-2xl my-8"
            style={{ border: '1px solid rgba(232, 93, 43, 0.12)' }}
          >
            <div className="relative p-6 border-b border-[rgba(232,93,43,0.08)]">
              <div 
                className="absolute top-0 left-0 right-0 h-1"
                style={{ 
                  background: `linear-gradient(90deg, ${skillColors[index % skillColors.length]}, transparent)`,
                }}
              />
              <button 
                onClick={() => setShowModal(false)}
                className="absolute top-4 right-4 w-8 h-8 rounded-lg flex items-center justify-center hover:bg-[rgba(139,69,19,0.08)] transition-colors"
              >
                <X className="w-4 h-4 text-[#8B6F5E]" />
              </button>

              <div className="flex items-center gap-4">
                <div 
                  className="w-16 h-16 rounded-2xl overflow-hidden"
                  style={{
                    border: `1px solid ${skillColors[index % skillColors.length]}40`,
                    boxShadow: `0 0 15px ${skillColors[index % skillColors.length]}15`,
                  }}
                >
                  <img 
                    src={`/experts/${expert.id.replace('expert-', '')}.jpg`}
                    alt={expert.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-[#2D1F1A]">{expert.name}</h2>
                  <p className="text-[#D4893F]">{expert.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <Award className="w-3 h-3 text-[#B8892D]" />
                    <span className="text-[10px] text-[#A89080]">{expert.yearsExperience} năm kinh nghiệm</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="p-6 space-y-6 max-h-[70vh] overflow-y-auto custom-scrollbar">
              <div>
                <h4 className="text-[#2D1F1A] font-semibold mb-2 flex items-center gap-2">
                  <BookOpen className="w-4 h-4 text-[#D4893F]" /> Giới thiệu
                </h4>
                <p className="text-[#5C4A3D] text-sm leading-relaxed">{expert.bio}</p>
              </div>

              <div>
                <h4 className="text-[#2D1F1A] font-semibold mb-3 flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#B8892D]" /> Chuyên môn
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
                        background: `${skillColors[i % skillColors.length]}06`,
                        border: `1px solid ${skillColors[i % skillColors.length]}18`,
                      }}
                    >
                      <Heart className="w-3 h-3" style={{ color: skillColors[i % skillColors.length] }} />
                      <span className="text-sm text-[#4A3B32]">{skill}</span>
                    </motion.div>
                  ))}
                </div>
              </div>

              <div>
                <h4 className="text-[#2D1F1A] font-semibold mb-3 flex items-center gap-2">
                  <Flame className="w-4 h-4 text-[#C75A3E]" /> Mẹo nhanh
                </h4>
                <div className="space-y-2">
                  {expert.quickTips.map((tip, i) => (
                    <motion.div
                      key={i}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.1 }}
                      className="p-3 rounded-lg bg-[rgba(232,93,43,0.04)] border border-[rgba(232,93,43,0.08)]"
                    >
                      <p className="text-[#D4893F] text-xs font-medium mb-1">{tip.title}</p>
                      <p className="text-[#5C4A3D] text-sm">{tip.content}</p>
                    </motion.div>
                  ))}
                </div>
              </div>

              <div>
                <h4 className="text-[#2D1F1A] font-semibold mb-3 flex items-center gap-2">
                  <BookOpen className="w-4 h-4 text-[#E85D2B]" /> Bài viết & Nghiên cứu
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
                      className="p-4 rounded-xl bg-[rgba(139,69,19,0.03)] border border-[rgba(139,69,19,0.06)] hover:border-[rgba(232,93,43,0.2)] transition-all cursor-pointer group block"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h5 className="text-[#2D1F1A] text-sm font-medium group-hover:text-[#D4893F] transition-colors flex items-center gap-2">
                            {article.title}
                            <ExternalLink className="w-3 h-3 text-[#A89080] group-hover:text-[#D4893F] opacity-0 group-hover:opacity-100 transition-all" />
                          </h5>
                          <p className="text-[#6B5B4F] text-xs mt-1">{article.summary}</p>
                        </div>
                        <ChevronRight className="w-4 h-4 text-[#A89080] group-hover:text-[#D4893F] transition-colors flex-shrink-0 mt-0.5" />
                      </div>
                      <div className="flex items-center gap-1 mt-2 text-[10px] text-[#A89080]">
                        <Clock className="w-3 h-3" />
                        <span>Đọc trong {article.readTime}</span>
                      </div>
                    </motion.a>
                  ))}
                </div>
              </div>

              <div className="p-4 rounded-xl bg-[rgba(139,69,19,0.03)] border border-[rgba(139,69,19,0.06)]">
                <h4 className="text-[#2D1F1A] font-semibold mb-3 flex items-center gap-2">
                  <Mail className="w-4 h-4 text-[#D4893F]" /> Liên hệ
                </h4>
                <div className="space-y-2">
                  <a 
                    href={`mailto:${expert.email}`}
                    className="flex items-center gap-2 text-sm text-[#5C4A3D] hover:text-[#D4893F] transition-colors group"
                  >
                    <Mail className="w-4 h-4 text-[#A89080] group-hover:text-[#D4893F]" />
                    {expert.email}
                  </a>
                  <a 
                    href={`tel:${expert.phone.replace(/\s/g, '')}`}
                    className="flex items-center gap-2 text-sm text-[#5C4A3D] hover:text-[#D4893F] transition-colors group"
                  >
                    <Phone className="w-4 h-4 text-[#A89080] group-hover:text-[#D4893F]" />
                    {expert.phone}
                  </a>
                </div>
              </div>

              <motion.a
                href={`mailto:${expert.email}?subject=Tư vấn nông nghiệp - GreenHouse`}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="w-full py-3 rounded-xl flex items-center justify-center gap-2 text-sm font-medium transition-all"
                style={{
                  background: 'linear-gradient(135deg, rgba(232, 93, 43, 0.1), rgba(212, 137, 63, 0.1))',
                  border: '1px solid rgba(232, 93, 43, 0.25)',
                  color: '#D4893F',
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
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h2 className="text-2xl font-bold text-[#2D1F1A] flex items-center gap-2">
          <Award className="w-6 h-6 text-[#B8892D]" style={{ filter: 'drop-shadow(0 0 4px rgba(184, 137, 45, 0.2))' }} />
          Mạng lưới chuyên gia
        </h2>
        <p className="text-[#6B5B4F] text-sm mt-1">
          Kết nối với các chuyên gia nông nghiệp được hỗ trợ bởi AI để nhận hướng dẫn cá nhân hóa
        </p>
      </motion.div>

      <div className="space-y-4">
        {experts.map((expert, index) => (
          <ExpertCard key={expert.id} expert={expert} index={index} />
        ))}
      </div>
    </div>
  );
}