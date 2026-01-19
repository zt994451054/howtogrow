
import React, { useState } from 'react';
import { ChevronLeftIcon } from './Icons';
import { FRUSTRATION_DATA, FrustrationIcon } from './FrustrationResources';

interface FrustrationSelectorProps {
    date: string;
    initialSelection?: string[];
    onConfirm: (ids: string[], labels: string[]) => void;
    onClose: () => void;
}

export const FrustrationSelector: React.FC<FrustrationSelectorProps> = ({ date, initialSelection, onConfirm, onClose }) => {
    // Convert initial single ID (if any bug) or array to state
    const [selectedIds, setSelectedIds] = useState<string[]>(initialSelection || []);

    const toggleSelection = (id: string) => {
        setSelectedIds(prev => {
            if (prev.includes(id)) {
                return prev.filter(item => item !== id);
            } else {
                return [...prev, id];
            }
        });
    };

    const handleConfirm = () => {
        if (selectedIds.length > 0) {
            const selectedItems = FRUSTRATION_DATA.filter(item => selectedIds.includes(item.id));
            const labels = selectedItems.map(item => item.label);
            onConfirm(selectedIds, labels);
        }
    };

    return (
        <div className="absolute inset-0 z-[60] bg-white flex flex-col animate-fade-in">
            {/* Header */}
            <div className="px-4 py-4 flex items-center justify-between bg-white relative shrink-0">
                <button onClick={onClose} className="p-1 text-gray-700">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <h1 className="text-gray-900 font-bold text-base leading-tight">
                    {date.replace(/-/g, '/')}烦恼存档
                </h1>
                <div className="w-8"></div>
            </div>

            {/* Scrollable Content Container */}
            <div className="flex-1 overflow-y-auto no-scrollbar pb-6">
                {/* Top Banner Illustration */}
                <div className="flex flex-col items-center justify-center mt-4 mb-6 px-6">
                    <div className="w-32 h-32 bg-orange-100 rounded-full flex items-center justify-center mb-4 relative overflow-hidden">
                        <div className="absolute inset-0 grid grid-cols-2 grid-rows-2 opacity-50">
                            <div className="bg-orange-200"></div>
                            <div className="bg-orange-50"></div>
                            <div className="bg-orange-50"></div>
                            <div className="bg-orange-200"></div>
                        </div>
                        <div className="absolute bottom-0 right-4 w-10 h-14 bg-orange-500 rounded-t-full transform rotate-12 border-2 border-white"></div>
                    </div>
                    
                    <h2 className="text-center font-bold text-base text-orange-500 leading-relaxed">
                        那些让你心力交瘁的时刻<br/>
                        <span className="text-orange-400">记下来，不是抱怨</span><br/>
                        <span className="text-orange-400">是改变的起点</span>
                    </h2>
                </div>

                {/* Vertical Grid (Waterfall) */}
                <div className="px-6 grid grid-cols-3 gap-y-8 gap-x-4">
                    {FRUSTRATION_DATA.map((item) => {
                        const isSelected = selectedIds.includes(item.id);
                        return (
                            <button
                                key={item.id}
                                onClick={() => toggleSelection(item.id)}
                                className="flex flex-col items-center gap-3 group"
                            >
                                <div 
                                    className={`
                                        w-20 h-20 rounded-full flex items-center justify-center transition-all duration-200
                                        ${isSelected 
                                            ? 'bg-orange-50 border-2 border-orange-500 text-orange-500 shadow-md transform scale-105' 
                                            : 'bg-gray-50 border-2 border-transparent text-gray-400 hover:bg-gray-100'
                                        }
                                    `}
                                >
                                    <div className="w-10 h-10">
                                        <FrustrationIcon id={item.id} />
                                    </div>
                                </div>
                                <span 
                                    className={`text-xs font-bold transition-colors ${isSelected ? 'text-orange-500' : 'text-gray-500'}`}
                                >
                                    {item.label}
                                </span>
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* Confirm Button */}
            <div className="px-6 pb-10 pt-4 bg-white shrink-0 border-t border-gray-50">
                <button 
                    onClick={handleConfirm}
                    disabled={selectedIds.length === 0}
                    className={`
                        w-full py-4 rounded-full font-bold text-base shadow-lg transition-all active:scale-[0.98]
                        ${selectedIds.length > 0
                            ? 'bg-orange-500 text-white shadow-orange-200 hover:bg-orange-600' 
                            : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                        }
                    `}
                >
                    确 定 {selectedIds.length > 0 ? `(${selectedIds.length})` : ''}
                </button>
            </div>
        </div>
    );
};
