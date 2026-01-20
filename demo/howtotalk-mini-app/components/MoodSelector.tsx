
import React, { useState } from 'react';
import { ChevronLeftIcon } from './Icons';
import { MOOD_DATA, MoodFace } from './MoodResources';

interface MoodSelectorProps {
    date: string;
    initialMood?: string;
    onConfirm: (mood: string) => void;
    onClose: () => void;
}

export const MoodSelector: React.FC<MoodSelectorProps> = ({ date, initialMood, onConfirm, onClose }) => {
    // Find initial selected ID or default to 'happy'
    // This supports both passing an ID or a Label for backward compatibility, though ID is preferred
    const defaultSelected = MOOD_DATA.find(m => m.id === initialMood || m.label === initialMood)?.id || 'happy';
    const [selectedId, setSelectedId] = useState<string>(defaultSelected);

    const selectedMood = MOOD_DATA.find(m => m.id === selectedId) || MOOD_DATA[0];

    const handleConfirm = () => {
        // Return ID instead of label to ensure easier resource lookup elsewhere
        onConfirm(selectedMood.id);
    };

    return (
        <div className="absolute inset-0 z-[60] bg-white flex flex-col animate-fade-in">
            {/* Header */}
            <div className="px-4 py-4 flex items-center justify-between bg-white relative">
                <button onClick={onClose} className="p-1 text-gray-700">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <h1 className="text-gray-900 font-bold text-base leading-tight">
                    {date.replace(/-/g, '/')}育儿状态
                </h1>
                {/* Spacer to balance the header since X button is removed */}
                <div className="w-8"></div>
            </div>

            {/* Title Quote */}
            <div className="px-8 mt-8 mb-4 text-center animate-fade-in-up">
                <h2 className="text-orange-500 font-bold text-lg leading-relaxed">
                    今天的你是充满能量，还是快没电了<br/>
                    我们会悄悄记住你的辛苦<br/>
                    并为你点亮一盏理解的灯
                </h2>
            </div>

            {/* Mood Circle Container */}
            <div className="flex-1 flex items-center justify-center relative min-h-[400px]">
                {/* Center Display (Selected Mood) */}
                <div className="absolute z-20 flex flex-col items-center justify-center animate-fade-in">
                    <div className={`w-32 h-32 rounded-full flex items-center justify-center shadow-xl transition-all duration-300 ${selectedMood.color} ${selectedMood.textColor} ring-4 ring-white`}>
                        <MoodFace id={selectedMood.id} className="w-20 h-20" />
                    </div>
                    <div className="mt-6 text-center animate-fade-in">
                        <p className="text-sm font-bold text-gray-800 leading-tight tracking-wider">让情绪被看见</p>
                        <p className="text-sm font-bold text-gray-800 leading-tight mt-1.5 tracking-wider">也让爱被察觉</p>
                    </div>
                </div>

                {/* Orbiting Options */}
                <div className="relative w-[340px] h-[340px]">
                    {MOOD_DATA.map((mood, index) => {
                        // Calculate position
                        const total = MOOD_DATA.length;
                        const angleDeg = (index * (360 / total)) - 90; // Start from top
                        const angleRad = angleDeg * (Math.PI / 180);
                        const radius = 42; // percentage
                        const x = 50 + radius * Math.cos(angleRad);
                        const y = 50 + radius * Math.sin(angleRad);
                        
                        const isSelected = selectedId === mood.id;

                        return (
                            <button
                                key={mood.id}
                                onClick={() => setSelectedId(mood.id)}
                                className={`absolute flex flex-col items-center justify-center transition-all duration-300 transform -translate-x-1/2 -translate-y-1/2 group`}
                                style={{ 
                                    left: `${x}%`,
                                    top: `${y}%`,
                                    zIndex: 10
                                }}
                            >
                                <div 
                                    className={`
                                        w-14 h-14 rounded-full flex items-center justify-center shadow-sm transition-all duration-300
                                        ${mood.color} ${mood.textColor}
                                        ${isSelected 
                                            ? 'scale-110 ring-2 ring-offset-2 ring-orange-300 shadow-lg' 
                                            : 'opacity-80 hover:opacity-100 hover:scale-105'
                                        }
                                    `}
                                >
                                    <MoodFace id={mood.id} className="w-8 h-8" />
                                </div>
                                <span className={`mt-1 text-[10px] font-medium transition-all duration-300 ${isSelected ? 'text-orange-600 font-bold scale-110' : 'text-gray-400'}`}>
                                    {mood.label}
                                </span>
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* Confirm Button */}
            <div className="px-6 pb-10 pt-4">
                <button 
                    onClick={handleConfirm}
                    className="w-full bg-orange-500 text-white py-4 rounded-full font-bold text-base shadow-lg shadow-orange-200 active:scale-[0.98] transition-all"
                >
                    确 定
                </button>
            </div>
        </div>
    );
};
