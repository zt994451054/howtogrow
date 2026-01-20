
import React from 'react';
import { Question } from '../types';
import { FlagIcon, CheckCircleIcon, ExclamationTriangleIcon, XMarkIcon, ChevronLeftIcon } from './Icons';

interface TestResultViewProps {
  questions: Question[];
  answers: Record<string, string[]>;
  onExit: () => void;
  title?: string;
  mode?: 'complete' | 'history' | 'expert_whisper';
}

export const TestResultView: React.FC<TestResultViewProps> = ({ questions, answers, onExit, title = "完成测试", mode = 'complete' }) => {
  return (
    <div className="absolute inset-0 z-50 bg-[#FFF5F2] flex flex-col animate-fade-in">
       {/* Header with Exit */}
       <div className="w-full h-12 shrink-0" /> {/* Status bar spacer */}
       <div className="px-4 pb-2 flex justify-between items-center">
           {mode === 'expert_whisper' ? (
                <button onClick={onExit} className="p-2 -ml-2 text-gray-700 hover:text-orange-500 transition-colors">
                    <ChevronLeftIcon className="w-6 h-6 stroke-[2]" />
                </button>
           ) : (
                <div className="w-10"></div>
           )}
           
           {(mode === 'history' || mode === 'expert_whisper') && <h3 className="text-gray-800 font-bold">测试详情</h3>}
           
           {mode !== 'expert_whisper' ? (
               <button onClick={onExit} className="p-2 text-gray-400 hover:text-gray-600">
                  <XMarkIcon className="w-6 h-6" />
               </button>
           ) : (
               <div className="w-10" />
           )}
       </div>

       {/* Scrollable Result Content */}
       <div className={`flex-1 overflow-y-auto px-6 no-scrollbar ${mode === 'expert_whisper' ? 'pb-10' : 'pb-24'}`}>
          
          {/* Top Graphic */}
          <div className="flex justify-center mb-8">
              <FlagIcon className="w-40 h-40 shadow-lg rounded-full" />
          </div>

          {/* Questions Review List */}
          <div className="flex flex-col gap-10">
             {questions.map((q, idx) => {
                 // Find user's selected option(s)
                 const userAnsIds = answers[q.id] || [];
                 if (userAnsIds.length === 0) return null;

                 const selectedOption = q.options.find(opt => opt.id === userAnsIds[0]);
                 
                 if (!selectedOption) return null;

                 return (
                     <div key={q.id} className="flex flex-col gap-3 animate-fade-in-up" style={{animationDelay: `${idx * 100}ms`}}>
                         {/* Question Title */}
                         <div className="flex items-start gap-3">
                             <div className="w-6 h-6 shrink-0 rounded-full bg-orange-500 text-white text-sm font-bold flex items-center justify-center mt-0.5">
                                {idx + 1}
                             </div>
                             <h3 className="text-base font-bold text-gray-800 leading-tight">
                                 {q.title.replace('\n', '')}
                             </h3>
                         </div>

                         {/* Your Action */}
                         <div className="ml-9 border-l-2 border-orange-400 pl-4 py-1">
                             <div className="text-orange-500 text-sm font-bold mb-1">你的做法</div>
                             <div className="text-gray-500 text-[13px] italic">
                                 {selectedOption.text}
                             </div>
                         </div>

                         {/* Advice Block */}
                         <div className="ml-9 mt-1">
                             <div className="flex items-center gap-2 mb-2">
                                 {selectedOption.sentiment === 'positive' ? (
                                     <CheckCircleIcon className="w-5 h-5" />
                                 ) : (
                                     <ExclamationTriangleIcon className="w-5 h-5" />
                                 )}
                                 <span className="font-bold text-gray-800 text-sm">
                                     {selectedOption.adviceTitle || "建议"}
                                 </span>
                             </div>
                             <p className="text-gray-600 text-[13px] leading-relaxed text-justify">
                                 {selectedOption.advice || "暂无具体建议。"}
                             </p>
                         </div>
                     </div>
                 );
             })}
          </div>
       </div>

       {/* Bottom Done Button - Hide if expert_whisper */}
       {mode !== 'expert_whisper' && (
           <div className="px-6 pb-10 pt-4 bg-[#FFF5F2]/95 backdrop-blur-sm shrink-0">
               <button 
                 onClick={onExit}
                 className="w-full bg-orange-500 text-white shadow-lg shadow-orange-200 hover:bg-orange-600 py-4 rounded-full font-bold text-base transition-all active:scale-[0.98]"
               >
                 {title}
               </button>
           </div>
       )}
    </div>
  );
};
