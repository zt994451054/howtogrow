
import React, { useState, useEffect } from 'react';
import { MOCK_QUESTIONS, SPARE_QUESTIONS } from '../constants';
import { Child, Question, TestRecord } from '../types';
import { SmileLogo, PlusIcon, ChevronLeftIcon, XMarkIcon, RefreshIcon } from './Icons';
import { TestResultView } from './TestResultView';

type TestState = 'selection' | 'intro' | 'active' | 'result';
type ResultMode = 'complete' | 'expert_whisper';

interface DailyTestProps {
  childrenData: Child[];
  testRecords: TestRecord[];
  onNavigate: (tab: string, params?: Record<string, any>) => void;
  initialParams?: Record<string, any> | null;
  onClearParams?: () => void;
  onSaveRecord?: (record: TestRecord) => void;
}

export const DailyTest: React.FC<DailyTestProps> = ({ 
  childrenData, 
  testRecords, 
  onNavigate, 
  initialParams, 
  onClearParams,
  onSaveRecord 
}) => {
  const [selectedChildId, setSelectedChildId] = useState<string | null>(null);
  const [testState, setTestState] = useState<TestState>('selection');
  const [resultMode, setResultMode] = useState<ResultMode>('complete');
  
  // Test Logic State
  const [activeQuestions, setActiveQuestions] = useState<Question[]>(MOCK_QUESTIONS);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  
  // Track context for the test
  const [testDate, setTestDate] = useState<string>(new Date().toISOString().split('T')[0]);
  
  // State to remember where to return to
  const [returnTarget, setReturnTarget] = useState<string | null>(null);

  const selectedChild = childrenData.find(c => c.id === selectedChildId);
  const currentQuestion = activeQuestions[currentQuestionIdx];
  const totalQuestions = activeQuestions.length;

  // --- EFFECT: Handle Deep Links ---
  useEffect(() => {
    if (initialParams) {
        const { action, childId, date, returnTarget: target } = initialParams;
        
        if (date) {
            setTestDate(date);
        } else {
            setTestDate(new Date().toISOString().split('T')[0]);
        }

        if (target) {
            setReturnTarget(target);
        }
        
        if (childId) {
            const childExists = childrenData.some(c => c.id === childId);
            if (childExists) {
                const child = childrenData.find(c => c.id === childId);
                setSelectedChildId(childId);
                
                // Logic for checking status
                if (action === 'check_test_status') {
                    // Try to find if record exists for this child & date
                    const record = testRecords.find(r => r.childName === child?.name && r.date === date);
                    if (record) {
                        setActiveQuestions(MOCK_QUESTIONS);
                        setAnswers(record.answers);
                        setResultMode('complete');
                        setTestState('result');
                    } else {
                        // Start Intro flow if not done
                        setTestState('intro');
                    }
                } else if (action === 'view_result') {
                    // Force result view (Expert Whisper)
                    const record = testRecords.find(r => r.childName === child?.name && r.date === date);
                    
                    // Always use MOCK_QUESTIONS for result view to ensure consistent rendering
                    setActiveQuestions(MOCK_QUESTIONS);

                    if (record) {
                        setAnswers(record.answers);
                    } else {
                        setAnswers({}); // No answers
                    }
                    setResultMode('expert_whisper');
                    setTestState('result');
                } else if (action === 'start_test') {
                    setTestState('intro');
                }
            }
        }
        
        // Clear params after consuming
        if (onClearParams) onClearParams();
    }
  }, [initialParams, childrenData, testRecords, onClearParams]);

  const handleChildSelect = (id: string) => {
    setSelectedChildId(id);
    setTestDate(new Date().toISOString().split('T')[0]); // Default to today if manually selecting
    setTestState('intro');
  };

  const startTest = () => {
    setTestState('active');
    setActiveQuestions(MOCK_QUESTIONS);
    setCurrentQuestionIdx(0);
    setAnswers({});
  };

  const handleExitTest = () => {
    // If we came from the observation detail page, go back there
    if (returnTarget === 'observation_detail' && selectedChildId) {
        onNavigate('home', {
            action: 'open_detail_view',
            date: testDate,
            childId: selectedChildId
        });
        setReturnTarget(null); // Reset
    } else {
        // Default behavior: return to child selection screen within Test tab
        setTestState('selection');
        setSelectedChildId(null);
    }
  };

  const handlePrev = () => {
    if (currentQuestionIdx > 0) {
      setCurrentQuestionIdx(prev => prev - 1);
    } else {
      setTestState('intro');
    }
  };

  const handleNext = () => {
    if (currentQuestionIdx < totalQuestions - 1) {
      setCurrentQuestionIdx(prev => prev + 1);
    } else {
      // Submit Test
      if (onSaveRecord && selectedChild) {
          onSaveRecord({
              id: `record-${Date.now()}`,
              date: testDate,
              childName: selectedChild.name,
              answers: answers
          });
      }
      setResultMode('complete');
      setTestState('result');
    }
  };

  const handleSwapQuestion = () => {
    const allPool = [...MOCK_QUESTIONS, ...SPARE_QUESTIONS];
    const availableForSwap = allPool.filter(q => q.id !== currentQuestion.id);
    const randomNewQuestion = availableForSwap[Math.floor(Math.random() * availableForSwap.length)];
    const newActiveQuestions = [...activeQuestions];
    newActiveQuestions[currentQuestionIdx] = randomNewQuestion;
    setActiveQuestions(newActiveQuestions);
  };

  const toggleOption = (optionId: string) => {
    const currentQId = currentQuestion.id;
    const currentType = currentQuestion.type;
    const existingAnswers = answers[currentQId] || [];

    let newAnswers: string[];

    if (currentType === 'single') {
       newAnswers = [optionId];
    } else {
       if (existingAnswers.includes(optionId)) {
         newAnswers = existingAnswers.filter(id => id !== optionId);
       } else {
         newAnswers = [...existingAnswers, optionId];
       }
    }

    setAnswers(prev => ({
      ...prev,
      [currentQId]: newAnswers
    }));
  };

  const handleAddChildClick = () => {
    // Navigate to 'Me' tab root page
    onNavigate('me');
  };

  // --- RENDER: EMPTY STATE ---
  if (childrenData.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full px-8 pb-20 text-center animate-fade-in">
        <div className="w-24 h-24 bg-white rounded-full flex items-center justify-center mb-6 shadow-sm">
           <SmileLogo className="w-12 h-12 opacity-30 grayscale" />
        </div>
        <h3 className="text-lg font-bold text-gray-800 mb-2">还没有添加孩子</h3>
        <p className="text-gray-400 mb-8 text-xs leading-5">
          添加孩子信息后<br/>即可开启专属的成长自测计划
        </p>
        <button 
          className="bg-orange-500 text-white px-10 py-3 rounded-full font-bold text-sm shadow-lg shadow-orange-200 active:scale-95 transition-transform"
          onClick={handleAddChildClick}
        >
          添加孩子
        </button>
      </div>
    );
  }

  // --- RENDER: RESULT SCREEN ---
  if (testState === 'result' && selectedChild) {
      return (
        <TestResultView 
          questions={activeQuestions}
          answers={answers}
          onExit={handleExitTest}
          mode={resultMode}
        />
      );
  }

  // --- RENDER: ACTIVE TEST MODE (FULL SCREEN OVERLAY) ---
  if (testState === 'active' && selectedChild) {
      const currentSelected = answers[currentQuestion.id] || [];
      const progressPercent = ((currentQuestionIdx + 1) / totalQuestions) * 100;

      return (
        <div className="absolute inset-0 z-50 bg-white flex flex-col animate-fade-in">
          
          <div className="w-full h-12 shrink-0 bg-white" />

          {/* Header Area */}
          <div className="px-6 pb-2 shrink-0 bg-white relative z-10">
            {/* Progress Bar */}
            <div className="w-full h-2 bg-gray-100 rounded-full mb-6 overflow-hidden">
               <div 
                 className="h-full bg-orange-500 rounded-full transition-all duration-500 ease-out"
                 style={{ width: `${progressPercent}%` }}
               />
            </div>
            
            {/* Navigation Icons */}
            <div className="flex justify-between items-center">
               <button 
                 onClick={handlePrev}
                 className="p-2 -ml-2 text-gray-800 hover:text-orange-500 transition-colors rounded-full active:bg-gray-100"
               >
                 <ChevronLeftIcon className="w-6 h-6 stroke-[2.5]" />
               </button>
               
               <button 
                 onClick={handleExitTest}
                 className="p-2 -mr-2 text-gray-800 hover:text-red-500 transition-colors rounded-full active:bg-gray-100"
               >
                 <XMarkIcon className="w-6 h-6 stroke-[2.5]" />
               </button>
            </div>
          </div>

          {/* Scrollable Content Area */}
          <div className="flex-1 overflow-y-auto px-6 py-2 no-scrollbar">
             {/* Title */}
             <div className="mt-2 mb-4 text-center">
               <h2 className="text-[1.35rem] font-bold text-orange-500 leading-snug whitespace-pre-line tracking-tight">
                 {currentQuestion.title}
               </h2>
               {currentQuestion.subtitle && (
                 <p className="text-gray-400 text-xs mt-2">{currentQuestion.subtitle}</p>
               )}
             </div>

             {/* Change Scenario Button */}
             <div className="flex justify-center mb-8">
                <button 
                  onClick={handleSwapQuestion}
                  className="flex items-center gap-1.5 text-gray-400 text-[11px] font-bold bg-gray-100 px-4 py-1.5 rounded-full hover:bg-gray-200 active:scale-95 transition-all"
                >
                  <RefreshIcon className="w-3.5 h-3.5 stroke-[2.5]" />
                  <span>换个题目</span>
                </button>
             </div>

             {/* Options List */}
             <div className="flex flex-col gap-3 pb-6">
                {currentQuestion.options.map((option) => {
                  const isSelected = currentSelected.includes(option.id);
                  return (
                    <div 
                      key={option.id}
                      onClick={() => toggleOption(option.id)}
                      className={`
                        w-full p-5 rounded-2xl text-[14px] leading-relaxed font-semibold transition-all duration-200 cursor-pointer border-2 select-none
                        ${isSelected 
                           ? 'bg-[#FFF6E6] border-orange-200 text-gray-900 shadow-sm' 
                           : 'bg-[#FFF6E6]/40 border-transparent text-gray-600 hover:bg-[#FFF6E6]'
                        }
                      `}
                    >
                      {option.text}
                    </div>
                  );
                })}
             </div>
          </div>

          {/* Footer Button Area */}
          <div className="px-6 pb-10 pt-4 bg-white/95 shrink-0 backdrop-blur-sm">
             {currentSelected.length > 0 ? (
               <button 
                 onClick={handleNext}
                 className="w-full bg-[#FFE4CF] text-orange-900 shadow-lg shadow-orange-100 hover:bg-[#FFDCC0] py-4 rounded-full font-bold text-base transition-all active:scale-[0.98] animate-fade-in-up"
               >
                 {currentQuestionIdx === totalQuestions - 1 ? '提交' : '下一题'}
               </button>
             ) : (
                <div className="h-[56px] w-full" />
             )}
          </div>
        </div>
      );
  }

  // --- RENDER: INTRO SCREEN ---
  if (testState === 'intro' && selectedChild) {
      const displayDate = testDate ? testDate.replace(/-/g, '/') : '今日';
      
      return (
          <div className="flex flex-col h-full animate-fade-in bg-white">
             <div className="px-4 py-3 flex items-center gap-2 border-b border-gray-50">
                 <button 
                   onClick={() => {
                     setTestState('selection');
                     setSelectedChildId(null);
                   }}
                   className="p-2 -ml-2 text-gray-500 hover:text-orange-500 transition-colors"
                 >
                    <ChevronLeftIcon className="w-6 h-6" />
                 </button>
                 <div className="flex flex-col">
                    <span className="font-bold text-sm text-gray-800">
                        {selectedChild.name}的每日自测
                    </span>
                    <span className="text-[10px] text-gray-400">
                        {displayDate}
                    </span>
                 </div>
             </div>
             <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
                 <div className="w-32 h-32 bg-gray-50 rounded-full flex items-center justify-center mb-6 animate-pulse">
                     <span className="text-4xl">📝</span>
                 </div>
                 <h3 className="font-bold text-gray-800 mb-2">准备好了吗？</h3>
                 <p className="text-gray-400 text-sm mb-8 leading-relaxed">
                     接下来将开始关于 {selectedChild.name} 的沟通能力测试<br/>
                     <span className="text-xs text-orange-400">记录日期: {displayDate}</span>
                 </p>
                 <button 
                   onClick={startTest}
                   className="w-full bg-orange-500 text-white py-3 rounded-xl font-bold shadow-lg shadow-orange-100 active:scale-95"
                 >
                     开始测试
                 </button>
             </div>
          </div>
      );
  }

  // --- RENDER: CHILD SELECTION ---
  return (
    <div className="flex flex-col h-full animate-fade-in">
      {/* Header - Fixed */}
      <div className="px-6 pt-6 mb-4 shrink-0">
        <h2 className="text-xl font-bold text-gray-800">
            每日自测
        </h2>
        <p className="text-xs text-gray-400 mt-1">选择一个孩子开始今天的成长记录</p>
      </div>
      
      {/* Scrollable Grid Area */}
      <div className="flex-1 overflow-y-auto px-6 pb-24 no-scrollbar">
        <div className="grid grid-cols-2 gap-4">
          {childrenData.map(child => (
             <div 
               key={child.id}
               onClick={() => handleChildSelect(child.id)}
               className="bg-white aspect-[4/5] rounded-2xl p-4 flex flex-col items-center justify-center gap-3 shadow-sm border border-transparent hover:border-orange-200 hover:shadow-md transition-all cursor-pointer relative overflow-hidden group"
             >
                <div className={`w-16 h-16 rounded-full flex items-center justify-center text-3xl mb-1 ${
                    child.gender === 'boy' ? 'bg-blue-50' : 'bg-pink-50'
                }`}>
                   {child.gender === 'boy' ? '👦' : '👧'}
                </div>
                <div className="text-center z-10">
                   <div className="font-bold text-gray-800 text-sm group-hover:text-orange-500 transition-colors">{child.name}</div>
                   <div className="text-[10px] text-gray-400 mt-1 bg-gray-50 px-2 py-0.5 rounded-full">{child.age} 岁</div>
                </div>
             </div>
          ))}

          {/* Add Button Card */}
           <div 
               className="bg-transparent aspect-[4/5] rounded-2xl p-4 flex flex-col items-center justify-center gap-2 border-2 border-dashed border-gray-300 hover:border-orange-400 hover:bg-orange-50 transition-all cursor-pointer group"
               onClick={handleAddChildClick}
             >
                <div className="w-10 h-10 rounded-full bg-gray-100 group-hover:bg-white flex items-center justify-center transition-colors">
                   <PlusIcon className="w-5 h-5 text-gray-400 group-hover:text-orange-500" />
                </div>
                <span className="text-xs font-bold text-gray-400 group-hover:text-orange-500">添加孩子</span>
             </div>
        </div>
      </div>
    </div>
  );
}
