
import React, { useMemo } from 'react';
import { ChevronLeftIcon } from './Icons';
import { FRUSTRATION_DATA, FrustrationIcon } from './FrustrationResources';

interface FrustrationDistributionDetailProps {
    onBack: () => void;
    startDate: string;
    endDate: string;
}

export const FrustrationDistributionDetail: React.FC<FrustrationDistributionDetailProps> = ({ onBack, startDate, endDate }) => {
    // Generate mock statistics based on date range seed to ensure consistency
    const distribution = useMemo(() => {
        // Simple hash of dates to seed the random generator
        const seed = startDate.charCodeAt(startDate.length - 1) + endDate.charCodeAt(endDate.length - 1);
        
        const stats = FRUSTRATION_DATA.map((item, idx) => {
             // Generate a pseudo-random count between 0 and 25
             // Math.sin provides deterministic variability based on index and date seed
             const count = Math.floor(((Math.sin(idx + seed) + 1) * 12.5));
             return { ...item, count };
        });

        // Filter out 0 counts and sort by count descending (most frequent first)
        return stats.filter(i => i.count > 0).sort((a, b) => b.count - a.count);
    }, [startDate, endDate]);

    // Find max value for progress bar scaling
    const maxCount = Math.max(...distribution.map(d => d.count));

    return (
        <div className="flex flex-col h-full bg-white animate-fade-in relative z-50">
             {/* Header */}
             <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2 shrink-0 bg-white sticky top-0 z-20">
                <button onClick={onBack} className="p-1 -ml-2 text-gray-500 hover:text-orange-500 transition-colors">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <span className="font-bold text-gray-800 text-base">我的困扰分布</span>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-4 pb-24 no-scrollbar">
                <div className="mb-6 bg-orange-50 p-3 rounded-lg flex justify-between items-center">
                    <span className="text-xs font-bold text-orange-600">统计范围</span>
                    <span className="text-xs text-orange-800 bg-white/50 px-2 py-0.5 rounded">
                        {startDate.replace(/-/g, '.')} - {endDate.replace(/-/g, '.')}
                    </span>
                </div>

                <div className="space-y-5">
                    {distribution.map((item, index) => {
                        const isTop3 = index < 3;
                        const percentage = maxCount > 0 ? (item.count / maxCount) * 100 : 0;

                        return (
                            <div key={item.id} className="flex items-center gap-4 group">
                                {/* Rank Number */}
                                <div className={`w-6 text-center font-bold flex flex-col items-center justify-center shrink-0 ${
                                    index === 0 ? 'text-yellow-500 text-xl' :
                                    index === 1 ? 'text-gray-500 text-lg' :
                                    index === 2 ? 'text-orange-700 text-lg' :
                                    'text-gray-300 text-sm'
                                }`}>
                                    {index + 1}
                                </div>

                                {/* Icon Circle */}
                                <div className={`w-12 h-12 rounded-full flex items-center justify-center shrink-0 border-2 transition-colors ${
                                    isTop3 
                                    ? 'bg-orange-50 border-orange-100 text-orange-500' 
                                    : 'bg-gray-50 border-transparent text-gray-400 group-hover:bg-gray-100'
                                }`}>
                                     <div className="w-7 h-7">
                                         <FrustrationIcon id={item.id} />
                                     </div>
                                </div>

                                {/* Progress Bar & Labels */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between items-end mb-1.5">
                                        <span className={`text-sm font-bold truncate ${isTop3 ? 'text-gray-900' : 'text-gray-600'}`}>
                                            {item.label}
                                        </span>
                                        <span className={`text-xs font-bold ${isTop3 ? 'text-orange-500' : 'text-gray-400'}`}>
                                            {item.count}次
                                        </span>
                                    </div>
                                    
                                    <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
                                        <div 
                                            className={`h-full rounded-full transition-all duration-1000 ease-out ${
                                                index === 0 ? 'bg-gradient-to-r from-yellow-400 to-orange-500' :
                                                index === 1 ? 'bg-orange-400' :
                                                index === 2 ? 'bg-orange-300' :
                                                'bg-gray-300'
                                            }`}
                                            style={{ width: `${percentage}%` }}
                                        />
                                    </div>
                                </div>
                            </div>
                        );
                    })}

                    {distribution.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                             <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
                                <FrustrationIcon id="other" className="w-8 h-8 opacity-50" />
                             </div>
                             <p className="text-sm">这段时间没有记录到困扰哦~</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
