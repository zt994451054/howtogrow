
import React, { useState, useEffect } from 'react';
import { Child, ObservationRecord, BannerItem } from '../types';
import { MOCK_BANNERS } from '../constants';
import { Bars3Icon, ChevronRightIcon, PlusIcon } from './Icons';
import { ObservationDetail } from './ObservationDetail';
import { ArticleDetail } from './ArticleDetail';
import { MOOD_DATA, MoodFace } from './MoodResources';

interface HomeObservationProps {
  childrenData: Child[];
  selectedChildId: string | null;
  onSelectChild: (id: string) => void;
  onNavigate: (tab: string, params?: Record<string, any>) => void;
  initialParams?: Record<string, any> | null;
  onClearParams?: () => void;
}

export const HomeObservation: React.FC<HomeObservationProps> = ({ 
  childrenData, 
  selectedChildId, 
  onSelectChild, 
  onNavigate,
  initialParams,
  onClearParams
}) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [activeBannerIndex, setActiveBannerIndex] = useState(0);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [observations, setObservations] = useState<ObservationRecord[]>([]);
  
  // State for showing the detail view
  const [selectedObservation, setSelectedObservation] = useState<ObservationRecord | null>(null);
  // State to trigger auto-open of mood selector in Detail View
  const [autoOpenSelector, setAutoOpenSelector] = useState<'mood' | null>(null);
  
  // State for showing the banner article view
  const [selectedBanner, setSelectedBanner] = useState<BannerItem | null>(null);

  const selectedChild = childrenData.find(c => c.id === selectedChildId) || childrenData[0];
  const formattedMonth = `${currentDate.getFullYear()}年 ${currentDate.getMonth() + 1}月`;
  const dateInputStr = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;
  
  // Constrain max date to current month
  const today = new Date();
  const maxMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;

  // --- 1. Banner Auto-Rotation ---
  useEffect(() => {
    // Only auto-rotate if no banner is selected (active viewing)
    if (selectedBanner) return;

    const interval = setInterval(() => {
      setActiveBannerIndex(prev => (prev + 1) % MOCK_BANNERS.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [selectedBanner]);

  // --- 2. Generate Observations for Month (Initial Load) ---
  useEffect(() => {
    // Determine the number of days to render
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    // Check if the selected month is the current real-world month
    const isCurrentRealMonth = year === today.getFullYear() && month === today.getMonth();
    
    // If it's the current month, only render up to today. Otherwise render full month.
    const daysInMonth = isCurrentRealMonth 
        ? today.getDate() 
        : new Date(year, month + 1, 0).getDate();

    const newObservations: ObservationRecord[] = [];
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    
    // Get available mood IDs
    const moodIds = MOOD_DATA.map(m => m.id);

    // Loop backwards from latest day to 1st day
    for (let i = daysInMonth; i >= 1; i--) {
        const dateObj = new Date(year, month, i);
        const dayOfWeek = dateObj.getDay();
        
        // Mock logic: randomly decide if done, mood, content
        const isPast = dateObj < new Date();
        const isDone = isPast && Math.random() > 0.4; 
        
        // Pick a random mood ID if done
        const mood = isDone ? moodIds[Math.floor(Math.random() * moodIds.length)] : undefined;
        
        newObservations.push({
            id: `obs-${i}`,
            date: dateObj.toISOString().split('T')[0],
            day: i,
            weekday: weekdays[dayOfWeek],
            isDone: isDone,
            mood: mood, // Now stores ID like 'happy', 'sad'
            imageUrl: isDone ? `https://picsum.photos/seed/${selectedChild?.id}-${i}-${currentDate.getMonth()}/300/300` : undefined,
            title: isDone ? '孩子不做作业，催了几次...' : '你忘记觉察了哦',
            content: isDone ? '下次就不要去打人，打算...' : '你没留下任何感悟',
            // Mock Data for Detail View
            summaryQuote: "你记下的每个烦躁瞬间\n都是写给孩子未来的一封信\n“看，爸爸妈妈也在学者长大”",
            moodDescription: "今天的你是温柔耐心的爸妈，还是被气到想“重启系统”？",
            frustrationText: isDone ? "面对孩子磨蹭拖拉，内心火气直冒，但也意识到了自己的急躁。" : "拖拉磨蹭，情绪失控，隔代教育矛盾不断",
            mirrorText: isDone ? "建议尝试‘我’字句表达感受，而不是指责。例如‘我看到你还没穿好鞋子，我担心我们会迟到’。" : "一条建议，照亮明天的方向。我们不教你应该怎样，只陪你一起发现“原来我还可以这样”",
            diaryText: isDone ? "今天虽然发火了，但事后和孩子道歉拥抱，感觉关系反而更近了一步。" : "别一个人扛，写下来，不是抱怨，而是一次自我梳理，也可能是改变的起点",
            expertText: "情绪管理是父母的必修课，今天的觉察很有价值，继续保持对情绪的敏锐度。"
        });
    }
    setObservations(newObservations);
  }, [currentDate, selectedChildId]);

  // --- 3. Handle Navigation Params (e.g., Mood Check-in or Return to Detail) ---
  useEffect(() => {
    if (initialParams?.action === 'open_mood_selector' || initialParams?.action === 'open_detail_view') {
        // Ensure we are on the current month to show today
        const targetDate = initialParams.date ? new Date(initialParams.date) : new Date();
        
        if (currentDate.getMonth() !== targetDate.getMonth() || currentDate.getFullYear() !== targetDate.getFullYear()) {
            setCurrentDate(targetDate);
        }
        
        // Find "Target Day" in the observations list
        const targetDay = targetDate.getDate();
        
        const targetRecord = observations.find(o => o.day === targetDay);
        
        if (targetRecord) {
             setSelectedObservation(targetRecord);
             if (initialParams.action === 'open_mood_selector') {
                 setAutoOpenSelector('mood');
             } else {
                 setAutoOpenSelector(null); // Just view detail
             }
        }

        if (onClearParams) onClearParams();
    }
  }, [initialParams, observations, currentDate, onClearParams]);


  const handleMonthChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if(e.target.value) {
        const [y, m] = e.target.value.split('-');
        setCurrentDate(new Date(parseInt(y), parseInt(m) - 1, 1));
    }
  };

  const handleCardClick = (item: ObservationRecord) => {
      setSelectedObservation(item);
      setAutoOpenSelector(null); // Reset auto-open on manual click
  };

  const handleBannerClick = (banner: BannerItem) => {
      setSelectedBanner(banner);
  };

  const handleUpdateRecord = (id: string, updates: Partial<ObservationRecord>) => {
      setObservations(prev => prev.map(obs => {
          if (obs.id === id) {
              const updated = { ...obs, ...updates };
              setSelectedObservation(updated);
              return updated;
          }
          return obs;
      }));
  };

  if (!selectedChild) return null;

  // --- RENDER DETAIL VIEW IF SELECTED ---
  if (selectedObservation) {
      return (
          <ObservationDetail 
            data={selectedObservation} 
            childName={selectedChild.name}
            childId={selectedChild.id}
            onBack={() => {
                setSelectedObservation(null);
                setAutoOpenSelector(null);
            }} 
            onUpdateRecord={handleUpdateRecord}
            onNavigate={onNavigate}
            initialAction={autoOpenSelector}
          />
      );
  }

  // --- RENDER BANNER ARTICLE VIEW IF SELECTED ---
  if (selectedBanner) {
      return (
          <ArticleDetail 
            banner={selectedBanner}
            onBack={() => setSelectedBanner(null)}
          />
      );
  }

  // --- RENDER MAIN LIST VIEW ---
  return (
    <div className="flex flex-col h-full bg-white relative">
        
        {/* --- HEADER --- */}
        <div className="px-6 pt-4 pb-2 flex justify-between items-center bg-white sticky top-0 z-20">
            {/* Left: Avatar + Greeting */}
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-orange-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {/* Placeholder for user avatar if no URL */}
                    {selectedChild.relation ? selectedChild.relation[0] : '妈'}
                </div>
                <div className="flex flex-col">
                     <h1 className="text-gray-900 font-bold text-base leading-tight">
                        {selectedChild.name}{selectedChild.relation || '妈妈'}，您好
                     </h1>
                </div>
            </div>

            {/* Right: Actions */}
            <div className="flex items-center gap-4">
                 <button onClick={() => setIsMenuOpen(!isMenuOpen)} className="p-1 relative">
                    <Bars3Icon className="w-7 h-7 text-gray-700" />
                 </button>
            </div>
        </div>

        {/* --- CHILD SWITCHER DRAWER (Simplified Dropdown) --- */}
        {isMenuOpen && (
            <>
                <div className="absolute inset-0 z-30 bg-black/10" onClick={() => setIsMenuOpen(false)} />
                <div className="absolute top-16 right-4 z-40 bg-white shadow-xl rounded-xl p-2 min-w-[160px] animate-fade-in-up border border-gray-100">
                    <div className="text-[10px] text-gray-400 px-2 py-1">切换孩子</div>
                    {childrenData.map(child => (
                        <button
                            key={child.id}
                            onClick={() => {
                                onSelectChild(child.id);
                                setIsMenuOpen(false);
                            }}
                            className={`w-full text-left px-3 py-2 rounded-lg text-sm mb-1 flex items-center justify-between ${
                                selectedChildId === child.id ? 'bg-orange-50 text-orange-600 font-bold' : 'text-gray-600 hover:bg-gray-50'
                            }`}
                        >
                            <span>{child.name}</span>
                            {selectedChildId === child.id && <div className="w-1.5 h-1.5 rounded-full bg-orange-500" />}
                        </button>
                    ))}
                    <div className="h-px bg-gray-100 my-1" />
                    <button 
                        onClick={() => onNavigate('me', {view: 'children', action: 'add'})}
                        className="w-full text-left px-3 py-2 text-xs text-orange-500 font-bold flex items-center gap-1"
                    >
                        <PlusIcon className="w-3 h-3" /> 添加新孩子
                    </button>
                </div>
            </>
        )}

        {/* --- SCROLLABLE CONTENT --- */}
        <div className="flex-1 overflow-y-auto no-scrollbar pb-24">
            
            {/* 1. Banner Carousel */}
            <div className="mt-2 mb-6 px-4">
                <div className="w-full h-[140px] rounded-2xl overflow-hidden relative shadow-sm cursor-pointer group">
                    {MOCK_BANNERS.map((banner, idx) => (
                        <div 
                            key={banner.id}
                            onClick={() => handleBannerClick(banner)}
                            className={`absolute inset-0 transition-opacity duration-700 ease-in-out ${
                                idx === activeBannerIndex ? 'opacity-100' : 'opacity-0'
                            }`}
                        >
                            <img src={banner.imageUrl} alt="banner" className="w-full h-full object-cover" />
                            {/* Overlay Title for Better Context */}
                            <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/60 to-transparent">
                                <h3 className="text-white font-bold text-sm line-clamp-1">{banner.title}</h3>
                            </div>
                        </div>
                    ))}
                    {/* Indicators */}
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1.5 z-10">
                        {MOCK_BANNERS.map((_, idx) => (
                            <div 
                                key={idx} 
                                className={`w-1.5 h-1.5 rounded-full transition-all ${
                                    idx === activeBannerIndex ? 'bg-white w-3' : 'bg-white/50'
                                }`}
                            />
                        ))}
                    </div>
                </div>
            </div>

            {/* 2. Date Picker & Section Title */}
            <div className="px-6 flex justify-between items-center mb-4">
                <h2 className="text-orange-500 font-bold text-sm tracking-widest uppercase">
                    每日觉察
                </h2>
                
                <div className="relative group">
                    <div className="flex items-center gap-1 text-gray-800 font-bold text-base cursor-pointer">
                        <span>{formattedMonth}</span>
                        <div className="rotate-90">
                            <ChevronRightIcon className="w-4 h-4 text-gray-400" />
                        </div>
                    </div>
                    {/* Invisible Month Picker Overlay */}
                    <input 
                        type="month" 
                        value={dateInputStr}
                        max={maxMonth}
                        onChange={handleMonthChange}
                        className="absolute inset-0 opacity-0 cursor-pointer w-full h-full z-10"
                    />
                </div>
            </div>

            {/* 3. Waterfall Grid (2 columns) */}
            <div className="px-4 grid grid-cols-2 gap-3">
                {observations.map((item) => {
                    const moodObj = item.mood ? MOOD_DATA.find(m => m.id === item.mood) : null;
                    return (
                        <div 
                            key={item.id} 
                            className="flex flex-col group cursor-pointer"
                            onClick={() => handleCardClick(item)}
                        >
                            {/* Card Image Area */}
                            <div className={`
                                relative aspect-square rounded-2xl overflow-hidden mb-2 shadow-sm transition-transform active:scale-95
                                ${!item.isDone ? 'bg-[#FFF9F2] flex items-center justify-center' : ''}
                            `}>
                                {/* Date Label */}
                                <div className="absolute top-2 left-2 z-10">
                                    <div className={`
                                        flex flex-col items-center justify-center rounded-lg px-2 py-1 min-w-[2.5rem]
                                        backdrop-blur-md shadow-sm border border-white/20
                                        ${item.isDone 
                                            ? 'bg-black/40 text-white' 
                                            : 'bg-white/80 text-orange-600'
                                        }
                                    `}>
                                        <span className="text-xl font-bold leading-none tracking-tighter">
                                            {String(item.day).padStart(2, '0')}
                                        </span>
                                        <span className="text-[9px] font-medium opacity-90 leading-tight mt-0.5">
                                            {item.weekday}
                                        </span>
                                    </div>
                                </div>

                                {item.isDone ? (
                                    <>
                                        {/* Background Image */}
                                        <div className="absolute inset-0 bg-orange-900/10 z-0" />
                                        <img src={item.imageUrl} alt="day" className="w-full h-full object-cover" />
                                        
                                        {/* Mood Icon (Bottom Left) - Now using SVG */}
                                        {item.mood && moodObj && (
                                            <div className={`absolute bottom-2 left-2 w-8 h-8 rounded-full flex items-center justify-center shadow-sm border border-white/20 ${moodObj.color}`}>
                                                <MoodFace id={item.mood} className="w-5 h-5" />
                                            </div>
                                        )}
                                    </>
                                ) : (
                                    <>
                                        {/* Not Done State */}
                                        <div className="flex flex-col items-center justify-center">
                                            <PlusIcon className="w-10 h-10 text-orange-300/50" />
                                        </div>
                                    </>
                                )}
                            </div>

                            {/* Footer Text */}
                            <div className="px-1">
                                <h3 className={`text-xs font-bold mb-0.5 line-clamp-1 ${item.isDone ? 'text-gray-800' : 'text-gray-400'}`}>
                                    {item.title}
                                </h3>
                                <p className="text-[10px] text-gray-400 line-clamp-1 leading-tight">
                                    {item.content}
                                </p>
                            </div>
                        </div>
                    );
                })}
            </div>
            
            <div className="h-16" /> {/* Spacer for taller TabBar */}
        </div>
    </div>
  );
};
