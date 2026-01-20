
import React from 'react';
import { BannerItem } from '../types';
import { ChevronLeftIcon } from './Icons';

interface ArticleDetailProps {
    banner: BannerItem;
    onBack: () => void;
}

export const ArticleDetail: React.FC<ArticleDetailProps> = ({ banner, onBack }) => {
    return (
        <div className="flex flex-col h-full bg-white relative animate-fade-in z-50">
            {/* Header */}
            <div className="px-4 py-4 flex items-center bg-white sticky top-0 z-20 border-b border-gray-100">
                <button 
                    onClick={onBack} 
                    className="p-1 -ml-2 text-gray-700 hover:text-orange-500 transition-colors"
                >
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
                <h1 className="flex-1 text-center text-gray-900 font-bold text-base leading-tight pr-6 truncate">
                    {banner.title}
                </h1>
            </div>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto no-scrollbar pb-24">
                {/* Hero Image */}
                <div className="w-full h-48 sm:h-56 relative">
                    <img 
                        src={banner.imageUrl} 
                        alt={banner.title} 
                        className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                </div>

                {/* Article Body */}
                <div className="px-6 py-6">
                    {/* Meta Info */}
                    <div className="mb-6">
                        <h2 className="text-2xl font-bold text-gray-900 leading-snug mb-3">
                            {banner.title}
                        </h2>
                        <div className="flex items-center gap-3 text-xs text-gray-400">
                            {banner.author && (
                                <span className="bg-orange-50 text-orange-600 px-2 py-0.5 rounded font-bold">
                                    {banner.author}
                                </span>
                            )}
                            {banner.publishDate && (
                                <span>{banner.publishDate}</span>
                            )}
                        </div>
                    </div>

                    {/* Rich Text Content */}
                    <div 
                        className="prose prose-orange prose-sm sm:prose-base max-w-none text-gray-600 leading-7"
                        dangerouslySetInnerHTML={{ __html: banner.content }}
                    />
                </div>
            </div>
        </div>
    );
};
