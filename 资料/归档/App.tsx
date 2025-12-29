
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Brush
} from 'recharts';
import { 
  ChevronLeft, 
  ChevronRight,
  Plus,
  Minus,
  Bookmark,
  Search,
  Users,
  BookOpen,
  Sparkles,
  Info,
  X,
  TriangleAlert,
  Flag,
  Play
} from 'lucide-react';
import { generateMockData, processBatches } from './mockData';
import { DIMENSION_NAMES, DIMENSION_COLORS, BatchedResult, RawTestResult, FeedbackData } from './types';
import { analyzeTrends } from './geminiService';

// --- Sub-components ---

const ProgressBar = ({ progress }: { progress: number }) => (
  <div className="w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
    <div 
      className="h-full bg-[#F97316] rounded-full transition-all duration-500 ease-out" 
      style={{ width: `${progress}%` }} 
    />
  </div>
);

// 1. Test Page (Matching Image 1)
const TestPage: React.FC<{ onComplete: (result: RawTestResult) => void; onBack: () => void }> = ({ onComplete, onBack }) => {
  const [step, setStep] = useState(1);
  const totalSteps = 4;
  const [selectedOption, setSelectedOption] = useState<number | null>(null);

  const options = [
    "“Why are you telling me this, and not your boss?”",
    "“Forget about them! Just focus on the fun stuff at work instead”",
    "“It's not that bad — every job has those days, right?”",
    "“Oh well, that's bosses for you. That's just the way they are”"
  ];

  const handleNext = () => {
    if (selectedOption === null) return;
    if (step < totalSteps) {
      setStep(s => s + 1);
      setSelectedOption(null);
    } else {
      // Simulate completing a test
      const newTest: RawTestResult = {
        id: `test-${Date.now()}`,
        date: new Date().toISOString().split('T')[0],
        scores: {
          dimension1: 75,
          dimension2: 60,
          dimension3: 85,
          dimension4: 70,
          dimension5: 90
        },
        feedback: {
          scenario: "当孩子因为弄丢玩具而大哭时...",
          yourOldWay: options[selectedOption],
          insightTitle: "示弱赋能 激发胜任感",
          insightBody: "你一把抢过鞋子替他穿好，孩子心里可能会觉得：“妈妈觉得我不行，只有她做才对。”久而久之，他就不再下次你可以试试蹲下来，轻声说：“妈妈相信你能穿好，需要时喊我‘救援队’哦！”然后忍住不插手。",
          yourNewWay: "“看到心爱的玩具不见了，你一定很难过。我们一起回想一下它可能在哪里？”"
        }
      };
      onComplete(newTest);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-6 pt-4 flex justify-between items-center">
        <button onClick={onBack} className="p-2 -ml-2"><ChevronLeft className="w-6 h-6" /></button>
        <div className="flex-1 px-8">
          <ProgressBar progress={(step / totalSteps) * 100} />
        </div>
        <button className="p-2 -mr-2"><X className="w-6 h-6" /></button>
      </div>

      <div className="flex-1 px-8 pt-12">
        <h2 className="text-[26px] font-bold text-[#D97706] text-center leading-tight mb-12">
          你的孩子知道不应该<br />这样做，但是他们依然这样做...
        </h2>

        <div className="space-y-4">
          {options.map((opt, idx) => (
            <button
              key={idx}
              onClick={() => setSelectedOption(idx)}
              className={`w-full p-6 rounded-[20px] text-left transition-all duration-200 border-2 ${
                selectedOption === idx 
                ? 'bg-[#F97316] text-white border-[#F97316] shadow-lg scale-[1.02]' 
                : 'bg-[#FEE2D2] text-slate-800 border-transparent hover:bg-[#FCD5C0]'
              }`}
            >
              <p className="font-bold text-base leading-relaxed">{opt}</p>
            </button>
          ))}
        </div>
      </div>

      <div className="p-8">
        <button
          disabled={selectedOption === null}
          onClick={handleNext}
          className={`w-full py-5 rounded-[30px] font-bold text-xl transition-all shadow-md active:scale-95 ${
            selectedOption !== null ? 'bg-[#F97316] text-white' : 'bg-slate-200 text-slate-400 cursor-not-allowed'
          }`}
        >
          Next
        </button>
      </div>
    </div>
  );
};

// 2. Feedback Page (Matching Image 2)
const FeedbackPage: React.FC<{ test: RawTestResult; onNext: () => void }> = ({ test, onNext }) => {
  const { feedback } = test;
  if (!feedback) return null;

  return (
    <div className="flex flex-col h-full bg-[#FFF5F0]">
      <div className="px-6 pt-4">
        <ProgressBar progress={100} />
      </div>

      <div className="flex-1 overflow-y-auto no-scrollbar px-8 py-8">
        <div className="flex justify-center mb-8">
          <div className="w-36 h-36 bg-[#FEE8DC] rounded-full flex items-center justify-center relative">
            <div className="flex flex-col items-center">
               <div className="w-1.5 h-16 bg-[#F97316] rounded-full mb-[-12px]" />
               <div className="bg-[#F97316] px-4 py-2 rounded-sm text-white font-bold text-sm flex items-center gap-1 shadow-md text-center">
                 今日<br/>建议
               </div>
            </div>
          </div>
        </div>

        <div className="space-y-10 pb-12">
          <div className="flex items-start gap-4">
            <div className="w-8 h-8 bg-[#F97316] rounded-full flex-shrink-0 flex items-center justify-center text-white font-bold text-sm mt-1">1</div>
            <h3 className="text-2xl font-bold text-slate-800 leading-tight">{feedback.scenario}</h3>
          </div>

          <div className="pl-12 relative">
            <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-[#F97316] rounded-full" />
            <p className="text-[#F97316] font-bold text-sm mb-2">你的做法</p>
            <p className="text-slate-500 italic text-xl leading-relaxed">“{feedback.yourOldWay}”</p>
          </div>

          <div className="space-y-4">
            <div className="flex items-center gap-2 text-[#E11D48]">
              <TriangleAlert className="w-6 h-6" />
              <h4 className="text-xl font-bold">{feedback.insightTitle}</h4>
            </div>
            <p className="text-slate-700 text-lg leading-relaxed font-medium">
              {feedback.insightBody}
            </p>
          </div>

          <div className="pl-12 relative">
            <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-[#F97316] rounded-full" />
            <p className="text-[#F97316] font-bold text-sm mb-2">建议做法</p>
            <p className="text-slate-500 italic text-xl leading-relaxed">“{feedback.yourNewWay}”</p>
          </div>
        </div>
      </div>

      <div className="p-8 bg-gradient-to-t from-[#FFF5F0] via-[#FFF5F0] to-transparent">
        <button 
          onClick={onNext}
          className="w-full bg-[#FEE2D2] hover:bg-[#FCD5C0] text-[#F97316] py-5 rounded-[30px] font-bold text-xl transition-all shadow-sm active:scale-95"
        >
          Next
        </button>
      </div>
    </div>
  );
};

// 3. Overview Page (Modified existing UI)
const OverviewPage: React.FC<{
  batches: BatchedResult[];
  rawTests: RawTestResult[];
  onStartTest: () => void;
  onViewFeedback: (test: RawTestResult) => void;
}> = ({ batches, rawTests, onStartTest, onViewFeedback }) => {
  const [aiInsight, setAiInsight] = useState<string>("");
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  
  const [brushRange, setBrushRange] = useState({ 
    start: Math.max(0, batches.length - 8), 
    end: Math.max(0, batches.length - 1) 
  });

  const [visibleDimensions, setVisibleDimensions] = useState<Record<string, boolean>>({
    dimension1: true,
    dimension2: true,
    dimension3: true,
    dimension4: true,
    dimension5: true,
  });

  const chartData = useMemo(() => {
    return batches.map(b => ({
      name: b.completionDate.split('-').slice(1).join('/'),
      fullDate: b.completionDate,
      batchId: b.batchId,
      ...b.avgScores,
    }));
  }, [batches]);

  const handleAnalyze = async () => {
    setIsAnalyzing(true);
    const insight = await analyzeTrends(batches);
    setAiInsight(insight);
    setIsAnalyzing(false);
  };

  const handleMove = (direction: 'left' | 'right') => {
    const step = 1;
    let { start, end } = brushRange;
    if (direction === 'left') {
      start = Math.max(0, start - step);
      end = start + (brushRange.end - brushRange.start);
    } else {
      end = Math.min(chartData.length - 1, end + step);
      start = end - (brushRange.end - brushRange.start);
    }
    setBrushRange({ start, end });
  };

  return (
    <div className="max-w-xl mx-auto min-h-screen bg-white flex flex-col font-sans select-none overflow-x-hidden">
      <nav className="px-6 pt-4 flex justify-between items-center bg-white sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-orange-500 rounded-lg flex items-center justify-center text-[10px] text-white font-bold leading-none text-center">how<br/>to<br/>talk</div>
            <div>
              <h1 className="text-sm font-bold text-slate-900 leading-none">能力分析报告</h1>
              <p className="text-[10px] text-slate-400 mt-0.5 uppercase tracking-tighter">Aggregation: 5 Tests/Point</p>
            </div>
          </div>
        </div>
        <button onClick={onStartTest} className="px-4 py-2 bg-orange-100 text-orange-600 rounded-full text-xs font-bold flex items-center gap-1.5 active:scale-95 transition-all">
          <Play className="w-3 h-3 fill-current" /> 开始测评
        </button>
      </nav>

      <main className="flex-1 px-6 pt-8 pb-32">
        <header className="mb-6">
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">成长轨迹总览</h2>
          <div className="flex items-center gap-1.5 mt-2 text-slate-400">
            <Info className="w-3.5 h-3.5" />
            <p className="text-[11px] font-medium italic">每点聚合了连续5次测试的平均得分</p>
          </div>
        </header>

        <div className="flex items-center justify-between mb-4 px-2">
          <div className="flex items-center gap-2">
            <button onClick={() => handleMove('left')} className="p-2 bg-slate-50 rounded-xl text-slate-600 active:scale-90"><ChevronLeft className="w-4 h-4" /></button>
            <button onClick={() => handleMove('right')} className="p-2 bg-slate-50 rounded-xl text-slate-600 active:scale-90"><ChevronRight className="w-4 h-4" /></button>
          </div>
          <div className="text-[10px] font-bold text-slate-400 uppercase">滑动缩放区域查看详情</div>
        </div>

        <div className="w-full mb-4 relative h-72">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart 
              data={chartData} 
              margin={{ top: 10, right: 10, left: -25, bottom: 0 }}
              onClick={(e) => {
                if (e && e.activePayload) {
                   const batchId = e.activePayload[0].payload.batchId;
                   const batch = batches.find(b => b.batchId === batchId);
                   if (batch) onViewFeedback(batch.tests[batch.tests.length - 1]);
                }
              }}
            >
              <CartesianGrid strokeDasharray="0" vertical={false} stroke="#f1f5f9" />
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 9, fill: '#94a3b8', fontWeight: 600 }} dy={10} />
              <YAxis domain={[0, 100]} ticks={[0, 20, 40, 60, 80, 100]} axisLine={false} tickLine={false} tick={{ fontSize: 9, fill: '#cbd5e1' }} />
              <Tooltip 
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    return (
                      <div className="bg-slate-900/95 backdrop-blur-sm px-4 py-3 rounded-2xl shadow-2xl border border-slate-800 animate-in fade-in zoom-in-95 duration-200">
                        <p className="text-[10px] font-bold text-slate-400 mb-2 uppercase tracking-widest">{payload[0].payload.fullDate}</p>
                        <div className="space-y-1">
                          {payload.map((item: any, idx) => (
                            <div key={idx} className="flex items-center justify-between gap-4">
                              <div className="flex items-center gap-1.5">
                                <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: item.stroke }} />
                                <span className="text-[11px] text-white/70 font-medium">{item.name}</span>
                              </div>
                              <span className="text-[11px] text-white font-bold">{item.value.toFixed(0)}</span>
                            </div>
                          ))}
                        </div>
                        <p className="text-[8px] text-orange-400 mt-2 text-center font-bold">点击查看该阶段代表反馈</p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              {Object.keys(DIMENSION_NAMES).map((dim) => (
                visibleDimensions[dim] && (
                  <Line
                    key={dim}
                    type="monotone"
                    dataKey={dim}
                    name={DIMENSION_NAMES[dim as keyof typeof DIMENSION_NAMES].split(' ')[0]}
                    stroke={DIMENSION_COLORS[dim as keyof typeof DIMENSION_COLORS]}
                    strokeWidth={2.5}
                    dot={{ r: 3.5, strokeWidth: 1.5, fill: '#fff' }}
                    activeDot={{ r: 6, strokeWidth: 0, fill: DIMENSION_COLORS[dim as keyof typeof DIMENSION_COLORS] }}
                    animationDuration={1500}
                  />
                )
              ))}
              <Brush dataKey="name" height={30} stroke="#fb923c" fill="#fff7ed" startIndex={brushRange.start} endIndex={brushRange.end} travelerWidth={15} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="flex flex-wrap gap-x-6 gap-y-3 mb-10 px-2 justify-center">
          {Object.entries(DIMENSION_NAMES).map(([key, name]) => (
            <button key={key} onClick={() => setVisibleDimensions(p => ({...p, [key]: !p[key]}))} className={`flex items-center gap-2 transition-all ${visibleDimensions[key] ? 'opacity-100' : 'opacity-30'}`}>
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: DIMENSION_COLORS[key as keyof typeof DIMENSION_COLORS] }} />
              <span className="text-xs font-bold text-slate-700">{name.split(' ')[0]}</span>
            </button>
          ))}
        </div>

        <div className="mb-10">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-slate-800">近期自测反馈</h3>
            <button onClick={() => setShowHistory(!showHistory)} className="text-[11px] text-orange-500 font-bold uppercase">{showHistory ? '收起' : '查看全部'}</button>
          </div>
          <div className={`grid grid-cols-2 gap-3 transition-all ${showHistory ? '' : 'max-h-24 overflow-hidden'}`}>
            {rawTests.slice(-4).reverse().map((test) => (
              <button 
                key={test.id}
                onClick={() => onViewFeedback(test)}
                className="p-4 bg-slate-50 hover:bg-orange-50 rounded-2xl border border-slate-100 transition-all text-left"
              >
                <span className="text-[10px] text-slate-400 font-bold block mb-1">{test.date}</span>
                <span className="text-xs font-bold text-slate-700 line-clamp-1">{test.feedback?.insightTitle}</span>
              </button>
            ))}
          </div>
        </div>

        <section className="bg-[#E4EFE9] rounded-[40px] p-8 relative overflow-hidden shadow-sm">
          <h3 className="text-2xl font-bold text-slate-800 mb-3 tracking-tight">{aiInsight ? 'AI 导师建议' : '练习遇到瓶颈？'}</h3>
          <p className="text-slate-600 text-sm leading-relaxed mb-8 max-w-[85%]">
            {aiInsight || '让 AI 深度分析你过去 50 次测试的曲线趋势，揭示潜在的进步空间与行为偏好。'}
          </p>
          <button
            onClick={handleAnalyze}
            disabled={isAnalyzing}
            className="bg-[#333333] text-white px-8 py-4 rounded-full text-sm font-bold transition-all active:scale-95 flex items-center gap-2"
          >
            {isAnalyzing ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : (aiInsight ? '重新分析' : '开始智能分析')}
          </button>
          <Sparkles className="absolute -top-4 -right-4 w-24 h-24 text-[#2D5A41] opacity-10 rotate-12" />
        </section>
      </main>

      <footer className="fixed bottom-0 left-0 right-0 bg-white/80 backdrop-blur-md border-t border-slate-100 px-8 pt-3 pb-8 z-20">
        <div className="max-w-xl mx-auto flex justify-between items-center text-slate-300">
          <div className="flex flex-col items-center gap-1 cursor-pointer"><BookOpen className="w-6 h-6" /><span className="text-[9px] font-bold uppercase tracking-widest">Learn</span></div>
          <div className="flex flex-col items-center gap-1 cursor-pointer"><Users className="w-6 h-6" /><span className="text-[9px] font-bold uppercase tracking-widest">Meet</span></div>
          <div className="flex flex-col items-center gap-1 text-slate-900"><Search className="w-6 h-6" /><span className="text-[9px] font-bold uppercase tracking-widest">Discover</span></div>
          <div className="flex flex-col items-center gap-1 cursor-pointer"><Bookmark className="w-6 h-6" /><span className="text-[9px] font-bold uppercase tracking-widest">Save</span></div>
        </div>
      </footer>
    </div>
  );
};

const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<'overview' | 'test' | 'feedback'>('overview');
  const [rawTests, setRawTests] = useState<RawTestResult[]>([]);
  const [batches, setBatches] = useState<BatchedResult[]>([]);
  const [activeTest, setActiveTest] = useState<RawTestResult | null>(null);

  useEffect(() => {
    const data = generateMockData(60); 
    setRawTests(data);
    setBatches(processBatches(data));
  }, []);

  const handleCompleteTest = (newTest: RawTestResult) => {
    const updatedRaw = [...rawTests, newTest];
    setRawTests(updatedRaw);
    setBatches(processBatches(updatedRaw));
    setActiveTest(newTest);
    setCurrentPage('feedback');
  };

  return (
    <div className="h-screen w-full bg-white max-w-xl mx-auto overflow-hidden shadow-xl">
      {currentPage === 'overview' && (
        <OverviewPage 
          batches={batches} 
          rawTests={rawTests} 
          onStartTest={() => setCurrentPage('test')}
          onViewFeedback={(test) => {
            setActiveTest(test);
            setCurrentPage('feedback');
          }}
        />
      )}
      {currentPage === 'test' && (
        <TestPage 
          onBack={() => setCurrentPage('overview')} 
          onComplete={handleCompleteTest} 
        />
      )}
      {currentPage === 'feedback' && activeTest && (
        <FeedbackPage 
          test={activeTest} 
          onNext={() => setCurrentPage('overview')} 
        />
      )}
    </div>
  );
};

export default App;
