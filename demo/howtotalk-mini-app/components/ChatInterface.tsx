
import React, { useState, useRef, useEffect } from 'react';
import { Bars3Icon, PencilSquareIcon, PaperAirplaneIcon, RobotIcon, UserIcon, SmileLogo, SparklesIcon } from './Icons';

interface Message {
  id: string;
  role: 'user' | 'ai';
  text: string;
  timestamp: number;
}

interface ChatSession {
  id: string;
  title: string;
  timestamp: number;
}

const INITIAL_MESSAGES: Message[] = [
  { id: '1', role: 'ai', text: '你好！我是你的育儿助手 Howtotalk。今天遇到了什么挑战吗？', timestamp: Date.now() }
];

const SUGGESTED_QUESTIONS = [
    "面对孩子磨蹭拖拉，忍不住发火",
    "孩子情绪崩溃大哭，怎么哄都没用",
    "二胎之间总是争抢打架怎么办"
];

const MOCK_HISTORY: ChatSession[] = [
  { id: '1', title: '关于孩子不爱吃饭的讨论', timestamp: Date.now() - 86400000 },
  { id: '2', title: '如何处理兄弟姐妹争吵', timestamp: Date.now() - 172800000 },
];

export const ChatInterface: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState('');
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  const sendMessage = (text: string) => {
    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      text: text,
      timestamp: Date.now(),
    };

    setMessages(prev => [...prev, userMsg]);
    setIsTyping(true);

    // Mock AI Response
    setTimeout(() => {
      const aiMsg: Message = {
        id: (Date.now() + 1).toString(),
        role: 'ai',
        text: '我明白这可能让你感到困扰。作为父母，我们的情绪也会受到孩子行为的影响。你能具体说说当时发生了什么吗？',
        timestamp: Date.now(),
      };
      setMessages(prev => [...prev, aiMsg]);
      setIsTyping(false);
    }, 1500);
  };

  const handleSend = () => {
    if (!input.trim()) return;
    sendMessage(input);
    setInput('');
  };

  const startNewChat = () => {
    setMessages([
        { id: Date.now().toString(), role: 'ai', text: '开启了新会话！请问有什么可以帮您？', timestamp: Date.now() }
    ]);
    setIsDrawerOpen(false);
  };

  return (
    <div className="flex flex-col flex-1 w-full h-full bg-white relative overflow-hidden">
      
      {/* HEADER for Chat - Replaces global header */}
      <div className="px-6 py-4 flex justify-between items-center bg-white border-b border-gray-100 z-10 shrink-0 h-[60px]">
        <button 
          onClick={() => setIsDrawerOpen(true)}
          className="p-2 -ml-2 text-gray-500 hover:text-orange-500 transition-colors"
        >
          <Bars3Icon className="w-6 h-6" />
        </button>
        
        <h1 className="text-sm font-bold tracking-wide text-gray-800 uppercase">
          马上沟通
        </h1>
        
        <button 
          onClick={startNewChat}
          className="p-2 -mr-2 text-gray-500 hover:text-orange-500 transition-colors"
        >
          <PencilSquareIcon className="w-6 h-6" />
        </button>
      </div>

      {/* DRAWER SIDEBAR */}
      {/* Backdrop */}
      {isDrawerOpen && (
        <div 
          className="absolute inset-0 bg-black/20 z-40 backdrop-blur-sm animate-fade-in"
          onClick={() => setIsDrawerOpen(false)}
        />
      )}
      
      {/* Drawer Content */}
      <div className={`absolute top-0 bottom-0 left-0 w-[75%] bg-white z-50 shadow-2xl transform transition-transform duration-300 ease-out flex flex-col ${isDrawerOpen ? 'translate-x-0' : '-translate-x-full'}`}>
         <div className="p-6 bg-orange-500 text-white">
             <div className="flex items-center gap-3 mb-2">
                <div className="bg-white p-2 rounded-full">
                    <SmileLogo className="w-8 h-8" />
                </div>
                <span className="font-bold text-lg">历史对话</span>
             </div>
             <p className="text-orange-100 text-xs">查看之前的咨询记录</p>
         </div>
         
         <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-2">
            <button 
              onClick={startNewChat}
              className="w-full text-left px-4 py-3 rounded-xl bg-orange-50 text-orange-600 font-bold text-sm flex items-center gap-2 mb-4 hover:bg-orange-100 transition-colors"
            >
               <PencilSquareIcon className="w-5 h-5" />
               开启新会话
            </button>

            <div className="text-xs font-bold text-gray-400 px-2 mb-2 uppercase tracking-wider">最近</div>
            
            {MOCK_HISTORY.map(session => (
                <button 
                  key={session.id}
                  onClick={() => {
                      // Mock loading history
                      setIsDrawerOpen(false);
                      setMessages(INITIAL_MESSAGES); // In real app, load actual history
                  }}
                  className="w-full text-left px-4 py-3 rounded-xl hover:bg-gray-50 transition-colors text-gray-700 text-sm border border-transparent hover:border-gray-200 truncate"
                >
                   {session.title}
                </button>
            ))}
         </div>
         
         <div className="p-4 border-t border-gray-100 text-center text-xs text-gray-400">
             Howtotalk AI Assistant v1.0
         </div>
      </div>

      {/* CHAT MESSAGES */}
      <div className="flex-1 overflow-y-auto p-4 bg-gray-50/50 no-scrollbar pb-4">
         <div className="flex flex-col gap-6">
             {messages.map((msg) => {
                 const isUser = msg.role === 'user';
                 return (
                     <div key={msg.id} className={`flex w-full ${isUser ? 'justify-end' : 'justify-start'}`}>
                         <div className={`flex max-w-[85%] gap-2 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
                             {/* Avatar */}
                             <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${isUser ? 'bg-orange-100 text-orange-500' : 'bg-white border border-gray-200 text-gray-400'}`}>
                                 {isUser ? <UserIcon className="w-5 h-5" /> : <RobotIcon className="w-5 h-5" />}
                             </div>
                             
                             {/* Bubble */}
                             <div className={`px-4 py-3 shadow-sm text-[14px] leading-6 ${
                                 isUser 
                                 ? 'bg-orange-500 text-white rounded-2xl rounded-tr-sm' 
                                 : 'bg-white text-gray-700 border border-gray-100 rounded-2xl rounded-tl-sm'
                             }`}>
                                 {msg.text}
                             </div>
                         </div>
                     </div>
                 );
             })}

             {/* Suggested Questions (Only show when there is just 1 message - the greeting) */}
             {messages.length === 1 && (
                 <div className="pl-[40px] pr-4 mt-1 animate-fade-in">
                     <div className="flex flex-col gap-2.5 items-start">
                        {SUGGESTED_QUESTIONS.map((q, idx) => (
                            <button 
                                key={idx}
                                onClick={() => sendMessage(q)}
                                className="text-left bg-white px-4 py-2.5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md hover:border-orange-200 hover:bg-orange-50/30 hover:text-orange-700 transition-all duration-200 active:scale-[0.98] text-[13px] text-gray-600 font-medium leading-relaxed"
                            >
                                {q}
                            </button>
                        ))}
                     </div>
                 </div>
             )}
             
             {isTyping && (
                 <div className="flex w-full justify-start animate-pulse">
                     <div className="flex max-w-[85%] gap-2">
                        <div className="w-8 h-8 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-400">
                             <RobotIcon className="w-5 h-5" />
                        </div>
                        <div className="bg-gray-200 h-8 w-16 rounded-full flex items-center justify-center gap-1">
                            <div className="w-1.5 h-1.5 bg-gray-400 rounded-full"></div>
                            <div className="w-1.5 h-1.5 bg-gray-400 rounded-full"></div>
                            <div className="w-1.5 h-1.5 bg-gray-400 rounded-full"></div>
                        </div>
                     </div>
                 </div>
             )}
             <div ref={messagesEndRef} />
         </div>
      </div>

      {/* INPUT AREA */}
      <div className="p-4 bg-white border-t border-gray-100 shrink-0 pb-24">
          <div className="flex items-center gap-2 bg-gray-100 p-1.5 rounded-full pr-2 focus-within:ring-2 focus-within:ring-orange-100 transition-all">
              <input 
                type="text" 
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                placeholder="输入你的问题..."
                className="flex-1 bg-transparent border-none outline-none px-4 py-2 text-sm text-gray-800 placeholder-gray-400"
              />
              <button 
                onClick={handleSend}
                disabled={!input.trim()}
                className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                    input.trim() 
                    ? 'bg-orange-500 text-white shadow-md active:scale-90' 
                    : 'bg-gray-200 text-gray-400'
                }`}
              >
                  <PaperAirplaneIcon className="w-5 h-5 ml-0.5" />
              </button>
          </div>
      </div>

    </div>
  );
};
