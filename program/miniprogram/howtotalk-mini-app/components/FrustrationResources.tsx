
import React from 'react';

export interface FrustrationData {
    id: string;
    label: string;
}

// Extended to 15 Scenarios to show pagination
export const FRUSTRATION_DATA: FrustrationData[] = [
    { id: 'eating', label: '吃饭磨蹭' },
    { id: 'sleeping', label: '拒绝睡觉' },
    { id: 'screen', label: '沉迷屏幕' },
    { id: 'emotion', label: '情绪崩溃' },
    { id: 'conflict', label: '顶嘴冲突' },
    { id: 'study', label: '辅导作业' },
    { id: 'sibling', label: '二胎打闹' },
    { id: 'hygiene', label: '不爱卫生' },
    { id: 'activity', label: '缺乏运动' }, // New
    { id: 'social', label: '社交退缩' }, // New
    { id: 'lie', label: '撒谎隐瞒' }, // New
    { id: 'rude', label: '举止粗鲁' }, // New
    { id: 'clingy', label: '过分粘人' }, // New
    { id: 'anxiety', label: '分离焦虑' }, // New
    { id: 'other', label: '其他烦恼' },
];

export const FrustrationIcon: React.FC<{ id: string; className?: string }> = ({ id, className = "w-full h-full" }) => {
    const strokeProps = { stroke: "currentColor", strokeWidth: "1.5", strokeLinecap: "round" as const, strokeLinejoin: "round" as const, fill: "none" };
    
    // Helper for generic icon shapes
    const renderIcon = () => {
        switch (id) {
            case 'eating': // Apple
                return (
                    <>
                     <path d="M12 20.94c1.5 0 3-.6 3-2.5 0-1.2.8-2.5 2.5-2.5 3 0 4.5-2.5 4.5-5.5 0-4.5-3.5-8-7-8-2 0-3 1-4 2.5C10 3.4 9 2.44 7 2.44c-3.5 0-7 3.5-7 8 0 3 1.5 5.5 4.5 5.5 1.7 0 2.5 1.3 2.5 2.5 0 1.9 1.5 2.5 3 2.5z" />
                     <path d="M12 4.44c.5-1.5 2-2 2-2" />
                    </>
                );
            case 'sleeping': // Moon
                return <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />;
            case 'screen': // Tablet
                return (
                    <>
                        <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
                        <line x1="12" y1="18" x2="12.01" y2="18" strokeWidth="2" />
                    </>
                );
            case 'emotion': // Sad Face
                return (
                    <>
                        <circle cx="12" cy="12" r="10" />
                        <line x1="8" y1="9" x2="8.01" y2="9" strokeWidth="2" />
                        <line x1="16" y1="9" x2="16.01" y2="9" strokeWidth="2" />
                        <path d="M16 16s-1.5-2-4-2-4 2-4 2" />
                    </>
                );
            case 'conflict': // Swords
                return (
                    <>
                        <path d="M14.5 17.5L3 6V3h3l11.5 11.5" />
                        <path d="M13 19l6-6 5 5v3h-3l-8-2" />
                        <path d="M19 5l-5 5" />
                        <path d="M5 19l5-5" />
                    </>
                );
            case 'study': // Sparkles
                return <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />;
            case 'sibling': // People
                return (
                    <>
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                        <circle cx="9" cy="7" r="4" />
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                    </>
                );
            case 'hygiene': // Drop
                return <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />;
            case 'activity': // Ball
                return (
                    <>
                        <circle cx="12" cy="12" r="10" />
                        <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20" />
                        <path d="M2 12h20" />
                    </>
                );
            case 'social': // Chat bubble off
                return (
                    <>
                         <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                         <line x1="9" y1="10" x2="15" y2="10" />
                    </>
                );
            case 'lie': // Mask
                return (
                    <>
                         <path d="M2 12c0-3 2-6 10-6s10 3 10 6-4 6-10 6-10-3-10-6" />
                         <circle cx="8" cy="12" r="2" />
                         <circle cx="16" cy="12" r="2" />
                    </>
                );
            case 'rude': // Lightning
                return <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />;
            case 'clingy': // Heart connection
                return (
                    <>
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </>
                );
            case 'anxiety': // Cloud rain
                 return (
                    <>
                        <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242" />
                        <path d="M16 20v-4" />
                        <path d="M8 20v-4" />
                        <path d="M12 20v-4" />
                    </>
                 );
            case 'other': // Sun
            default:
                return (
                    <>
                        <circle cx="12" cy="12" r="5" />
                        <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
                    </>
                );
        }
    };

    return (
        <svg viewBox="0 0 24 24" className={className} xmlns="http://www.w3.org/2000/svg" {...strokeProps}>
            {renderIcon()}
        </svg>
    );
};
