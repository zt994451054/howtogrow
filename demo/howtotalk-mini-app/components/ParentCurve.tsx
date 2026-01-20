
import React, { useEffect, useRef, useState, useMemo } from 'react';
import * as echarts from 'echarts';
import { ChevronRightIcon, BookmarkIcon, CalendarDaysIcon, Bars3Icon, PlusIcon } from './Icons';
import { Child } from '../types';
import { MOCK_PARENT_CURVE_DATA } from '../constants';
import { MOOD_DATA, MoodFace } from './MoodResources';
import { FrustrationIcon } from './FrustrationResources';
import { FrustrationDistributionDetail } from './FrustrationDistributionDetail';

interface ParentCurveProps {
    childrenData: Child[];
    selectedChildId: string | null;
    onSelectChild: (id: string) => void;
    onNavigate: (tab: string, params?: Record<string, any>) => void;
}

export const ParentCurve: React.FC<ParentCurveProps> = ({
    childrenData,
    selectedChildId,
    onSelectChild,
    onNavigate
}) => {
    const chartRef = useRef<HTMLDivElement>(null);
    const chartInstance = useRef<echarts.ECharts | null>(null);
    const selectedChild = childrenData.find(c => c.id === selectedChildId) || childrenData[0];
    
    // --- Date State ---
    // Default to last 3 months (90 days)
    const today = new Date();
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setDate(today.getDate() - 90);

    const [startDate, setStartDate] = useState(threeMonthsAgo.toISOString().split('T')[0]);
    const [endDate, setEndDate] = useState(today.toISOString().split('T')[0]);
    const [isDatePickerOpen, setIsDatePickerOpen] = useState(false);

    // --- View State ---
    const [showFrustrationDetail, setShowFrustrationDetail] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    // --- Chart Filters ---
    const [selectedMetrics, setSelectedMetrics] = useState<string[]>(['relation', 'rules', 'study', 'emotion', 'communication']);

    // Colors matching the GrowthReport (Capability Chart) palette for consistency
    const metrics = [
        { key: 'relation', label: '亲子关系', color: '#F97316' }, // Orange
        { key: 'rules', label: '规则建立', color: '#EC4899' }, // Pink
        { key: 'study', label: '学习支持', color: '#10B981' }, // Green
        { key: 'emotion', label: '情绪管理', color: '#3B82F6' }, // Blue
        { key: 'communication', label: '沟通表达', color: '#8B5CF6' } // Purple
    ];

    // Toggle logic for chart lines
    const toggleMetric = (key: string) => {
        setSelectedMetrics(prev => 
            prev.includes(key) 
            ? prev.filter(k => k !== key) 
            : [...prev, key]
        );
    };

    // Calculate Persistence Day based on filtered range (Mock logic)
    const getDaysDiff = (s: string, e: string) => {
        const d1 = new Date(s);
        const d2 = new Date(e);
        const diffTime = Math.abs(d2.getTime() - d1.getTime());
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24)); 
    }
    const persistenceDays = Math.floor(getDaysDiff(startDate, endDate) * 0.7);

    // --- Mock Mood Distribution Data (Dynamic based on date range seed) ---
    const moodDistribution = useMemo(() => {
        // Use startDate char codes to seed random generator for stability
        const seed = startDate.charCodeAt(startDate.length - 1);
        
        // Generate random counts for all 10 moods
        const stats = MOOD_DATA.map((mood, idx) => {
            // Pseudo-random value between 5 and 50
            const count = Math.floor(((Math.sin(idx + seed) + 1) * 25) + 5); 
            return { ...mood, count };
        });

        const total = stats.reduce((acc, cur) => acc + cur.count, 0);
        
        // Sort by count descending to show "Intuitive Insight"
        return stats
            .sort((a, b) => b.count - a.count)
            .map(s => ({
                ...s,
                percentage: Math.round((s.count / total) * 100)
            }));
    }, [startDate, endDate]);
    
    // Max percentage for bar scaling
    const maxPercentage = Math.max(...moodDistribution.map(m => m.percentage));


    // --- ECharts Initialization & Update ---
    useEffect(() => {
        if (!chartRef.current) return;

        // Initialize only once
        if (!chartInstance.current) {
            chartInstance.current = echarts.init(chartRef.current);
            window.addEventListener('resize', () => chartInstance.current?.resize());
        }

        // Filter Data based on Date Range
        const filteredData = MOCK_PARENT_CURVE_DATA.filter(item => 
            item.date >= startDate && item.date <= endDate
        );

        const dates = filteredData.map(item => item.date.slice(5)); // Show MM-DD

        // Map series based on selection
        const seriesList = metrics.map(m => {
            const isVisible = selectedMetrics.includes(m.key);
            return {
                name: m.label,
                type: 'line',
                smooth: true,
                showSymbol: false,
                symbolSize: 6,
                lineStyle: { 
                    width: 2.5,
                    color: m.color,
                    opacity: isVisible ? 1 : 0 // Fade out if not selected
                },
                data: isVisible ? filteredData.map(item => (item as any)[m.key]) : [], 
                silent: !isVisible
            };
        });

        const option: echarts.EChartsOption = {
            tooltip: {
                trigger: 'axis',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                textStyle: { color: '#333', fontSize: 12 },
                padding: 8,
                confine: true,
                formatter: (params: any) => {
                    if (!params || params.length === 0) return '';
                    let res = `<div style="font-weight:bold;margin-bottom:4px;">${params[0].name}</div>`;
                    params.forEach((p: any) => {
                        if (p.value !== undefined) {
                            res += `<div style="display:flex;align-items:center;gap:4px;font-size:11px;">
                                <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background-color:${p.color};"></span>
                                <span>${p.seriesName}: ${p.value}</span>
                            </div>`;
                        }
                    });
                    return res;
                }
            },
            grid: {
                left: '2%',
                right: '5%',
                bottom: '3%', 
                top: '10%',
                containLabel: true // Important for labels to show inside container
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: dates,
                axisLine: { 
                    show: true,
                    lineStyle: { color: '#6B7280', width: 1.5 } 
                },
                axisTick: { 
                    show: true, 
                    alignWithLabel: true,
                    lineStyle: { color: '#6B7280' } 
                },
                axisLabel: {
                    show: true,
                    interval: 'auto',
                    color: '#6B7280',
                    fontSize: 10,
                    margin: 8,
                    hideOverlap: true
                }
            },
            yAxis: {
                type: 'value',
                min: 0,
                max: 100,
                splitLine: { 
                    show: true,
                    lineStyle: { color: '#F3F4F6', type: 'dashed' } 
                },
                axisLine: { 
                    show: true, 
                    lineStyle: { color: '#6B7280', width: 1.5 } 
                },
                axisLabel: { 
                    show: true, 
                    color: '#6B7280',
                    fontSize: 10
                }
            },
            series: seriesList as any
        };

        chartInstance.current.setOption(option, { notMerge: false });

    }, [selectedMetrics, startDate, endDate, showFrustrationDetail]); 

    // --- RENDER DETAIL PAGE ---
    if (showFrustrationDetail) {
        return (
            <FrustrationDistributionDetail 
                onBack={() => setShowFrustrationDetail(false)}
                startDate={startDate}
                endDate={endDate}
            />
        );
    }

    // --- RENDER MAIN PAGE ---
    return (
        <div className="flex flex-col h-full bg-white relative">
            {/* HEADER */}
            <div className="px-6 pt-4 pb-2 flex justify-between items-center bg-white sticky top-0 z-20">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-orange-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
                        {selectedChild.relation ? selectedChild.relation[0] : '妈'}
                    </div>
                    <div className="flex flex-col">
                        <h1 className="text-gray-900 font-bold text-base leading-tight">
                            {selectedChild.name}{selectedChild.relation || '妈妈'}，您好
                        </h1>
                    </div>
                </div>
                
                {/* Right: Child Switcher Menu */}
                <div className="flex items-center gap-4">
                     <button onClick={() => setIsMenuOpen(!isMenuOpen)} className="p-1 relative">
                        <Bars3Icon className="w-7 h-7 text-gray-700" />
                     </button>
                </div>
            </div>

            {/* --- CHILD SWITCHER DRAWER (Dropdown) --- */}
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

            <div className="flex-1 overflow-y-auto no-scrollbar pb-24">
                {/* SECTION 1: CHART */}
                <div className="px-6 mt-6 relative z-0">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="font-bold text-gray-800 text-sm">我的成长曲线</h2>

                        {/* Date Range Picker - Moved Here */}
                        <div className="relative">
                            <button 
                                onClick={() => setIsDatePickerOpen(!isDatePickerOpen)}
                                className="flex items-center gap-2 bg-gray-50 border border-gray-100 rounded-full px-3 py-1.5 active:bg-gray-100 transition-colors"
                            >
                                <CalendarDaysIcon className="w-4 h-4 text-orange-500" />
                                <span className="text-[11px] font-bold text-gray-600">
                                     {startDate.replace(/-/g, '.').slice(5)} - {endDate.replace(/-/g, '.').slice(5)}
                                </span>
                                <div className={`transition-transform duration-200 ${isDatePickerOpen ? 'rotate-180' : 'rotate-90'}`}>
                                    <ChevronRightIcon className="w-3 h-3 text-gray-400" />
                                </div>
                            </button>

                            {/* Dropdown Date Picker */}
                            {isDatePickerOpen && (
                                <>
                                    <div className="fixed inset-0 z-40" onClick={() => setIsDatePickerOpen(false)} />
                                    <div className="absolute top-full right-0 mt-2 bg-white rounded-xl shadow-xl border border-gray-100 p-4 z-50 w-[240px] animate-fade-in-up">
                                        <h3 className="text-xs font-bold text-gray-400 mb-3 uppercase tracking-wider">选择日期范围 (最多半年)</h3>
                                        
                                        <div className="space-y-3 mb-4">
                                            <div>
                                                <label className="block text-[10px] text-gray-400 mb-1">开始日期</label>
                                                <input 
                                                    type="date" 
                                                    value={startDate}
                                                    max={endDate}
                                                    onChange={(e) => setStartDate(e.target.value)}
                                                    className="w-full bg-gray-50 rounded-lg px-2 py-1.5 text-xs text-gray-800 outline-none focus:ring-1 focus:ring-orange-200"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-[10px] text-gray-400 mb-1">结束日期</label>
                                                <input 
                                                    type="date" 
                                                    value={endDate}
                                                    min={startDate}
                                                    max={new Date().toISOString().split('T')[0]}
                                                    onChange={(e) => setEndDate(e.target.value)}
                                                    className="w-full bg-gray-50 rounded-lg px-2 py-1.5 text-xs text-gray-800 outline-none focus:ring-1 focus:ring-orange-200"
                                                />
                                            </div>
                                        </div>
                                        
                                        <button 
                                            onClick={() => setIsDatePickerOpen(false)}
                                            className="w-full bg-orange-500 text-white text-xs font-bold py-2 rounded-lg"
                                        >
                                            确认
                                        </button>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Chart Container */}
                    <div className="w-full h-[260px] mb-4 relative overflow-hidden bg-white border border-gray-50 rounded-lg shadow-sm">
                        <div ref={chartRef} className="w-full h-full" />
                    </div>

                    {/* Toggles - Adjusted to fit nicely without scroll */}
                    <div className="flex flex-wrap gap-2 mb-6 relative z-10 bg-white justify-between">
                        {metrics.map(m => {
                            const isActive = selectedMetrics.includes(m.key);
                            return (
                                <button
                                    key={m.key}
                                    onClick={() => toggleMetric(m.key)}
                                    className={`
                                        flex-1 min-w-[60px] max-w-[80px] px-1 py-1.5 rounded-lg text-[10px] font-bold transition-all border flex items-center justify-center gap-1
                                        ${isActive 
                                        ? 'bg-white border-transparent shadow-sm' 
                                        : 'bg-transparent border-gray-100 text-gray-400 grayscale opacity-70'}
                                    `}
                                    style={isActive ? { borderColor: m.color, color: m.color, backgroundColor: `${m.color}08` } : {}}
                                >
                                    <span 
                                        className="w-1.5 h-1.5 rounded-full shrink-0" 
                                        style={{ backgroundColor: isActive ? m.color : '#9CA3AF' }}
                                    />
                                    <span className="truncate">{m.label}</span>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* SECTION 2: PERSISTENCE */}
                <div className="flex flex-col items-center justify-center text-center px-6 mb-8 relative z-10 bg-white">
                     <div className="w-14 h-14 bg-orange-100 rounded-full flex items-center justify-center mb-3 text-orange-500 shadow-sm">
                        <BookmarkIcon className="w-7 h-7" />
                     </div>
                     <h3 className="text-sm font-bold text-gray-800 mb-1">七仔妈妈</h3>
                     <p className="text-sm text-gray-600 mb-1">
                         在此期间坚持进步了 <span className="text-orange-500 font-bold">{persistenceDays}</span> 天
                     </p>
                     <p className="text-xs text-gray-400 max-w-[80%]">
                         你的每次觉察，都是孩子成长路上最珍贵的礼物
                     </p>
                </div>

                {/* SECTION 3: MOOD DISTRIBUTION (Updated Histogram Layout) */}
                <div className="bg-[#FFF9F5] p-6 rounded-2xl mx-4 mb-8 border border-orange-50 relative z-10">
                    <div className="mb-6 text-center">
                        <h3 className="font-bold text-gray-800 text-sm">我的育儿状态分布</h3>
                        <p className="text-[10px] text-gray-400 mt-1">总共10个星期数据洞察</p>
                    </div>
                    
                    {/* Histogram Grid: 5 Cols x 2 Rows */}
                    <div className="grid grid-cols-5 gap-y-6 gap-x-2">
                        {moodDistribution.map((m, idx) => {
                            // Calculate Bar Height (Max 50px)
                            const barHeight = Math.max(4, (m.percentage / maxPercentage) * 40);
                            
                            return (
                                <div key={m.id} className="flex flex-col items-center justify-end group">
                                    {/* Percentage */}
                                    <span className="text-[10px] font-bold text-gray-400 mb-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                        {m.percentage}%
                                    </span>
                                    
                                    {/* Bar & Icon Container */}
                                    <div className="flex flex-col items-center justify-end gap-1">
                                        {/* Vertical Bar */}
                                        <div 
                                            className="w-1.5 rounded-t-full transition-all duration-500 ease-out"
                                            style={{ 
                                                height: `${barHeight}px`, 
                                                backgroundColor: m.color.replace('bg-', '').replace('[', '').replace(']', '') 
                                            }}
                                        >
                                            <div className={`w-full h-full rounded-t-full ${m.color.replace('text-', 'bg-').replace('bg-white', 'bg-gray-200')}`}></div>
                                        </div>

                                        {/* Face Icon */}
                                        <div className={`w-8 h-8 rounded-full flex items-center justify-center shadow-sm ${m.color} ${m.textColor}`}>
                                            <MoodFace id={m.id} className="w-5 h-5" />
                                        </div>
                                    </div>
                                    
                                    {/* Mood Name */}
                                    <span className="text-[10px] text-gray-500 font-medium mt-1.5 whitespace-nowrap scale-90">
                                        {m.label}
                                    </span>
                                    
                                    {/* Percentage Label */}
                                    <span className="text-[9px] font-bold text-gray-400 leading-none mt-0.5">
                                        {m.percentage}%
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* SECTION 4: FRUSTRATION DISTRIBUTION */}
                <div className="px-6 mb-8 relative z-10 bg-white">
                    <div className="flex justify-between items-center mb-4">
                        <h3 className="font-bold text-gray-800 text-sm">我的困扰分布</h3>
                        <div 
                            onClick={() => setShowFrustrationDetail(true)}
                            className="flex items-center gap-1 text-gray-400 text-xs cursor-pointer hover:text-orange-500 transition-colors"
                        >
                            <span>更多</span>
                            <ChevronRightIcon className="w-3 h-3" />
                        </div>
                    </div>

                    <div className="grid grid-cols-3 gap-3">
                        {[
                            {rank: 1, id: 'eating', label: '吃饭问题'},
                            {rank: 2, id: 'sleeping', label: '拒绝睡觉'},
                            {rank: 3, id: 'emotion', label: '情绪崩溃'}
                        ].map((item, idx) => (
                            <div key={idx} className="bg-white border border-gray-100 rounded-xl p-3 flex flex-col items-center relative shadow-sm h-24 justify-center">
                                <span className="absolute top-2 left-3 text-gray-300 text-xs font-bold">{item.rank}</span>
                                <div className="w-10 h-10 bg-[#FDF1E3] rounded-full flex items-center justify-center mb-2 text-orange-800/80">
                                     <div className="w-6 h-6">
                                        <FrustrationIcon id={item.id} />
                                     </div>
                                </div>
                                <span className="text-xs text-gray-500 font-medium">{item.label}</span>
                            </div>
                        ))}
                    </div>

                    <div className="mt-6 text-center text-sm text-gray-600">
                        困扰最多的是 <span className="text-orange-500 font-bold">动不动哭闹发脾气</span>
                    </div>
                </div>
            </div>
        </div>
    );
};
