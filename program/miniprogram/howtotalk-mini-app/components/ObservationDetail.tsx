
import React, { useState, useEffect } from 'react';
import { ObservationRecord } from '../types';
import { 
    ChevronLeftIcon, 
    PuzzlePieceIcon, ClipboardDocumentListIcon, BookOpenIcon, AcademicCapIcon,
    SmileLogo 
} from './Icons';
import { MoodSelector } from './MoodSelector';
import { MOOD_DATA, MoodFace } from './MoodResources';
import { FrustrationSelector } from './FrustrationSelector';
import { FRUSTRATION_DATA, FrustrationIcon } from './FrustrationResources';
import { DiaryEditor } from './DiaryEditor';

// --- Sub-component: Timeline Item ---
interface TimelineItemProps {
    isDone: boolean;
    icon: React.ReactNode;
    title: string;
    content: string;
    circleColor?: string;
    isLast?: boolean;
}

const TimelineItem: React.FC<TimelineItemProps> = ({ isDone, icon, title, content, circleColor, isLast }) => {
    return (
        <div className={`relative flex items-start gap-4 mb-8 ${isLast ? 'mb-0' : ''}`}>
             {/* Icon Circle */}
             <div className={`z-10 w-14 h-14 rounded-full flex items-center justify-center border-4 shadow-sm shrink-0 transition-all duration-300 ${
                 isDone 
                 ? (circleColor ? `${circleColor} text-white border-white` : 'bg-orange-50 border-orange-100 text-orange-500')
                 : 'bg-white border-gray-100 text-gray-300'
             }`}>
                <div className="w-8 h-8 flex items-center justify-center">
                    {icon}
                </div>
             </div>

             {/* Text Content */}
             <div className="flex-1 pt-1.5">
                 <h3 className={`font-bold text-sm mb-1 transition-colors duration-300 ${isDone ? 'text-gray-900' : 'text-gray-400'}`}>
                     {title}
                 </h3>
                 <p className={`text-xs leading-relaxed transition-colors duration-300 text-justify ${isDone ? 'text-gray-600' : 'text-gray-300'}`}>
                     {content}
                 </p>
             </div>
        </div>
    );
};

interface ObservationDetailProps {
    data: ObservationRecord;
    childName: string;
    childId: string;
    onBack: () => void;
    onUpdateRecord?: (id: string, updates: Partial<ObservationRecord>) => void;
    onNavigate: (tab: string, params?: Record<string, any>) => void;
    initialAction?: 'mood' | null;
}

export const ObservationDetail: React.FC<ObservationDetailProps> = ({ 
    data, 
    childName, 
    childId, 
    onBack, 
    onUpdateRecord,
    onNavigate,
    initialAction
}) => {
    // Format Date: 2025/12/01
    const formattedDate = data.date.replace(/-/g, '/');
    const isDone = data.isDone;
    
    // State for Mood Selector Overlay
    const [showMoodSelector, setShowMoodSelector] = useState(false);
    // State for Frustration Selector Overlay
    const [showFrustrationSelector, setShowFrustrationSelector] = useState(false);
    // State for Diary Editor Overlay
    const [showDiaryEditor, setShowDiaryEditor] = useState(false);

    // Auto-open logic
    useEffect(() => {
        if (initialAction === 'mood') {
            setShowMoodSelector(true);
        }
    }, [initialAction]);

    // Theme Colors based on isDone status
    const theme = {
        quote: "text-gray-900", // Changed to always be dark/black
        line: isDone ? "border-orange-300" : "border-gray-200",
        lineType: isDone ? "border-solid" : "border-dashed",
    };

    // Retrieve full mood object based on ID
    const currentMoodObj = MOOD_DATA.find(m => m.id === data.mood);

    // Retrieve frustration objects based on array
    const frustrationIds = data.frustrationIds || [];
    // Just grab the first one for the main icon display, or generic if mixed
    const primaryFrustrationId = frustrationIds.length > 0 ? frustrationIds[0] : null;
    const currentFrustrationObj = primaryFrustrationId ? FRUSTRATION_DATA.find(f => f.id === primaryFrustrationId) : null;

    const handleMoodConfirm = (newMoodId: string) => {
        if (onUpdateRecord) {
            onUpdateRecord(data.id, {
                mood: newMoodId, 
                isDone: true, 
                moodDescription: "状态已更新。觉察当下的情绪，是改变的开始。",
                title: "已完成今日觉察" 
            });
        }
        setShowMoodSelector(false);
    };

    const handleFrustrationConfirm = (ids: string[], labels: string[]) => {
        if (onUpdateRecord) {
             const joinedLabels = labels.join('、');
             onUpdateRecord(data.id, {
                frustrationIds: ids,
                frustrationText: `${joinedLabels} - 意识到问题所在是解决的第一步。`, 
                isDone: true 
             });
        }
        setShowFrustrationSelector(false);
    };

    const handleDiaryConfirm = (text: string, image?: string) => {
        if (onUpdateRecord) {
             const updates: Partial<ObservationRecord> = {
                diaryText: text, 
                isDone: true
             };
             if (image) {
                 updates.imageUrl = image; // Use uploaded image as cover
             }
             onUpdateRecord(data.id, updates);
        }
        setShowDiaryEditor(false);
    };

    // Navigation Handler for Behavior Mirror
    const handleBehaviorMirrorClick = () => {
        onNavigate('test', { 
            action: 'check_test_status', 
            childId: childId,
            date: data.date,
            returnTarget: 'observation_detail' // Flag to return here after test
        });
    };

    // Navigation Handler for Expert Whisper
    const handleExpertWhisperClick = () => {
        onNavigate('test', { 
            action: 'view_result', 
            childId: childId,
            date: data.date,
            returnTarget: 'observation_detail' // Flag to return here after viewing result
        });
    };

    return (
        <div className="flex flex-col h-full bg-white relative animate-fade-in z-50">
            {/* Header - Centered Title with Absolute Back Button */}
            <div className="px-4 py-4 flex items-center justify-center bg-white sticky top-0 z-20 relative">
                <button onClick={onBack} className="absolute left-4 p-1 text-gray-700 z-10">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <h1 className="text-gray-900 font-bold text-base leading-tight">
                    {formattedDate}每日觉察
                </h1>
            </div>

            <div className="flex-1 overflow-y-auto no-scrollbar px-6 pb-24">
                {/* Greeting - Centered & Styled */}
                <div className="flex justify-center mb-8 mt-2">
                    <span className="text-xs font-medium tracking-widest text-gray-900">
                        {childName}妈妈，您好
                    </span>
                </div>

                {/* Orange Quote */}
                <div className="mb-12">
                    <h2 className={`${theme.quote} text-lg font-bold leading-relaxed whitespace-pre-line text-center transition-colors duration-300`}>
                        {data.summaryQuote || "你记下的每个烦躁瞬间\n都是写给孩子未来的一封信\n“看，爸爸妈妈也在学者长大”"}
                    </h2>
                </div>

                {/* Timeline Section */}
                <div className="relative pl-4">
                    {/* Vertical Line */}
                    <div className={`absolute left-[27px] top-6 bottom-10 w-0.5 border-l-2 ${theme.line} ${theme.lineType} z-0 transition-colors duration-300`}></div>

                    {/* 1. Parenting Status (Clickable) */}
                    <div onClick={() => setShowMoodSelector(true)} className="cursor-pointer active:scale-95 transition-transform">
                        <TimelineItem 
                            isDone={isDone}
                            icon={
                                isDone && currentMoodObj ? (
                                    <MoodFace id={currentMoodObj.id} className="w-8 h-8" />
                                ) : (
                                    <SmileLogo className="w-6 h-6 text-gray-300 grayscale opacity-50" />
                                )
                            }
                            // Pass the mood color to background if it exists
                            circleColor={currentMoodObj?.color}
                            title="育儿状态"
                            content={data.moodDescription || "今天的你是温柔耐心的爸妈，还是被气到想“重启系统”？"}
                        />
                    </div>

                    {/* 2. Frustration Archive (Clickable) */}
                    <div onClick={() => setShowFrustrationSelector(true)} className="cursor-pointer active:scale-95 transition-transform">
                        <TimelineItem 
                            isDone={isDone}
                            icon={
                                currentFrustrationObj ? (
                                    <div className="text-orange-500 w-full h-full p-2.5 relative">
                                        <FrustrationIcon id={currentFrustrationObj.id} />
                                        {/* Multi-select indicator badge */}
                                        {frustrationIds.length > 1 && (
                                            <div className="absolute -top-1 -right-1 w-4 h-4 bg-orange-600 text-white text-[9px] flex items-center justify-center rounded-full border border-white">
                                                +{frustrationIds.length - 1}
                                            </div>
                                        )}
                                    </div>
                                ) : (
                                    <PuzzlePieceIcon className={`w-6 h-6 ${isDone ? 'text-orange-500' : 'text-gray-300'}`} />
                                )
                            }
                            title="烦恼存档"
                            content={data.frustrationText || "拖拉磨蹭，情绪失控，隔代教育矛盾不断"}
                        />
                    </div>

                    {/* 3. Behavior Mirror (Clickable) */}
                    <div onClick={handleBehaviorMirrorClick} className="cursor-pointer active:scale-95 transition-transform">
                        <TimelineItem 
                            isDone={isDone}
                            icon={<ClipboardDocumentListIcon className={`w-6 h-6 ${isDone ? 'text-orange-500' : 'text-gray-300'}`} />}
                            title="行为镜子"
                            content={data.mirrorText || "一条建议，照亮明天的方向。我们不教你应该怎样，只陪你一起发现“原来我还可以这样”"}
                        />
                    </div>

                    {/* 4. Parenting Diary (Clickable) */}
                    <div onClick={() => setShowDiaryEditor(true)} className="cursor-pointer active:scale-95 transition-transform">
                        <TimelineItem 
                            isDone={isDone}
                            icon={<BookOpenIcon className={`w-6 h-6 ${isDone ? 'text-orange-500' : 'text-gray-300'}`} />}
                            title="育儿日记"
                            content={data.diaryText || "别一个人扛，写下来，不是抱怨，而是一次自我梳理，也可能是改变的起点"}
                        />
                    </div>

                    {/* 5. Expert Whisper (Clickable) */}
                    <div onClick={handleExpertWhisperClick} className="cursor-pointer active:scale-95 transition-transform">
                        <TimelineItem 
                            isDone={isDone}
                            icon={<AcademicCapIcon className={`w-6 h-6 ${isDone ? 'text-orange-500' : 'text-gray-300'}`} />}
                            title="专家悄悄话 · 今日总结"
                            content={data.expertText || "您的专属家庭教育指导师可以帮你快速总结今天的得失"}
                            isLast={true}
                        />
                    </div>
                </div>
            </div>

            {/* Mood Selector Overlay */}
            {showMoodSelector && (
                <MoodSelector 
                    date={data.date}
                    initialMood={data.mood}
                    onConfirm={handleMoodConfirm}
                    onClose={() => setShowMoodSelector(false)}
                />
            )}

            {/* Frustration Selector Overlay */}
            {showFrustrationSelector && (
                <FrustrationSelector
                    date={data.date}
                    initialSelection={data.frustrationIds}
                    onConfirm={handleFrustrationConfirm}
                    onClose={() => setShowFrustrationSelector(false)}
                />
            )}

            {/* Diary Editor Overlay */}
            {showDiaryEditor && (
                <DiaryEditor 
                    date={data.date}
                    childId={childId}
                    initialText={data.diaryText}
                    onConfirm={handleDiaryConfirm}
                    onClose={() => setShowDiaryEditor(false)}
                />
            )}
        </div>
    );
};
