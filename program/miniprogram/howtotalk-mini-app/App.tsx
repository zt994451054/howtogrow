
import React, { useState } from 'react';
import { APP_TITLE, HERO_QUOTE, ABOUT_TEXT, COURSES, MOCK_CHILDREN, MOCK_RECORDS } from './constants';
import { SmileLogo } from './components/Icons';
import { CourseCard } from './components/CourseCard';
import { TabBar } from './components/TabBar';
import { DailyTest } from './components/DailyTest';
import { ChatInterface } from './components/ChatInterface';
import { Me } from './components/Me';
import { HomeObservation } from './components/HomeObservation';
import { ParentCurve } from './components/ParentCurve';
import { Child, TestRecord } from './types';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState('home');
  // Lifted state for children so it's shared between DailyTest and Me
  const [children, setChildren] = useState<Child[]>(MOCK_CHILDREN);
  // Track globally selected child for Home/Test consistency
  const [selectedChildId, setSelectedChildId] = useState<string | null>(MOCK_CHILDREN[0]?.id || null);
  
  // Shared state for Test Records
  const [testRecords, setTestRecords] = useState<TestRecord[]>(MOCK_RECORDS);

  // State to handle deep linking/navigation parameters between tabs
  // Changed to Record<string, any> to support flexible params like childId
  const [navParams, setNavParams] = useState<Record<string, any> | null>(null);

  const handleNavigate = (tabId: string, params?: Record<string, any>) => {
    // Special handling for Mood Check-in Middle Button
    if (tabId === 'mood-checkin') {
        const todayStr = new Date().toISOString().split('T')[0];
        setNavParams({ action: 'open_mood_selector', date: todayStr });
        setActiveTab('home');
        return;
    }

    if (params) {
      setNavParams(params);
    }
    setActiveTab(tabId);
  };

  const handleSaveTestRecord = (record: TestRecord) => {
      setTestRecords(prev => {
          // Remove existing record for same child/date if exists (update logic)
          const filtered = prev.filter(r => !(r.childName === record.childName && r.date === record.date));
          return [...filtered, record];
      });
  };

  const renderContent = () => {
    switch(activeTab) {
      case 'home':
        return (
          <HomeObservation 
            childrenData={children}
            selectedChildId={selectedChildId}
            onSelectChild={setSelectedChildId}
            onNavigate={handleNavigate}
            initialParams={navParams}
            onClearParams={() => setNavParams(null)}
          />
        );
      case 'curve':
        return (
           <ParentCurve
              childrenData={children}
              selectedChildId={selectedChildId}
              onSelectChild={setSelectedChildId}
              onNavigate={handleNavigate}
           />
        );
      case 'test': // Hidden tab, accessible via navigation
        return (
          <DailyTest 
            childrenData={children} 
            testRecords={testRecords}
            onNavigate={handleNavigate}
            initialParams={navParams}
            onClearParams={() => setNavParams(null)}
            onSaveRecord={handleSaveTestRecord}
          />
        );
      case 'chat':
        return <ChatInterface />;
      case 'me':
        return (
          <Me 
            childrenData={children} 
            setChildrenData={setChildren}
            initialParams={navParams}
            onClearParams={() => setNavParams(null)}
          />
        );
      default:
        return null;
    }
  }

  return (
    <div className="bg-gray-100 min-h-screen flex justify-center items-start pt-0 sm:pt-4 sm:pb-4 font-sans antialiased">
      {/* Mobile Device Simulator Container */}
      <div className="w-full max-w-[414px] bg-white h-[100vh] sm:h-[896px] sm:min-h-0 sm:rounded-[3rem] shadow-2xl relative overflow-hidden flex flex-col">
        
        {/* Status Bar Area (Simulated) */}
        <div className="h-12 w-full bg-white z-20 flex items-end justify-between px-6 pb-2 text-sm font-semibold text-black shrink-0">
          <span>9:41</span>
          <div className="flex gap-1">
             <div className="w-4 h-2.5 bg-black rounded-[1px]"></div>
             <div className="w-0.5 h-2.5 bg-black rounded-[1px]"></div>
          </div>
        </div>

        {/* Global Header removed for Home/Chat/Me/Curve specific headers */}
        {activeTab !== 'home' && activeTab !== 'chat' && activeTab !== 'me' && activeTab !== 'curve' && activeTab !== 'test' && (
          <header className="px-6 py-4 flex justify-between items-center sticky top-0 bg-white/95 backdrop-blur-sm z-10">
            <div className="w-6" /> {/* Spacer for centering */}
            <h1 className="text-sm font-bold tracking-wide text-gray-800 uppercase">
              {activeTab === 'test' ? '每日自测' : ''}
            </h1>
            <div className="w-6" /> {/* Spacer for centering */}
          </header>
        )}

        {/* Main Content */}
        <main className={`flex-1 flex flex-col w-full relative ${['home', 'chat', 'me', 'curve'].includes(activeTab) ? 'overflow-hidden' : 'overflow-y-auto pb-24'}`}>
           {renderContent()}
        </main>
        
        {/* TabBar */}
        {['home', 'curve', 'chat', 'me'].includes(activeTab) && (
             <TabBar activeTab={activeTab} onChange={(tab) => handleNavigate(tab)} />
        )}
        
        {/* Home Indicator */}
        <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 w-32 h-1 bg-gray-300 rounded-full z-40 pointer-events-none" />
      </div>
    </div>
  );
};

export default App;
