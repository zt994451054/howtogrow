
import React from 'react';

export interface MoodData {
    id: string;
    label: string;
    color: string;
    textColor?: string;
}

// Exactly 10 moods configuration
export const MOOD_DATA: MoodData[] = [
    { id: 'disappointed', label: '失望的', color: 'bg-[#B0A7CA]', textColor: 'text-white' },
    { id: 'calm', label: '平静的', color: 'bg-[#3D5A45]', textColor: 'text-white' },
    { id: 'optimistic', label: '乐观的', color: 'bg-[#FBC676]', textColor: 'text-white' },
    { id: 'happy', label: '开心的', color: 'bg-[#F97316]', textColor: 'text-white' },
    { id: 'sad', label: '难过的', color: 'bg-[#6CA0B6]', textColor: 'text-white' }, // Blueish
    { id: 'worried', label: '担忧的', color: 'bg-[#768C7F]', textColor: 'text-white' },
    { id: 'helpless', label: '无奈的', color: 'bg-[#8B7D6B]', textColor: 'text-white' },
    { id: 'angry', label: '愤怒的', color: 'bg-[#8E6C88]', textColor: 'text-white' },
    { id: 'relieved', label: '欣慰的', color: 'bg-[#F4AFA8]', textColor: 'text-white' },
    { id: 'desperate', label: '绝望的', color: 'bg-[#556980]', textColor: 'text-white' }, // Darker Grey/Blue
];

// SVG Face Component
export const MoodFace: React.FC<{ id: string; className?: string }> = ({ id, className = "w-full h-full" }) => {
    const strokeProps = { stroke: "white", strokeWidth: "2.5", strokeLinecap: "round" as const, strokeLinejoin: "round" as const, fill: "none" };
    const fillProps = { fill: "white" };

    const renderFace = () => {
        switch (id) {
            case 'happy': // Winking
                return (
                    <>
                        <circle cx="9" cy="9" r="1.5" {...fillProps} />
                        {/* Wink */}
                        <path d="M15 9.5C15.5 8 18.5 8 19 9.5" {...strokeProps} />
                        {/* Smile */}
                        <path d="M7 14.5C9 17.5 15 17.5 17 14.5" {...strokeProps} />
                    </>
                );
            case 'optimistic': // Smile
                return (
                    <>
                        <circle cx="9" cy="9" r="1.5" {...fillProps} />
                        <circle cx="15" cy="9" r="1.5" {...fillProps} />
                        <path d="M7 14C9 16.5 15 16.5 17 14" {...strokeProps} />
                    </>
                );
            case 'calm': // Straight
                return (
                    <>
                        <circle cx="9" cy="9" r="1.5" {...fillProps} />
                        <circle cx="15" cy="9" r="1.5" {...fillProps} />
                        <path d="M9 15H15" {...strokeProps} />
                    </>
                );
            case 'disappointed': // Sad
                return (
                    <>
                        <circle cx="9" cy="10" r="1.5" {...fillProps} />
                        <circle cx="15" cy="10" r="1.5" {...fillProps} />
                        {/* Brows */}
                        <path d="M7 7L10 8" {...strokeProps} strokeWidth="2" />
                        <path d="M17 7L14 8" {...strokeProps} strokeWidth="2" />
                        {/* Mouth */}
                        <path d="M9 16C10 15 14 15 15 16" {...strokeProps} />
                    </>
                );
            case 'sad': // Crying
                return (
                    <>
                        <circle cx="8" cy="9" r="1.5" {...fillProps} />
                        <circle cx="16" cy="9" r="1.5" {...fillProps} />
                        {/* Brows */}
                        <path d="M6 7C7 6 9 6 10 7" {...strokeProps} strokeWidth="2" />
                        <path d="M14 7C15 6 17 6 18 7" {...strokeProps} strokeWidth="2" />
                        {/* Mouth */}
                        <path d="M9 16C10 15 14 15 15 16" {...strokeProps} />
                        {/* Tear */}
                        <path d="M16 11.5C16 11.5 15.5 12.5 15.5 13.5C15.5 14.3 16.2 14.5 16.5 14.5C16.8 14.5 17.5 14.3 17.5 13.5C17.5 12.5 17 11.5 17 11.5" fill="white" />
                    </>
                );
            case 'worried': // Worried
                return (
                    <>
                        <circle cx="9" cy="10" r="1.5" {...fillProps} />
                        <circle cx="15" cy="10" r="1.5" {...fillProps} />
                         {/* Brows */}
                        <path d="M7 7C8 8 10 8 11 7" {...strokeProps} strokeWidth="2" />
                        <path d="M13 7C14 8 16 8 17 7" {...strokeProps} strokeWidth="2" />
                        <path d="M10 15L12 14L14 15" {...strokeProps} />
                    </>
                );
            case 'helpless': // Side glance / straight
                return (
                    <>
                         {/* Eyes look side */}
                        <path d="M8 9H10" {...strokeProps} />
                        <path d="M14 9H16" {...strokeProps} />
                        <path d="M9 15H15" {...strokeProps} />
                        {/* Sweat */}
                        <path d="M18 10C18 10 17.5 11 17.5 12C17.5 12.5 18 12.8 18.5 12.8C19 12.8 19.5 12.5 19.5 12C19.5 11 19 10 19 10" fill="white" />
                    </>
                );
            case 'angry': // Angry
                return (
                    <>
                        <circle cx="9" cy="10" r="1.5" {...fillProps} />
                        <circle cx="15" cy="10" r="1.5" {...fillProps} />
                        {/* Angry Brows */}
                        <path d="M7 8L10 9" {...strokeProps} strokeWidth="2" />
                        <path d="M17 8L14 9" {...strokeProps} strokeWidth="2" />
                        {/* Mouth */}
                        <path d="M10 15C11 14.5 13 14.5 14 15" {...strokeProps} />
                    </>
                );
            case 'relieved': // Happy Closed Eyes
                return (
                    <>
                        {/* Closed Eyes */}
                        <path d="M7 10C8 8.5 10 8.5 11 10" {...strokeProps} />
                        <path d="M13 10C14 8.5 16 8.5 17 10" {...strokeProps} />
                        <path d="M9 14C10 15 14 15 15 14" {...strokeProps} />
                    </>
                );
            case 'desperate': // Tired / Hollow
                return (
                    <>
                        {/* Hollow Eyes */}
                        <circle cx="9" cy="9" r="1.5" {...fillProps} />
                        <circle cx="15" cy="9" r="1.5" {...fillProps} />
                        {/* Bags under eyes */}
                        <path d="M8 11C8.5 11.5 9.5 11.5 10 11" {...strokeProps} strokeWidth="1" />
                        <path d="M14 11C14.5 11.5 15.5 11.5 16 11" {...strokeProps} strokeWidth="1" />
                        <path d="M9 15C10 14 14 14 15 15" {...strokeProps} />
                    </>
                );
            default:
                return null;
        }
    };

    return (
        <svg viewBox="0 0 24 24" className={className} xmlns="http://www.w3.org/2000/svg">
            {renderFace()}
        </svg>
    );
};
