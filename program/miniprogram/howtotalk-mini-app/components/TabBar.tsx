
import React from 'react';
import { HomeIcon, TestIcon, ChatBubbleIcon, UserIcon, ChartBarIcon, SmileLogo } from './Icons';

interface TabBarProps {
  activeTab: string;
  onChange: (tab: string) => void;
}

export const TabBar: React.FC<TabBarProps> = ({ activeTab, onChange }) => {
  return (
    <div className="absolute bottom-0 w-full bg-white border-t border-gray-100 h-[80px] z-30">
      <div className="relative w-full h-full flex items-end justify-around pb-4">
        
        {/* Left Items */}
        <button
          onClick={() => onChange('home')}
          className={`flex flex-col items-center gap-1 min-w-[64px] transition-colors duration-200 ${
            activeTab === 'home' ? 'text-orange-500' : 'text-gray-400 hover:text-gray-500'
          }`}
        >
          <HomeIcon className="w-6 h-6" />
          <span className="text-[10px] font-medium tracking-wide">每日觉察</span>
        </button>

        <button
          onClick={() => onChange('curve')}
          className={`flex flex-col items-center gap-1 min-w-[64px] transition-colors duration-200 ${
            activeTab === 'curve' ? 'text-orange-500' : 'text-gray-400 hover:text-gray-500'
          }`}
        >
          <ChartBarIcon className="w-6 h-6" />
          <span className="text-[10px] font-medium tracking-wide">家长曲线</span>
        </button>

        {/* Center Floating Button (Mood Check-in) - Text Removed */}
        <div className="relative -top-6">
           <button
             onClick={() => onChange('mood-checkin')}
             className="w-14 h-14 rounded-full bg-orange-500 shadow-lg shadow-orange-200 flex items-center justify-center text-white border-4 border-white transform transition-transform active:scale-95"
           >
              <SmileLogo className="w-8 h-8 text-white" />
           </button>
        </div>

        {/* Right Items */}
        <button
          onClick={() => onChange('chat')}
          className={`flex flex-col items-center gap-1 min-w-[64px] transition-colors duration-200 ${
            activeTab === 'chat' ? 'text-orange-500' : 'text-gray-400 hover:text-gray-500'
          }`}
        >
          <ChatBubbleIcon className="w-6 h-6" />
          <span className="text-[10px] font-medium tracking-wide">马上沟通</span>
        </button>

        <button
          onClick={() => onChange('me')}
          className={`flex flex-col items-center gap-1 min-w-[64px] transition-colors duration-200 ${
            activeTab === 'me' ? 'text-orange-500' : 'text-gray-400 hover:text-gray-500'
          }`}
        >
          <UserIcon className="w-6 h-6" />
          <span className="text-[10px] font-medium tracking-wide">我的</span>
        </button>

      </div>
    </div>
  );
};
