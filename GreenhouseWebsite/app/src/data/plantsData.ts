export interface PlantThresholds {
  tempMin: number;
  tempMax: number;
  tempOptimal: number;
  soilMoistureMin: number;
  soilMoistureMax: number;
  soilMoistureOptimal: number;
  airHumidityMin: number;
  airHumidityMax: number;
  airHumidityOptimal: number;
  lightMin: number;
  lightMax: number;
  lightOptimal: number;
}

export interface Disease {
  name: string;
  symptoms: string;
  cause: string;
  treatment: string[];
  prevention: string[];
}

export interface GrowthStage {
  stage: string;
  duration: string;
  tempRange: string;
  humidityRange: string;
  lightRequirement: string;
  description: string;
}

export interface QuickTip {
  title: string;
  content: string;
  icon: string;
}

export interface Plant {
  id: string;
  name: string;
  scientificName: string;
  icon: string;
  color: string;
  neonColor: string;
  gradientFrom: string;
  gradientTo: string;
  imagePrompt: string;
  description: string;
  thresholds: PlantThresholds;
  diseases: Disease[];
  growthStages: GrowthStage[];
  quickTips: QuickTip[];
  actionableInsights: { condition: string; action: string; severity: 'low' | 'medium' | 'high' | 'critical' }[];
}

export interface Expert {
  id: string;
  name: string;
  title: string;
  specialty: string;
  plantId: string;
  avatarPrompt: string;
  avatarUrl?: string;
  bio: string;
  skills: string[];
  yearsExperience: number;
  quickTips: QuickTip[];
  articles: { title: string; summary: string; readTime: string; url: string }[];
  email: string;
  phone: string;
}

export const plants: Plant[] = [
  {
    id: 'chili',
    name: 'Ớt',
    scientificName: 'Capsicum annuum',
    icon: 'Flame',
    color: '#ef4444',
    neonColor: '#ff4444',
    gradientFrom: '#7f1d1d',
    gradientTo: '#dc2626',
    imagePrompt: 'A vibrant chili pepper plant with red and green peppers in a modern greenhouse, dramatic lighting, photorealistic, 8k quality',
    description: 'Ớt là cây thân thảo nhiệt đới lâu năm, phát triển tốt trong điều kiện ấm áp và ẩm ướt. Cần duy trì nhiệt độ ổn định và kiểm soát độ ẩm chặt chẽ để đạt năng suất quả cao nhất.',
    thresholds: {
      tempMin: 15,
      tempMax: 30,
      tempOptimal: 22,
      soilMoistureMin: 50,
      soilMoistureMax: 80,
      soilMoistureOptimal: 65,
      airHumidityMin: 40,
      airHumidityMax: 70,
      airHumidityOptimal: 55,
      lightMin: 20000,
      lightMax: 50000,
      lightOptimal: 35000,
    },
    diseases: [
      {
        name: 'Rệp vảy',
        symptoms: 'Côn trùng nhỏ mềm ở mặt dưới lá, lá chuyển vàng, nhựa cây dính, nấm muội đen',
        cause: 'Myzus persicae và các loài rệp khác',
        treatment: ['Phun nước áp lực mạnh để rửa trôi rệp', 'Phun dầu neem hoặc xà phòng diệt côn trùng', 'Thả bọ rùa làm kẻ thù tự nhiên', 'Cắt tỉa lá nhiễm nặng'],
        prevention: ['Dùng mùn phủ phản quang', 'Đảm bảo thông gió tốt', 'Kiểm tra định kỳ mặt dưới lá', 'Duy trì khoảng cách trồng hợp lý'],
      },
      {
        name: 'Bệnh phấn trắng',
        symptoms: 'Đốm trắng như bột trên lá, lá quăn queo, cây còi cọc',
        cause: 'Nấm Leveillula taurica',
        treatment: ['Phun thuốc trừ nấm chứa lưu huỳnh', 'Dùng dầu neem cho bệnh nhẹ', 'Loại bỏ lá nhiễm nặng', 'Cải thiện tuần hoàn không khí'],
        prevention: ['Trồng nơi có nắng', 'Tránh tưới trên lá', 'Đảm bảo khoảng cách trồng', 'Chọn giống kháng bệnh'],
      },
      {
        name: 'Thối rễ',
        symptoms: 'Cây héo dù đất ẩm, lá dưới chuyển vàng, rễ nâu/đen',
        cause: 'Tưới quá nhiều và nấm bệnh (Phytophthora, Pythium)',
        treatment: ['Giảm tần suất tưới', 'Cải thiện thoát nước', 'Tưới thuốc trừ nấm qua đất', 'Nhổ bỏ cây bị nặng'],
        prevention: ['Dùng đất thoát nước tốt', 'Tránh ngập úng', 'Lịch tưới hợp lý', 'Đảm bảo chậu/luống có lỗ thoát nước'],
      },
    ],
    growthStages: [
      { stage: 'Nảy mầm', duration: '7-14 ngày', tempRange: '25-30°C', humidityRange: '70-80%', lightRequirement: 'Ít', description: 'Hạt cần ấm và ẩm để nảy mầm' },
      { stage: 'Cây con', duration: '2-4 tuần', tempRange: '16-18°C', humidityRange: '60-70%', lightRequirement: 'Sáng 14-16h', description: 'Phát triển lá thật và bộ rễ' },
      { stage: 'Sinh trưởng', duration: '4-8 tuần', tempRange: '18-24°C', humidityRange: '50-65%', lightRequirement: 'Đủ sáng 16h', description: 'Lá và thân phát triển nhanh' },
      { stage: 'Ra hoa', duration: '2-4 tuần', tempRange: '20-26°C', humidityRange: '50-60%', lightRequirement: 'Đủ sáng 16h', description: 'Hình thành nụ và bắt đầu ra hoa' },
      { stage: 'Đậu quả', duration: '6-10 tuần', tempRange: '20-28°C', humidityRange: '45-60%', lightRequirement: 'Đủ sáng 14-16h', description: 'Quả phát triển và chín' },
    ],
    quickTips: [
      { title: 'Hỗ trợ thụ phấn', content: 'Gõ nhẹ hoa để hỗ trợ thụ phấn trong nhà', icon: 'Wind' },
      { title: 'Phân bón', content: 'Dùng phân kali cao hàng tuần trong giai đoạn ra hoa', icon: 'Zap' },
      { title: 'Tỉa cành', content: 'Bấm hoa sớm để cây bụi rậm hơn', icon: 'Scissors' },
    ],
    actionableInsights: [
      { condition: 'temp > 30', action: 'Bật quạt thông gió và che nắng ngay lập tức!', severity: 'critical' },
      { condition: 'temp < 15', action: 'Bật sưởi và đóng cửa thông gió', severity: 'critical' },
      { condition: 'soilMoisture < 50', action: 'Tăng tần suất tưới - đất quá khô', severity: 'high' },
      { condition: 'soilMoisture > 80', action: 'Giảm tưới - nguy cơ thối rễ!', severity: 'high' },
      { condition: 'airHumidity < 40', action: 'Phun sương hoặc dùng khay độ ẩm', severity: 'medium' },
      { condition: 'airHumidity > 70', action: 'Tăng thông gió để giảm độ ẩm', severity: 'medium' },
    ],
  },
  {
    id: 'cucumber',
    name: 'Dưa chuột',
    scientificName: 'Cucumis sativus',
    icon: 'CucumberIcon',
    color: '#22c55e',
    neonColor: '#44ff66',
    gradientFrom: '#14532d',
    gradientTo: '#16a34a',
    imagePrompt: 'Lush cucumber vines with bright green cucumbers hanging, greenhouse setting, fresh and vibrant, photorealistic, 8k quality',
    description: 'Dưa chuột là cây leo sinh trưởng nhanh, thích ánh sáng mạnh, nhiệt độ và độ ẩm cao. Giống nhà kính có thể đậu quả không cần thụ phấn.',
    thresholds: {
      tempMin: 18,
      tempMax: 27,
      tempOptimal: 24,
      soilMoistureMin: 60,
      soilMoistureMax: 85,
      soilMoistureOptimal: 75,
      airHumidityMin: 60,
      airHumidityMax: 85,
      airHumidityOptimal: 70,
      lightMin: 25000,
      lightMax: 60000,
      lightOptimal: 40000,
    },
    diseases: [
      {
        name: 'Bệnh phấn trắng',
        symptoms: 'Mảng trắng như bột trên lá, lá vàng và quăn',
        cause: 'Nấm Sphaerotheca fuliginea',
        treatment: ['Phun lưu huỳnh hoặc kali bicarbonate', 'Loại bỏ lá nhiễm nặng', 'Dùng dầu khoáng cho bệnh vừa', 'Phun thuốc trừ nấm ngay khi phát hiện'],
        prevention: ['Chọn giống kháng phấn trắng', 'Thông gió tốt', 'Tránh tưới trên lá', 'Khoảng cách trồng hợp lý'],
      },
      {
        name: 'Sương mai',
        symptoms: 'Đốm vàng góc cạnh trên lá, lớp lông xám ở mặt dưới',
        cause: 'Pseudoperonospora cubensis (tảo)',
        treatment: ['Phun thuốc đồng', 'Dùng Trichoderma asperellum sinh học', 'Dọn sạch tàn dư cây bệnh', 'Cải thiện tuần hoàn không khí'],
        prevention: ['Dùng tưới nhỏ giọt thay tưới trên', 'Tưới vào buổi sáng sớm', 'Thông gió hợp lý', 'Phun thuốc phòng ngừa'],
      },
      {
        name: 'Virus khảm dưa chuột',
        symptoms: 'Lá loang lổ vàng-xanh, cây biến dạng, còi cọc',
        cause: 'Cucumber mosaic virus (CMV)',
        treatment: ['Không có thuốc chữa - nhổ bỏ cây bệnh', 'Kiểm soát rệp truyền bệnh', 'Dùng mùn phủ phản quang', 'Tiêu hủy cây bệnh'],
        prevention: ['Chọn giống kháng virus', 'Kiểm soát rệp', 'Luân canh cây trồng', 'Giữ nhà kính sạch cỏ dại'],
      },
    ],
    growthStages: [
      { stage: 'Nảy mầm', duration: '3-7 ngày', tempRange: '25-30°C', humidityRange: '80-90%', lightRequirement: 'Ít', description: 'Nảy mầm nhanh trong điều kiện ấm ẩm' },
      { stage: 'Cây con', duration: '2-3 tuần', tempRange: '20-25°C', humidityRange: '70-80%', lightRequirement: 'Sáng 14h', description: 'Phát triển lá thật' },
      { stage: 'Sinh trưởng', duration: '3-5 tuần', tempRange: '22-27°C', humidityRange: '65-75%', lightRequirement: 'Đủ sáng 16h', description: 'Dây leo phát triển và tán lá xanh tốt' },
      { stage: 'Ra hoa/Đậu quả', duration: '6-10 tuần', tempRange: '23-27°C', humidityRange: '60-70%', lightRequirement: 'Đủ sáng 16h', description: 'Bắt đầu cho quả liên tục' },
    ],
    quickTips: [
      { title: 'Làm giàn', content: 'Dẫn dây leo theo chiều dọc để tiết kiệm diện tích và thông thoáng', icon: 'ArrowUp' },
      { title: 'Thu hoạch', content: 'Hái dưa chuột 2-3 ngày/lần để kích thích ra quả nhiều hơn', icon: 'Hand' },
      { title: 'Độ ẩm', content: 'Đổ nước lên sàn nhà kính để tăng độ ẩm', icon: 'Droplets' },
    ],
    actionableInsights: [
      { condition: 'temp > 27', action: 'Tăng thông gió và che nắng', severity: 'high' },
      { condition: 'temp < 18', action: 'Bật sưởi - cây sẽ ngừng phát triển đáng kể', severity: 'critical' },
      { condition: 'airHumidity < 60', action: 'Phun sương hoặc làm ướt sàn nhà kính', severity: 'medium' },
      { condition: 'airHumidity > 85', action: 'Tăng thông gió - nguy cơ bệnh cao!', severity: 'high' },
      { condition: 'soilMoisture < 60', action: 'Tưới ngay - dưa chuột cần độ ẩm ổn định', severity: 'high' },
    ],
  },
  {
    id: 'herbs',
    name: 'Rau thơm',
    scientificName: 'Ocimum basilicum, Mentha spp.',
    icon: 'Leaf',
    color: '#84cc16',
    neonColor: '#aaff00',
    gradientFrom: '#365314',
    gradientTo: '#65a30d',
    imagePrompt: 'Beautiful fresh herbs - basil, mint, cilantro in hydroponic setup, vibrant green leaves, water droplets, photorealistic, 8k quality',
    description: 'Rau thơm như húng quế và bạc hà sinh trưởng nhanh, cây nhỏ gọn, lý tưởng cho hệ thống thủy canh. Cho thu hoạch liên tục với diện tích tối thiểu.',
    thresholds: {
      tempMin: 18,
      tempMax: 26,
      tempOptimal: 22,
      soilMoistureMin: 50,
      soilMoistureMax: 75,
      soilMoistureOptimal: 65,
      airHumidityMin: 40,
      airHumidityMax: 60,
      airHumidityOptimal: 50,
      lightMin: 15000,
      lightMax: 35000,
      lightOptimal: 25000,
    },
    diseases: [
      {
        name: 'Sương mai (húng quế)',
        symptoms: 'Vàng giữa gân lá, lớp lông xám ở mặt dưới',
        cause: 'Peronospora belbahrii',
        treatment: ['Loại bỏ lá bệnh ngay lập tức', 'Phun thuốc đồng', 'Cải thiện tuần hoàn không khí', 'Tránh tưới lên lá'],
        prevention: ['Chọn giống húng quế kháng bệnh', 'Thông gió tốt', 'Trồng đủ khoảng cách', 'Tưới vào gốc'],
      },
      {
        name: 'Thối rễ',
        symptoms: 'Cây héo, lá vàng, rễ nâu nhầy',
        cause: 'Pythium và Phytophthora trong điều kiện ngập úng',
        treatment: ['Thay nước thủy canh', 'Thêm hydrogen peroxide vào bể', 'Cắt rễ bị bệnh', 'Đảm bảo oxy hóa tốt'],
        prevention: ['Giữ hệ thống thủy canh sạch', 'Nhiệt độ nước < 22°C', 'Dùng sủi oxy', 'Thay nước định kỳ'],
      },
      {
        name: 'Phấn trắng (bạc hà)',
        symptoms: 'Lớp phủ trắng trên lá, cây biến dạng',
        cause: 'Erysiphe spp.',
        treatment: ['Phun dầu neem hoặc lưu huỳnh', 'Cắt tỉa cành bệnh', 'Cải thiện tuần hoàn không khí', 'Giảm độ ẩm xung quanh'],
        prevention: ['Trồng đủ khoảng cách để thông thoáng', 'Tránh tưới trên lá', 'Cắt tỉa định kỳ', 'Trồng nơi đủ sáng'],
      },
    ],
    growthStages: [
      { stage: 'Nảy mầm', duration: '5-10 ngày', tempRange: '21-24°C', humidityRange: '70-80%', lightRequirement: 'Ít', description: 'Hạt nảy trong điều kiện ấm ẩm' },
      { stage: 'Cây con', duration: '2-3 tuần', tempRange: '18-22°C', humidityRange: '60-70%', lightRequirement: 'Sáng 12-14h', description: 'Lá thật đầu tiên xuất hiện' },
      { stage: 'Sinh trưởng/Thu hoạch', duration: '8-12 tuần', tempRange: '18-24°C', humidityRange: '50-60%', lightRequirement: 'Đủ sáng 14h', description: 'Có thể thu hoạch liên tục' },
    ],
    quickTips: [
      { title: 'Thu hoạch', content: 'Thu hoạch từ trên xuống để cây bụi rậm hơn', icon: 'Scissors' },
      { title: 'Bấm nụ', content: 'Bấm nụ hoa để kéo dài thời gian thu lá', icon: 'Flower2' },
      { title: 'Kiểm soát bạc hà', content: 'Trồng bạc hà riêng chậu - nó lan rất nhanh!', icon: 'Container' },
    ],
    actionableInsights: [
      { condition: 'temp > 26', action: 'Tăng thông gió - rau thơm dễ ra hoa sớm khi nóng!', severity: 'high' },
      { condition: 'temp < 18', action: 'Húng quế nhạy cảm với lạnh - tăng nhiệt độ', severity: 'critical' },
      { condition: 'airHumidity > 60', action: 'Tăng thông gió - nguy cơ nấm bệnh', severity: 'medium' },
      { condition: 'soilMoisture < 50', action: 'Tưới ngay - rau thơm cần độ ẩm ổn định', severity: 'medium' },
    ],
  },
  {
    id: 'leafy-greens',
    name: 'Rau lá xanh',
    scientificName: 'Lactuca sativa, Spinacia oleracea',
    icon: 'Salad',
    color: '#10b981',
    neonColor: '#00ffaa',
    gradientFrom: '#064e3b',
    gradientTo: '#059669',
    imagePrompt: 'Beautiful lettuce and spinach plants in hydroponic greenhouse, vibrant green leaves, fresh and healthy, photorealistic, 8k quality',
    description: 'Rau lá xanh như xà lách và rau chân vịt sinh trưởng nhanh, cây lạnh, hoàn hảo cho sản xuất thủy canh nhà kính quanh năm.',
    thresholds: {
      tempMin: 10,
      tempMax: 24,
      tempOptimal: 18,
      soilMoistureMin: 60,
      soilMoistureMax: 85,
      soilMoistureOptimal: 75,
      airHumidityMin: 50,
      airHumidityMax: 70,
      airHumidityOptimal: 60,
      lightMin: 12000,
      lightMax: 30000,
      lightOptimal: 20000,
    },
    diseases: [
      {
        name: 'Sương mai',
        symptoms: 'Đốm vàng trên mặt trên lá, lớp lông xám ở mặt dưới',
        cause: 'Bremia lactucae (xà lách), Peronospora farinosa (rau chân vịt)',
        treatment: ['Loại bỏ lá bệnh', 'Phun Actinovate AG hoặc thuốc đồng', 'Cải thiện thông gió', 'Giảm độ ẩm'],
        prevention: ['Chọn giống kháng bệnh', 'Thông gió tốt', 'Tránh tưới trên lá', 'Trồng đủ khoảng cách'],
      },
      {
        name: 'Phấn trắng',
        symptoms: 'Đốm trắng trên lá, lá biến dạng',
        cause: 'Erysiphe cichoracearum',
        treatment: ['Phun lưu huỳnh hoặc dầu neem', 'Loại bỏ lá nhiễm nặng', 'Cải thiện tuần hoàn không khí', 'Giảm độ ẩm nếu cao'],
        prevention: ['Trồng nơi đủ sáng', 'Khoảng cách hợp lý', 'Thông gió tốt', 'Theo dõi định kỳ'],
      },
      {
        name: 'Cháy mép lá',
        symptoms: 'Mép lá nâu hoại tử',
        cause: 'Thiếu canxi kết hợp độ ẩm thấp',
        treatment: ['Kiểm tra canxi trong dung dịch dinh dưỡng', 'Tăng độ ẩm xung quanh', 'Tưới đều đặn', 'Bổ sung canxi'],
        prevention: ['Duy trì độ ẩm ổn định', 'Đảm bảo đủ canxi trong dinh dưỡng', 'Tránh biến động nhiệt độ đột ngột', 'Thông gió tốt nhưng không quá khô'],
      },
    ],
    growthStages: [
      { stage: 'Nảy mầm', duration: '2-4 ngày', tempRange: '18-20°C', humidityRange: '80-90%', lightRequirement: 'Ít sáng', description: 'Hạt nảy nhanh trong điều kiện mát ẩm' },
      { stage: 'Cây con', duration: '1-2 tuần', tempRange: '18-21°C', humidityRange: '70-80%', lightRequirement: 'Sáng 14-16h', description: 'Lá thật đầu tiên phát triển' },
      { stage: 'Sinh trưởng nhanh', duration: '2-3 tuần', tempRange: '16-21°C', humidityRange: '60-70%', lightRequirement: 'Đủ sáng 16h', description: 'Lá mở rộng nhanh - có thể thu lá non' },
      { stage: 'Trưởng thành/Thu hoạch', duration: '1-2 tuần', tempRange: '15-18°C', humidityRange: '55-65%', lightRequirement: 'Đủ sáng 14h', description: 'Bắp cải đạt kích thước thu hoạch' },
    ],
    quickTips: [
      { title: 'Trồng luân phiên', content: 'Gieo hạt mới 2 tuần/lần để thu hoạch liên tục', icon: 'Calendar' },
      { title: 'Thu lá ngoài', content: 'Hái lá ngoài trước để kéo dài thời gian thu hoạch', icon: 'Hand' },
      { title: 'Đêm mát', content: 'Đêm mát (13-16°C) giúp rau ngon và giòn hơn', icon: 'Thermometer' },
    ],
    actionableInsights: [
      { condition: 'temp > 24', action: 'Khẩn cấp! Xà lách sẽ ra hoa sớm - bật làm mát NGAY', severity: 'critical' },
      { condition: 'temp < 10', action: 'Sinh trưởng chậm lại đáng kể - tăng nhiệt độ', severity: 'high' },
      { condition: 'airHumidity < 50', action: 'Nguy cơ cháy mép lá - tăng độ ẩm', severity: 'medium' },
      { condition: 'light < 12000', action: 'Bổ sung đèn LED trồng cây', severity: 'medium' },
    ],
  },
  {
    id: 'strawberry',
    name: 'Dâu tây',
    scientificName: 'Fragaria x ananassa',
    icon: 'Cherry',
    color: '#ec4899',
    neonColor: '#ff66aa',
    gradientFrom: '#831843',
    gradientTo: '#db2777',
    imagePrompt: 'Beautiful strawberry plants with red ripe berries and white flowers, greenhouse setting, dew drops on berries, photorealistic, 8k quality',
    description: 'Dâu tây là cây lâu năm mỏng manh, đòi hỏi quản lý nhiệt độ chính xác qua các giai đoạn sinh trưởng khác nhau để đạt chất lượng quả tối ưu.',
    thresholds: {
      tempMin: 10,
      tempMax: 24,
      tempOptimal: 18,
      soilMoistureMin: 60,
      soilMoistureMax: 80,
      soilMoistureOptimal: 70,
      airHumidityMin: 60,
      airHumidityMax: 75,
      airHumidityOptimal: 68,
      lightMin: 20000,
      lightMax: 40000,
      lightOptimal: 30000,
    },
    diseases: [
      {
        name: 'Bệnh mốc xám (Botrytis)',
        symptoms: 'Nấm mốc xám-nâu trên quả, hoa và lá',
        cause: 'Nấm Botrytis cinerea',
        treatment: ['Loại bỏ toàn bộ bộ phận cây bệnh', 'Dùng Trichoderma harzianum sinh học', 'Dùng quạt để cải thiện tuần hoàn', 'Phun thuốc trừ nấm trong giai đoạn ra hoa'],
        prevention: ['Thông gió xuất sắc', 'Trồng đủ khoảng cách', 'Dùng tưới nhỏ giọt', 'Dọn lá và quả chết kịp thời'],
      },
      {
        name: 'Phấn trắng',
        symptoms: 'Lớp phủ trắng trên lá và quả',
        cause: 'Podosphaera aphanis',
        treatment: ['Phun lưu huỳnh hoặc kali bicarbonate', 'Loại bỏ lá nhiễm nặng', 'Cải thiện tuần hoàn không khí', 'Chọn giống kháng bệnh'],
        prevention: ['Thông gió tốt', 'Khoảng cách trồng hợp lý', 'Tránh tưới trên lá', 'Theo dõi độ ẩm'],
      },
      {
        name: 'Đốm lá',
        symptoms: 'Đốm tím đến nâu trên lá, lá vàng',
        cause: 'Mycosphaerella fragariae',
        treatment: ['Loại bỏ lá bệnh', 'Phun thuốc đồng', 'Cải thiện tuần hoàn không khí', 'Tránh làm ướt lá'],
        prevention: ['Dùng cây không bệnh', 'Khoảng cách hợp lý', 'Dọn sạch tàn dư', 'Luân canh cây trồng'],
      },
    ],
    growthStages: [
      { stage: 'Sinh trưởng', duration: '4-6 tuần', tempRange: '18-24°C ngày, 10-13°C đêm', humidityRange: '65-75%', lightRequirement: 'Đủ sáng 14h', description: 'Phát triển lá và thân' },
      { stage: 'Ra hoa', duration: '2-4 tuần', tempRange: '16-20°C ngày', humidityRange: '65-70%', lightRequirement: 'Đủ sáng 14-16h', description: 'Hình thành hoa và thụ phấn' },
      { stage: 'Đậu quả', duration: '2-3 tuần', tempRange: '15-20°C ngày, 8-12°C đêm', humidityRange: '60-70%', lightRequirement: 'Đủ sáng 14h', description: 'Quả bắt đầu phát triển' },
      { stage: 'Chín', duration: '2-4 tuần', tempRange: '18-22°C', humidityRange: '60-65%', lightRequirement: 'Đủ sáng 12h', description: 'Tích lũy đường và phát triển màu' },
    ],
    quickTips: [
      { title: 'Thụ phấn', content: 'Gõ nhẹ hoa hoặc dùng bút lông nhỏ để thụ phấn trong nhà', icon: 'Wind' },
      { title: 'Chồi rễ', content: 'Loại bỏ chồi rễ để tập trung năng lượng cho quả', icon: 'Scissors' },
      { title: 'Phủ mùn', content: 'Dùng màng phủ để giữ quả sạch và khô', icon: 'Layers' },
    ],
    actionableInsights: [
      { condition: 'temp > 24', action: 'KHẨN CẤP: Chất lượng quả giảm - bật làm mát!', severity: 'critical' },
      { condition: 'temp < 10', action: 'Sinh trưởng ngừng - tăng nhiệt độ', severity: 'high' },
      { condition: 'airHumidity > 75', action: 'Nguy cơ mốc xám CAO - tăng thông gió!', severity: 'critical' },
      { condition: 'airHumidity < 60', action: 'Tăng độ ẩm để quả phát triển tốt hơn', severity: 'medium' },
      { condition: 'soilMoisture > 80', action: 'Giảm tưới - nguy cơ thối rễ và mốc xám', severity: 'high' },
    ],
  },
  {
    id: 'tomato',
    name: 'Cà chua',
    scientificName: 'Solanum lycopersicum',
    icon: 'Apple',
    color: '#f97316',
    neonColor: '#ff8833',
    gradientFrom: '#7c2d12',
    gradientTo: '#ea580c',
    imagePrompt: 'Healthy tomato plants with red ripe tomatoes and yellow flowers, greenhouse with trellis, vibrant colors, photorealistic, 8k quality',
    description: 'Cà chua là cây trồng nhà kính phổ biến nhất, đòi hỏi sự cân bằng cẩn thận giữa nhiệt độ, độ ẩm và dinh dưỡng để cho năng suất quả dồi dào.',
    thresholds: {
      tempMin: 16,
      tempMax: 27,
      tempOptimal: 22,
      soilMoistureMin: 55,
      soilMoistureMax: 80,
      soilMoistureOptimal: 68,
      airHumidityMin: 60,
      airHumidityMax: 85,
      airHumidityOptimal: 70,
      lightMin: 25000,
      lightMax: 60000,
      lightOptimal: 40000,
    },
    diseases: [
      {
        name: 'Thối đen đầu quả',
        symptoms: 'Vết thối đen lõm ở đầu quả',
        cause: 'Thiếu canxi do tưới không đều',
        treatment: ['Bổ sung canxi vào đất ngay', 'Đảm bảo độ ẩm đất ổn định', 'Thêm vôi nếu pH thấp', 'Loại bỏ quả bị bệnh'],
        prevention: ['Duy trì lịch tưới đều đặn', 'Phủ mùn xung quanh gốc', 'Giữ pH đất 6.5', 'Đảm bảo đủ canxi trong đất'],
      },
      {
        name: 'Bệnh cháy lá sớm',
        symptoms: 'Đốm nâu có vòng đồng tâm trên lá già, lá vàng',
        cause: 'Nấm Alternaria linariae',
        treatment: ['Loại bỏ lá già bệnh', 'Phun thuốc đồng hoặc mancozeb', 'Cải thiện tuần hoàn không khí', 'Tránh làm ướt lá'],
        prevention: ['Chọn giống kháng bệnh', 'Luân canh cây trồng', 'Trồng đủ khoảng cách', 'Phủ mùn tránh bắn đất'],
      },
      {
        name: 'Héo Fusarium',
        symptoms: 'Héo một bên cây, lá vàng, mạch dẫn đổi màu',
        cause: 'Nấm Fusarium oxysporum',
        treatment: ['Không có thuốc chữa - nhổ bỏ cây bệnh', 'Điều chỉnh pH đất 6.5-7.0', 'Dùng phân đạm nitrate', 'Phơi đất giữa các vụ'],
        prevention: ['Chọn giống kháng Fusarium (FF)', 'Trồng trong đất sạch bệnh', 'Duy trì pH hợp lý', 'Luân canh cây trồng'],
      },
      {
        name: 'Bệnh cháy lá muộn',
        symptoms: 'Đốm thối đen trên lá, nấm trắng ở mặt dưới',
        cause: 'Phytophthora infestans',
        treatment: ['Nhổ bỏ cây bệnh ngay lập tức', 'Phun thuốc đồng', 'Cải thiện tuần hoàn không khí', 'Giảm độ ẩm'],
        prevention: ['Chọn giống kháng bệnh', 'Đảm bảo thoát nước tốt', 'Tránh tưới trên lá', 'Theo dõi thời tiết'],
      },
    ],
    growthStages: [
      { stage: 'Nảy mầm', duration: '5-10 ngày', tempRange: '21-27°C', humidityRange: '80-90%', lightRequirement: 'Không cần', description: 'Hạt cần ấm để nảy mầm' },
      { stage: 'Cây con', duration: '3-5 tuần', tempRange: '18-21°C', humidityRange: '70-80%', lightRequirement: 'Sáng 16h', description: 'Phát triển lá thật và thân' },
      { stage: 'Sinh trưởng', duration: '4-6 tuần', tempRange: '20-24°C', humidityRange: '65-75%', lightRequirement: 'Đủ sáng 16h', description: 'Dây leo phát triển nhanh' },
      { stage: 'Ra hoa', duration: '2-4 tuần', tempRange: '20-26°C', humidityRange: '65-75%', lightRequirement: 'Đủ sáng 16h', description: 'Cụm hoa hình thành' },
      { stage: 'Đậu quả', duration: '8-12 tuần', tempRange: '20-27°C', humidityRange: '60-70%', lightRequirement: 'Đủ sáng 14h', description: 'Quả phát triển và chín' },
    ],
    quickTips: [
      { title: 'Làm giàn', content: 'Dùng cọc hoặc giàn để đỡ dây nặng', icon: 'ArrowUp' },
      { title: 'Tỉa nhánh', content: 'Loại bỏ nhánh phụ để thông thoáng hơn', icon: 'Scissors' },
      { title: 'Phân bón', content: 'Dùng phân cân bằng, giảm đạm khi đậu quả', icon: 'Zap' },
    ],
    actionableInsights: [
      { condition: 'temp > 27', action: 'Nguy cơ rụng hoa! Bật làm mát ngay lập tức!', severity: 'critical' },
      { condition: 'temp < 16', action: 'Sinh trưởng chậm đáng kể - tăng nhiệt độ', severity: 'high' },
      { condition: 'airHumidity > 85', action: 'Nguy cơ nấm bệnh CAO - tăng thông gió!', severity: 'critical' },
      { condition: 'airHumidity < 60', action: 'Tăng độ ẩm để thụ phấn tốt hơn', severity: 'medium' },
      { condition: 'soilMoisture < 55', action: 'Tưới ngay - nguy cơ thối đen đầu quả!', severity: 'high' },
    ],
  },
];

export const experts: Expert[] = [
  {
    id: 'expert-chili',
    name: 'TS. Elena Volkov',
    title: 'Chuyên gia Ớt',
    specialty: 'Trồng Capsicum',
    plantId: 'chili',
    avatarPrompt: 'Professional female agronomist in her 40s, confident smile, wearing a white lab coat, holding a chili pepper, modern greenhouse background, warm lighting, photorealistic portrait',
    bio: 'TS. Elena Volkov đã dành 15 năm nghiên cứu loài Capsicum tại các vùng nhiệt đới. Bà có bằng tiến sĩ Khoa học Làm vườn từ Đại học Wageningen và đã xuất bản hơn 40 bài báo về tối ưu hóa trồng ớt.',
    skills: ['Sinh lý Ớt', 'Quản lý Stress Nhiệt', 'Quản lý Dịch hại Tổng hợp', 'Ớt Thủy canh'],
    yearsExperience: 15,
    quickTips: [
      { title: 'Bí mật Scoville', content: 'Gây stress nhẹ cho cây (hạn kiểm soát) để tăng sản xuất capsaicin', icon: 'Flame' },
      { title: 'Nhiệt độ Đêm', content: 'Giữ nhiệt độ đêm trên 15°C để đậu quả liên tục', icon: 'Thermometer' },
      { title: 'Thụ phấn', content: 'Rung giàn hàng ngày để cải thiện thụ phấn trong không gian kín', icon: 'Vibrate' },
    ],
    articles: [
      { title: 'Tối đa hóa Capsaicin: Khoa học của Cay', summary: 'Hiểu cách các yếu tố môi trường ảnh hưởng đến sản xuất hợp chất cay trong ớt.', readTime: '5 phút', url: 'https://pmc.ncbi.nlm.nih.gov/articles/PMC8309139/' },
      { title: 'Hướng dẫn Sản xuất Ớt Quanh năm', summary: 'Kỹ thuật duy trì thu hoạch liên tục trong môi trường kiểm soát.', readTime: '8 phút', url: 'https://www.sciencedirect.com/science/article/pii/S0304423818300341' },
      { title: 'Giống Kháng bệnh 2026', summary: 'Các giống mới nhất với kháng bệnh phấn trắng và thối rễ cải thiện.', readTime: '4 phút', url: 'https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2021.634534' },
    ],
    email: 'e.volkov@wur.nl',
    phone: '+31 317 480 000',
  },
  {
    id: 'expert-cucumber',
    name: 'GS. James Chen',
    title: 'Chuyên gia Hệ thống Dưa chuột',
    specialty: 'Hệ thống Nhà kính Cucurbit',
    plantId: 'cucumber',
    avatarPrompt: 'Professional Asian male agronomist in his 50s, glasses, kind expression, wearing a green shirt with plant logo, standing in a cucumber greenhouse, natural lighting, photorealistic portrait',
    bio: 'GS. James Chen là chuyên gia hàng đầu về hệ thống sản xuất dưa chuột nhà kính. Với 20 năm kinh nghiệm, ông đã phát triển kỹ thuật làm giàn và kiểm soát khí hậu sáng tạo tăng năng suất 30%.',
    skills: ['Thiết kế Nhà kính', 'Dẫn dây Leo', 'Kiểm soát Độ ẩm', 'Sản xuất Quanh năm'],
    yearsExperience: 20,
    quickTips: [
      { title: 'Phương pháp Dù', content: 'Dẫn thân chính lên 2m rồi để 2-3 cành nhánh rũ xuống', icon: 'GitBranch' },
      { title: 'Tải quả', content: 'Duy trì 1 quả/lá để dưa chuột chất lượng cao', icon: 'Target' },
      { title: 'Nhiệt độ Nước', content: 'Giữ nước tưới ở 20-22°C để hấp thu dinh dưỡng tối ưu', icon: 'Droplets' },
    ],
    articles: [
      { title: 'Hướng dẫn Làm giàn Dưa chuột Toàn diện', summary: 'Các bước chi tiết dẫn dây leo theo chiều dọc trong nhà kính.', readTime: '6 phút', url: 'https://www.mdpi.com/2223-7747/14/9/1285' },
      { title: 'Quản lý Độ ẩm cho Phòng bệnh', summary: 'Cách cân bằng nhu cầu độ ẩm cao với nguy cơ bệnh trong sản xuất dưa chuột.', readTime: '7 phút', url: 'https://www.mdpi.com/2223-7747/13/5/1087' },
      { title: 'Giống Đơn tính: Thành công Không hạt', summary: 'Tại sao giống tự thụ phấn đang cách mạng hóa trồng dưa chuột nhà kính.', readTime: '5 phút', url: 'https://www.sciencedirect.com/science/article/pii/S0304423818300341' },
    ],
    email: 'j.chen@zju.edu.cn',
    phone: '+86 571 8898 2000',
  },
  {
    id: 'expert-herbs',
    name: 'Maria Santos',
    title: 'Bậc thầy Trồng Rau thơm',
    specialty: 'Rau thơm Thơm & Ẩm thực',
    plantId: 'herbs',
    avatarPrompt: 'Young Latina woman herbalist in her 30s, warm smile, wearing an apron with herb pockets, surrounded by fresh basil and mint, greenhouse setting, soft natural light, photorealistic portrait',
    bio: 'Maria Santos lớn lên trong trang trại rau thơm ở Bồ Đào Nha và đã biến đam mê thành chuyên môn. Cô chuyên về hệ thống rau thơm thủy canh và đã giúp thiết lập hơn 100 vườn rau thơm đô thị trên toàn thế giới.',
    skills: ['Rau thơm Thủy canh', 'Sản xuất Tinh dầu', 'Kiểm soát Dịch hại Hữu cơ', 'Giống Ẩm thực'],
    yearsExperience: 12,
    quickTips: [
      { title: 'Thời điểm Thu hoạch', content: 'Thu hoạch húng quế ngay trước khi hoa nở để hương vị đậm nhất', icon: 'Clock' },
      { title: 'Sức khỏe Rễ', content: 'Thay nước thủy canh 7 ngày/lần để ngăn Pythium', icon: 'RefreshCw' },
      { title: 'Trồng Cặp', content: 'Trồng húng quế gần cà chua - chúng là bạn đồng hành tự nhiên', icon: 'Users' },
    ],
    articles: [
      { title: 'Bản thiết kế Vườn Rau thơm Đô thị', summary: 'Cách trồng 20+ loại rau thơm trong dưới 2 mét vuông.', readTime: '6 phút', url: 'https://www.mdpi.com/2311-7524/9/7/831' },
      { title: 'Húng quế: Từ Hạt đến Pesto trong 60 ngày', summary: 'Lộ trình hoàn chỉnh để sản xuất húng quế tối đa.', readTime: '5 phút', url: 'https://pmc.ncbi.nlm.nih.gov/articles/PMC11397607/' },
      { title: 'Tinh dầu: Trồng cho Hương vị', summary: 'Các yếu tố môi trường tối đa hóa sản xuất hợp chất thơm.', readTime: '7 phút', url: 'https://iris.unito.it/retrieve/handle/2318/2054730/1534207/2024_10.2478_fhort-2024-0034_NGS-basil.pdf' },
    ],
    email: 'm.santos@herbguru.pt',
    phone: '+351 21 000 0000',
  },
  {
    id: 'expert-leafy',
    name: 'TS. Aisha Patel',
    title: 'Nhà khoa học Rau lá xanh',
    specialty: 'Tối ưu hóa Xà lách & Rau chân vịt',
    plantId: 'leafy-greens',
    avatarPrompt: 'Professional Indian female scientist in her 40s, wearing a white lab coat and safety glasses, examining lettuce leaves under light, modern laboratory background, clean lighting, photorealistic portrait',
    bio: 'TS. Aisha Patel là nhà sinh lý học thực vật chuyên về trồng xà lách và rau chân vịt. Nghiên cứu của bà về giống chịu lạnh đã cho phép sản xuất rau lá xanh quanh năm ở khí hậu ôn đới.',
    skills: ['Sinh lý Thực vật', 'Chịu Lạnh', 'Hệ thống Thủy canh', 'Quản lý Dinh dưỡng'],
    yearsExperience: 18,
    quickTips: [
      { title: 'Thu hoạch 35 ngày', content: 'Trong điều kiện tối ưu, xà lách có thể từ hạt đến thu hoạch trong 35 ngày', icon: 'Timer' },
      { title: 'DLI Quan trọng', content: 'Duy trì DLI 17 mol/m²/ngày để kết quả tốt nhất', icon: 'Sun' },
      { title: 'Làm mát Đêm', content: 'Đêm mát (15°C) cải thiện kết cấu và hàm lượng đường', icon: 'Thermometer' },
    ],
    articles: [
      { title: 'Trồng nhanh: Xà lách 35 ngày', summary: 'Khoa học đằng sau sản xuất xà lách nhanh trong môi trường kiểm soát.', readTime: '6 phút', url: 'https://www.mdpi.com/2223-7747/12/3/463' },
      { title: 'Sản xuất Mùa đông Không cần Sưởi', summary: 'Sử dụng giống chịu lạnh để giảm chi phí sưởi nhà kính.', readTime: '8 phút', url: 'https://www.mdpi.com/2077-0472/11/11/1133' },
      { title: 'NFT vs DWC: So sánh Hệ thống Thủy canh', summary: 'So sánh các hệ thống thủy canh cho sản xuất rau lá xanh.', readTime: '7 phút', url: 'https://www.sciencedirect.com/science/article/pii/S0304423818300341' },
    ],
    email: 'a.patel@icar.gov.in',
    phone: '+91 11 2584 0000',
  },
  {
    id: 'expert-strawberry',
    name: 'TS. Hans Mueller',
    title: 'Chuyên gia Sản xuất Quả mọng',
    specialty: 'Khoa học Trồng Dâu tây',
    plantId: 'strawberry',
    avatarPrompt: 'Professional German male horticulturist in his 50s, wearing a vest over a checkered shirt, examining strawberry plants with a magnifying glass, greenhouse with berry rows, warm light, photorealistic portrait',
    bio: 'TS. Hans Mueller đã dành 25 năm sự nghiệp để hoàn thiện trồng dâu tây trong môi trường kiểm soát. Ông là người tiên phong hệ thống sản xuất "Trung tính ngày" cho phép ra quả liên tục quanh năm.',
    skills: ['Sinh lý Quả mọng', 'Quản lý Thụ phấn', 'Xử lý Sau thu hoạch', 'Chọn lọc Giống'],
    yearsExperience: 25,
    quickTips: [
      { title: 'Yêu cầu Làm lạnh', content: 'Dâu tây cần 200-300 giờ dưới 7°C để ra hoa đúng', icon: 'Snowflake' },
      { title: 'Quản lý Chồi rễ', content: 'Loại bỏ tất cả chồi rễ trong mùa ra quả để tăng 40% năng suất', icon: 'Scissors' },
      { title: 'Cửa sổ Thụ phấn', content: 'Thụ phấn hoa trong 3 ngày đầu để quả đẹp nhất', icon: 'Clock' },
    ],
    articles: [
      { title: 'Sản xuất Dâu tây Quanh năm', summary: 'Hệ thống trung tính ngày cho thu hoạch quả mọng liên tục.', readTime: '8 phút', url: 'https://www.sciencedirect.com/science/article/pii/S0304423818300341' },
      { title: 'Kỹ thuật Thụ phấn Hoàn hảo', summary: 'Các phương pháp thụ phấn thủ công và hỗ trợ cho dâu tây nhà kính.', readTime: '5 phút', url: 'https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2021.634534' },
      { title: 'Mốc xám: Phòng ngừa là Tất cả', summary: 'Chiến lược tổng hợp quản lý Botrytis trong sản xuất dâu tây.', readTime: '7 phút', url: 'https://pmc.ncbi.nlm.nih.gov/articles/PMC8309139/' },
    ],
    email: 'h.mueller@uni-hohenheim.de',
    phone: '+49 711 459 0',
  },
  {
    id: 'expert-tomato',
    name: 'TS. Sarah Johnson',
    title: 'Bậc thầy Trồng Cà chua',
    specialty: 'Sản xuất Cà chua Nhà kính',
    plantId: 'tomato',
    avatarPrompt: 'Professional female agronomist in her 40s, confident expression, wearing a denim shirt with tomato plant logo, standing in a tomato greenhouse with vines full of red fruit, golden hour lighting, photorealistic portrait',
    bio: 'TS. Sarah Johnson là một trong những chuyên gia trồng cà chua hàng đầu thế giới. Các phương pháp kiểm soát khí hậu và dinh dưỡng sáng tạo của bà đã đặt ra tiêu chuẩn mới cho sản xuất cà chua nhà kính.',
    skills: ['Kiểm soát Khí hậu', 'Quản lý Dinh dưỡng', 'Phòng bệnh', 'Tối ưu Năng suất'],
    yearsExperience: 22,
    quickTips: [
      { title: 'Điểm ngọt VPD', content: 'Duy trì VPD 0.5-0.8 kPa để sinh trưởng tối ưu', icon: 'Gauge' },
      { title: 'Tỉa lá', content: 'Loại bỏ lá dưới đến cụm quả đầu tiên để thông thoáng', icon: 'Scissors' },
      { title: 'Khắc phục Rụng hoa', content: 'Nếu hoa rụng, kiểm tra nhiệt độ đêm - nên là 16-18°C', icon: 'AlertTriangle' },
    ],
    articles: [
      { title: 'Thành thạo VPD: Bí mật Thành công Cà chua', summary: 'Hiểu và kiểm soát Độ chênh hơi nước để năng suất tối đa.', readTime: '7 phút', url: 'https://www.sciencedirect.com/science/article/pii/S0378377424002142' },
      { title: 'Ngăn chặn Thối đen đầu quả Mãi mãi', summary: 'Cách tưới đều đặn và quản lý canxi loại bỏ BER.', readTime: '6 phút', url: 'https://hero.epa.gov/reference/10779055/' },
      { title: 'Cây 50kg: Tối đa hóa Năng suất', summary: 'Kỹ thuật nâng cao để đạt năng suất cà chua kỷ lục mỗi cây.', readTime: '9 phút', url: 'https://www.ars.usda.gov/ARSUserFiles/57795/Shamshiri2018%20-%20review%20optimum%20microclimate%20greenhouse.pdf' },
    ],
    email: 's.johnson@ucdavis.edu',
    phone: '+1 530 752 0000',
  },
];

export const solarData = {
  panelVoltage: 18.5,
  panelCurrent: 4.2,
  batteryVoltage: 12.8,
  batteryPercentage: 85,
  batteryHealth: 'Tốt',
  dailyGeneration: 2.4,
  dailyConsumption: 1.8,
  efficiency: 92,
};