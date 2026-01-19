
import React, { useEffect, useRef, useState } from 'react';
import * as echarts from 'echarts';
import { MOCK_GROWTH_DATA } from '../constants';
import { ChevronLeftIcon, SmileLogo } from './Icons';
import { Child } from '../types';

interface GrowthReportProps {
  childrenData: Child[];
  onBack: () => void;
}

export const GrowthReport: React.FC<GrowthReportProps> = ({ childrenData, onBack }) => {
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null);

  // If there are no children, we can't show a report
  if (childrenData.length === 0) {
     return (
        <div className="flex flex-col h-full bg-white animate-fade-in">
           <Header onBack={onBack} title="成长报告" />
           <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-gray-400">
              <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                  <SmileLogo className="w-8 h-8 opacity-50 grayscale" />
              </div>
              <p className="text-sm">暂无孩子数据</p>
              <p className="text-xs mt-2">请先在“孩子管理”中添加孩子</p>
           </div>
        </div>
     )
  }

  // Step 1: Child Selection View
  if (!selectedChildId) {
    return (
      <div className="flex flex-col h-full bg-gray-50 animate-fade-in">
        <Header onBack={onBack} title="成长报告" />
        
        <div className="p-6">
           <h2 className="text-xl font-bold text-gray-800 mb-2">选择孩子</h2>
           <p className="text-xs text-gray-400 mb-6">查看特定孩子的成长能力模型</p>

           <div className="grid grid-cols-2 gap-4">
              {childrenData.map(child => (
                 <div 
                   key={child.id}
                   onClick={() => setSelectedChildId(child.id)}
                   className="bg-white aspect-[4/5] rounded-2xl p-4 flex flex-col items-center justify-center gap-3 shadow-sm border border-transparent hover:border-orange-200 hover:shadow-md transition-all cursor-pointer group active:scale-95"
                 >
                    <div className={`w-16 h-16 rounded-full flex items-center justify-center text-3xl mb-1 ${
                        child.gender === 'boy' ? 'bg-blue-50' : 'bg-pink-50'
                    }`}>
                       {child.gender === 'boy' ? '👦' : '👧'}
                    </div>
                    <div className="text-center">
                       <div className="font-bold text-gray-800 text-sm group-hover:text-orange-500 transition-colors">{child.name}</div>
                       <div className="text-[10px] text-gray-400 mt-1 bg-gray-50 px-2 py-0.5 rounded-full">{child.age} 岁</div>
                    </div>
                 </div>
              ))}
           </div>
        </div>
      </div>
    );
  }

  // Step 2: Report View with ECharts
  const selectedChild = childrenData.find(c => c.id === selectedChildId);
  return (
    <ReportView 
        child={selectedChild!} 
        onBack={() => setSelectedChildId(null)} 
    />
  );
};

// --- Sub-components ---

const Header: React.FC<{onBack: () => void, title: string}> = ({ onBack, title }) => (
  <div className="px-4 py-3 bg-white border-b border-gray-100 flex items-center gap-2 shrink-0">
    <button onClick={onBack} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
      <ChevronLeftIcon className="w-6 h-6" />
    </button>
    <span className="font-bold text-gray-800">{title}</span>
  </div>
);

const ReportView: React.FC<{child: Child, onBack: () => void}> = ({ child, onBack }) => {
    const chartRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!chartRef.current) return;

        const chart = echarts.init(chartRef.current);
        const data = MOCK_GROWTH_DATA;
        const dates = data.map(item => item.date);

        const option: echarts.EChartsOption = {
            color: ['#F97316', '#EC4899', '#10B981', '#3B82F6', '#8B5CF6'],
            tooltip: {
                trigger: 'axis',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                borderColor: '#eee',
                textStyle: {
                    color: '#333',
                    fontSize: 12
                },
                padding: 10
            },
            legend: {
                data: ['逻辑思维', '知识储备', '反应速度', '准确度', '创造力'],
                bottom: 0,
                icon: 'circle',
                itemWidth: 8,
                itemHeight: 8,
                textStyle: {
                    fontSize: 10,
                    color: '#666'
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '15%',
                top: '5%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: dates,
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: {
                    color: '#9CA3AF',
                    fontSize: 10,
                    margin: 12
                }
            },
            yAxis: {
                type: 'value',
                min: 40,
                max: 100,
                splitLine: {
                    lineStyle: {
                        color: '#F3F4F6',
                        type: 'dashed'
                    }
                },
                axisLabel: {
                    color: '#9CA3AF',
                    fontSize: 10
                }
            },
            series: [
                {
                    name: '逻辑思维',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    symbolSize: 6,
                    lineStyle: { width: 3 },
                    data: data.map(item => item.logic)
                },
                {
                    name: '知识储备',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    symbolSize: 6,
                    lineStyle: { width: 3 },
                    data: data.map(item => item.knowledge)
                },
                {
                    name: '反应速度',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    symbolSize: 6,
                    lineStyle: { width: 3 },
                    data: data.map(item => item.reaction)
                },
                {
                    name: '准确度',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    symbolSize: 6,
                    lineStyle: { width: 3 },
                    data: data.map(item => item.accuracy)
                },
                {
                    name: '创造力',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    symbolSize: 6,
                    lineStyle: { width: 3 },
                    data: data.map(item => item.creativity)
                }
            ]
        };

        chart.setOption(option);

        // Resize handler
        const handleResize = () => chart.resize();
        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.dispose();
        };
    }, []);

    return (
        <div className="flex flex-col h-full bg-white animate-fade-in">
           <Header onBack={onBack} title={`${child.name}的成长报告`} />

           <div className="flex-1 overflow-y-auto pb-24 px-4 py-6 no-scrollbar">
                {/* Title Section */}
                <div className="mb-6 flex justify-between items-end">
                    <div>
                        <h2 className="text-xl font-bold text-gray-800">能力五维图</h2>
                        <p className="text-xs text-gray-400 mt-1">近30天能力发展趋势追踪</p>
                    </div>
                    <div className="text-right">
                         <span className="text-[10px] bg-orange-100 text-orange-600 px-2 py-1 rounded-lg font-bold">
                             {child.age}岁 · {child.gender === 'boy' ? '男孩' : '女孩'}
                         </span>
                    </div>
                </div>

                {/* ECharts Container */}
                <div className="w-full bg-white p-2 rounded-2xl shadow-sm border border-gray-50 mb-6">
                     {/* Added inline style to ensure height is not zero */}
                     <div ref={chartRef} style={{ width: '100%', height: '350px' }} />
                </div>
           </div>
        </div>
    );
}
