
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { fetchRandomQuote } from '../services/miniprogramQuotes';
import { ChevronLeftIcon, CameraIcon, TrashIcon, RefreshIcon, PlusIcon, SparklesIcon } from './Icons';

interface DiaryEditorProps {
    date: string;
    childId: string;
    initialText?: string;
    initialImage?: string;
    onConfirm: (text: string, image?: string) => void;
    onClose: () => void;
}

const QUOTE_SCENE = "育儿日记";

const FALLBACK_DIARY_PROMPTS = [
    "今天虽然很累，但看到孩子的笑容觉得一切都值得。",
    "发火后很后悔，但也许这也是我成长的机会。",
    "孩子的一个小进步，让我惊喜了好久。",
    "不仅是养育孩子，也是在养育那个曾经小小的自己。",
    "放慢脚步，听听孩子心里的话。",
    "最好的爱是陪伴，今天我做到了吗？"
];

export const DiaryEditor: React.FC<DiaryEditorProps> = ({ date, childId, initialText, initialImage, onConfirm, onClose }) => {
    const [text, setText] = useState(initialText || '');
    const [image, setImage] = useState<string | null>(initialImage || null);
    const [, setFallbackPromptIdx] = useState(0);
    const [capsuleText, setCapsuleText] = useState(FALLBACK_DIARY_PROMPTS[0]);
    const [isCapsuleLoading, setIsCapsuleLoading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const useNextFallbackPrompt = useCallback(() => {
        setFallbackPromptIdx((prev) => {
            const next = (prev + 1) % FALLBACK_DIARY_PROMPTS.length;
            setCapsuleText(FALLBACK_DIARY_PROMPTS[next]);
            return next;
        });
    }, []);

    const refreshCapsule = useCallback(async (signal?: AbortSignal) => {
        const childIdNumber = Number.parseInt(childId, 10);
        if (!Number.isFinite(childIdNumber) || childIdNumber <= 0) {
            useNextFallbackPrompt();
            return;
        }

        setIsCapsuleLoading(true);
        try {
            const quote = await fetchRandomQuote(childIdNumber, QUOTE_SCENE, { signal });
            if (quote) {
                setCapsuleText(quote);
                return;
            }
            useNextFallbackPrompt();
        } catch {
            useNextFallbackPrompt();
        } finally {
            setIsCapsuleLoading(false);
        }
    }, [childId, useNextFallbackPrompt]);

    useEffect(() => {
        const controller = new AbortController();
        refreshCapsule(controller.signal);
        return () => controller.abort();
    }, [refreshCapsule]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setImage(reader.result as string);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleUsePrompt = () => {
        setText(capsuleText);
    };

    const handleNextPrompt = () => {
        if (isCapsuleLoading) return;
        refreshCapsule();
    };

    const handleConfirm = () => {
        if (!text.trim() && !image) return;
        onConfirm(text, image || undefined);
    };

    return (
        <div className="absolute inset-0 z-[60] bg-white flex flex-col animate-fade-in">
            {/* Header */}
            <div className="px-4 py-4 flex items-center justify-between bg-white border-b border-gray-50">
                <button onClick={onClose} className="p-1 text-gray-700">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <h1 className="text-gray-900 font-bold text-base">
                    {date.replace(/-/g, '/')}育儿日记
                </h1>
                <div className="w-8"></div>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
                
                {/* Prompts Section (Moved Up) */}
                <div className="mb-6">
                    <div className="flex items-center gap-2 mb-3 px-1">
                        <SparklesIcon className="w-4 h-4 text-orange-400" />
                        <span className="text-xs font-bold text-gray-500">灵感胶囊</span>
                    </div>
                    <div className="bg-[#FFF9F2] rounded-xl p-4 border border-orange-100 relative">
                        <p className="text-sm text-gray-700 font-medium pr-8 leading-relaxed">
                            {capsuleText}
                        </p>
                        
                        <div className="flex items-center gap-3 mt-4">
                            <button 
                                onClick={handleUsePrompt}
                                className="bg-orange-500 text-white text-xs px-3 py-1.5 rounded-lg font-bold shadow-sm active:scale-95"
                            >
                                一键使用
                            </button>
                            <button 
                                onClick={handleNextPrompt}
                                className="text-orange-400 text-xs px-2 py-1.5 font-bold flex items-center gap-1 active:scale-95 disabled:opacity-50"
                                disabled={isCapsuleLoading}
                            >
                                <RefreshIcon className={`w-3 h-3 ${isCapsuleLoading ? 'animate-spin' : ''}`} />
                                {isCapsuleLoading ? '获取中' : '换一句'}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Text Area (Moved Down) */}
                <div className="relative mb-8">
                    <textarea
                        className="w-full h-40 p-4 bg-gray-50 rounded-2xl text-base text-gray-800 placeholder-gray-400 outline-none focus:ring-2 focus:ring-orange-100 resize-none"
                        placeholder="记录当下的感受，哪怕只是只言片语..."
                        maxLength={200}
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                    />
                    <div className="absolute bottom-3 right-4 text-xs text-gray-400">
                        {text.length}/200
                    </div>
                </div>

                {/* Image Upload */}
                <div>
                     <div className="flex items-center gap-2 mb-3 px-1">
                        <CameraIcon className="w-4 h-4 text-gray-500" />
                        <span className="text-xs font-bold text-gray-500">定格瞬间 (可选)</span>
                    </div>
                    
                    <input 
                        type="file" 
                        ref={fileInputRef}
                        accept="image/*"
                        className="hidden"
                        onChange={handleFileChange}
                    />

                    {image ? (
                        <div className="relative w-full aspect-video rounded-2xl overflow-hidden shadow-sm group">
                            <img src={image} alt="uploaded" className="w-full h-full object-cover" />
                            <button 
                                onClick={() => setImage(null)}
                                className="absolute top-2 right-2 p-2 bg-black/50 text-white rounded-full hover:bg-red-500/80 transition-colors"
                            >
                                <TrashIcon className="w-5 h-5" />
                            </button>
                        </div>
                    ) : (
                        <button 
                            onClick={() => fileInputRef.current?.click()}
                            className="w-full h-32 bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center gap-2 text-gray-400 hover:bg-gray-100 hover:border-orange-200 hover:text-orange-400 transition-all"
                        >
                            <PlusIcon className="w-8 h-8 opacity-50" />
                            <span className="text-xs font-bold">添加照片</span>
                        </button>
                    )}
                </div>
            </div>

            {/* Footer */}
            <div className="px-6 pb-10 pt-4 bg-white border-t border-gray-50">
                <button 
                    onClick={handleConfirm}
                    disabled={!text.trim() && !image}
                    className={`w-full py-4 rounded-full font-bold text-base transition-all active:scale-[0.98] ${
                        text.trim() || image
                        ? 'bg-orange-500 text-white shadow-lg shadow-orange-200'
                        : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                    }`}
                >
                    完成日记
                </button>
            </div>
        </div>
    );
};
