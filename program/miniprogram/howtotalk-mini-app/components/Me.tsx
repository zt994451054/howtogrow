
import React, { useState, useRef, useEffect } from 'react';
import { MOCK_RECORDS, SUBSCRIPTION_PLANS, USER_AGREEMENT_HTML, MOCK_QUESTIONS } from '../constants';
import { Child, SubscriptionPlan, TestRecord } from '../types';
import { 
    UsersIcon, ChevronRightIcon, CalendarDaysIcon, CurrencyYenIcon, 
    DocumentTextIcon, ChevronLeftIcon, PlusIcon, ClockIcon, CameraIcon 
} from './Icons';
import { TestResultView } from './TestResultView';

type MeView = 'main' | 'children' | 'add-child' | 'history' | 'subscription' | 'agreement' | 'history-detail' | 'profile-edit';

const INITIAL_USER_INFO = {
    nickname: '育儿新手',
    avatarUrl: 'https://i.pravatar.cc/150?img=5',
    dob: ''
};

const INITIAL_EXPIRE_DATE = '2023-10-01';

const RELATIONS = ['妈妈', '爸爸', '爷爷', '奶奶', '外公', '外婆'];

interface MeProps {
    childrenData: Child[];
    setChildrenData: React.Dispatch<React.SetStateAction<Child[]>>;
    initialParams?: {view?: string, action?: string} | null;
    onClearParams?: () => void;
}

export const Me: React.FC<MeProps> = ({ childrenData, setChildrenData, initialParams, onClearParams }) => {
    const [view, setView] = useState<MeView>('main');
    
    // User Profile State
    const [userInfo, setUserInfo] = useState(INITIAL_USER_INFO);
    
    // Edit Form State (Temporary)
    const [editForm, setEditForm] = useState(INITIAL_USER_INFO);
    
    const [subscription, setSubscription] = useState<{isSub: boolean, expireDate: string}>({
        isSub: false, // Default state for demo
        expireDate: INITIAL_EXPIRE_DATE
    });
    
    // For History Detail
    const [selectedRecord, setSelectedRecord] = useState<TestRecord | null>(null);

    // New Child Form State
    const [newChildName, setNewChildName] = useState('');
    const [newChildGender, setNewChildGender] = useState<'boy'|'girl'>('boy');
    const [newChildDob, setNewChildDob] = useState('');
    const [newChildRelation, setNewChildRelation] = useState('妈妈');
    
    // Ref for date input to trigger picker
    const dateInputRef = useRef<HTMLInputElement>(null);
    const profileDateRef = useRef<HTMLInputElement>(null);
    const avatarInputRef = useRef<HTMLInputElement>(null);

    // Subscription Plan State
    const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);

    const today = new Date().toISOString().split('T')[0];
    const isExpired = subscription.expireDate < today;

    // --- EFFECT: Handle Deep Links ---
    useEffect(() => {
        if (initialParams) {
            if (initialParams.view) {
                setView(initialParams.view as MeView);
            }
            if (initialParams.action === 'add') {
                resetAddChildForm();
                setView('add-child');
            }
            // Clear params after consuming
            if (onClearParams) onClearParams();
        }
    }, [initialParams, onClearParams]);

    // --- LOGIC ---
    const resetAddChildForm = () => {
        setNewChildName('');
        setNewChildGender('boy');
        setNewChildDob('');
        setNewChildRelation('妈妈');
    };

    const handleAddChild = () => {
        if(!newChildName.trim() || !newChildDob) {
            alert("请填写完整信息");
            return;
        }

        const selectedDate = new Date(newChildDob);
        const currentDate = new Date();
        if (selectedDate > currentDate) {
            alert("出生日期不能是未来时间");
            return;
        }
        if (selectedDate.getFullYear() < 1900) {
            alert("出生日期年份无效");
            return;
        }

        const age = currentDate.getFullYear() - selectedDate.getFullYear();
        const newChild: Child = {
            id: Date.now().toString(),
            name: newChildName,
            gender: newChildGender,
            dob: newChildDob,
            age: age >= 0 ? age : 0,
            relation: newChildRelation
        };
        setChildrenData([...childrenData, newChild]);
        resetAddChildForm();
        setView('children');
    };

    const handleBuyPlan = (plan: SubscriptionPlan) => {
        if(window.confirm(`确认使用微信支付 ${plan.price} 元购买 ${plan.name}?`)) {
            // Mock purchase success
            const date = new Date();
            date.setDate(date.getDate() + plan.days);
            setSubscription({
                isSub: true,
                expireDate: date.toISOString().split('T')[0]
            });
            alert("支付成功！订阅已更新。");
            setView('main');
            setSelectedPlanId(null); 
        }
    };

    const viewRecordDetail = (record: TestRecord) => {
        setSelectedRecord(record);
        setView('history-detail');
    };

    // --- Profile Edit Logic ---
    const handleOpenProfileEdit = () => {
        setEditForm({ ...userInfo });
        setView('profile-edit');
    };

    const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setEditForm(prev => ({ ...prev, avatarUrl: reader.result as string }));
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSaveProfile = () => {
        if (!editForm.nickname.trim()) {
            alert("昵称不能为空");
            return;
        }
        setUserInfo(editForm);
        setView('main');
    };

    // --- RENDER CONTENT ---
    const renderContent = () => {
        switch (view) {
            case 'children':
                return (
                    <div className="flex flex-col h-full bg-gray-50">
                        <div className="bg-white px-4 py-3 border-b border-gray-100 flex items-center gap-2">
                            <button onClick={() => setView('main')} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <span className="font-bold text-gray-800">孩子管理</span>
                        </div>
                        <div className="p-4 flex-1 overflow-y-auto pb-24">
                            {childrenData.map(child => (
                                <div key={child.id} className="bg-white p-4 rounded-xl mb-3 flex items-center justify-between shadow-sm">
                                    <div className="flex items-center gap-3">
                                        <div className={`w-12 h-12 rounded-full flex items-center justify-center text-2xl ${child.gender === 'boy' ? 'bg-blue-50' : 'bg-pink-50'}`}>
                                            {child.gender === 'boy' ? '👦' : '👧'}
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <div className="font-bold text-gray-800">{child.name}</div>
                                                {child.relation && (
                                                    <span className="text-[10px] bg-orange-50 text-orange-500 px-1.5 py-0.5 rounded">
                                                        我是{child.relation}
                                                    </span>
                                                )}
                                            </div>
                                            <div className="text-xs text-gray-400 mt-1">{child.age}岁 · {child.dob || '未知生日'}</div>
                                        </div>
                                    </div>
                                    <ChevronRightIcon className="w-4 h-4 text-gray-300" />
                                </div>
                            ))}

                            <button 
                                onClick={() => {
                                    resetAddChildForm();
                                    setView('add-child');
                                }} 
                                className="w-full mt-4 py-4 border-2 border-dashed border-gray-300 rounded-xl text-gray-400 text-sm font-bold flex items-center justify-center gap-1 hover:bg-white hover:border-orange-300 hover:text-orange-500 transition-all active:scale-95"
                            >
                                <PlusIcon className="w-5 h-5" /> 添加新孩子
                            </button>
                        </div>
                    </div>
                );

            case 'add-child':
                return (
                    <div className="flex flex-col h-full bg-white animate-fade-in z-50">
                        {/* Header */}
                        <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2 bg-white">
                            <button onClick={() => setView('children')} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <span className="font-bold text-gray-800">添加孩子</span>
                        </div>

                        <div className="flex-1 overflow-y-auto p-6 pb-24">
                            {/* Gender Selection */}
                            <div className="flex justify-center gap-8 mb-8 mt-2">
                                <button 
                                    onClick={() => setNewChildGender('boy')}
                                    className={`flex flex-col items-center gap-2 transition-all ${newChildGender === 'boy' ? 'scale-110' : 'opacity-60 scale-95'}`}
                                >
                                    <div className={`w-20 h-20 rounded-full flex items-center justify-center text-5xl shadow-sm border-4 ${newChildGender === 'boy' ? 'bg-blue-50 border-blue-200' : 'bg-gray-50 border-transparent'}`}>
                                        👦
                                    </div>
                                    <span className={`text-sm font-bold ${newChildGender === 'boy' ? 'text-blue-500' : 'text-gray-400'}`}>男孩</span>
                                </button>
                                <button 
                                    onClick={() => setNewChildGender('girl')}
                                    className={`flex flex-col items-center gap-2 transition-all ${newChildGender === 'girl' ? 'scale-110' : 'opacity-60 scale-95'}`}
                                >
                                    <div className={`w-20 h-20 rounded-full flex items-center justify-center text-5xl shadow-sm border-4 ${newChildGender === 'girl' ? 'bg-pink-50 border-pink-200' : 'bg-gray-50 border-transparent'}`}>
                                        👧
                                    </div>
                                    <span className={`text-sm font-bold ${newChildGender === 'girl' ? 'text-pink-500' : 'text-gray-400'}`}>女孩</span>
                                </button>
                            </div>

                            {/* Form Fields */}
                            <div className="space-y-6">
                                {/* Name */}
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">孩子昵称</label>
                                    <input 
                                        className="w-full bg-gray-50 rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-orange-100 focus:bg-white transition-all text-gray-800"
                                        placeholder="请输入孩子的小名"
                                        value={newChildName}
                                        onChange={e => setNewChildName(e.target.value)}
                                        autoFocus
                                    />
                                </div>

                                {/* DOB */}
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">出生日期</label>
                                    <div 
                                        className="relative cursor-pointer"
                                        onClick={() => dateInputRef.current?.showPicker()}
                                    >
                                        <input 
                                            ref={dateInputRef}
                                            type="date"
                                            max={today}
                                            min="1900-01-01"
                                            className="w-full bg-gray-50 rounded-xl px-4 py-3 text-sm outline-none text-gray-800 focus:bg-white focus:ring-2 focus:ring-orange-100 transition-all cursor-pointer"
                                            value={newChildDob}
                                            onChange={e => setNewChildDob(e.target.value)}
                                        />
                                        <CalendarDaysIcon className="absolute right-4 top-3 w-5 h-5 text-gray-400 pointer-events-none" />
                                    </div>
                                </div>

                                {/* Relation Selector */}
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-3">我是孩子的...</label>
                                    <div className="grid grid-cols-3 gap-3">
                                        {RELATIONS.map(rel => (
                                            <button
                                                key={rel}
                                                onClick={() => setNewChildRelation(rel)}
                                                className={`py-2.5 rounded-lg text-sm font-bold transition-all border ${
                                                    newChildRelation === rel
                                                    ? 'bg-orange-500 text-white border-orange-500 shadow-md shadow-orange-100'
                                                    : 'bg-white text-gray-500 border-gray-200 hover:border-orange-300 hover:text-orange-500'
                                                }`}
                                            >
                                                {rel}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Footer Actions */}
                        <div className="p-6 border-t border-gray-100 bg-white">
                            <button 
                                onClick={handleAddChild}
                                className="w-full bg-orange-500 text-white py-3.5 rounded-full font-bold text-base shadow-lg shadow-orange-200 active:scale-[0.98] transition-transform"
                            >
                                保存信息
                            </button>
                        </div>
                    </div>
                );

            case 'profile-edit':
                return (
                    <div className="flex flex-col h-full bg-white animate-fade-in z-50">
                        {/* Header */}
                        <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2 bg-white sticky top-0 z-10">
                            <button onClick={() => setView('main')} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <span className="font-bold text-gray-800">个人信息</span>
                        </div>

                        <div className="flex-1 overflow-y-auto p-6 pb-24">
                            {/* Avatar Section */}
                            <div className="flex flex-col items-center mb-8 mt-4">
                                <div className="relative group cursor-pointer" onClick={() => avatarInputRef.current?.click()}>
                                    <img 
                                        src={editForm.avatarUrl} 
                                        alt="Avatar" 
                                        className="w-24 h-24 rounded-full object-cover border-4 border-gray-50 shadow-sm"
                                    />
                                    <div className="absolute inset-0 bg-black/20 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                                        <CameraIcon className="w-8 h-8 text-white" />
                                    </div>
                                    <div className="absolute bottom-0 right-0 bg-orange-500 rounded-full p-1.5 border-2 border-white shadow-sm">
                                        <CameraIcon className="w-3.5 h-3.5 text-white" />
                                    </div>
                                    <input 
                                        ref={avatarInputRef}
                                        type="file" 
                                        accept="image/*" 
                                        className="hidden" 
                                        onChange={handleAvatarChange}
                                    />
                                </div>
                                <p className="text-xs text-gray-400 mt-3">点击头像更换</p>
                            </div>

                            {/* Form Fields */}
                            <div className="space-y-6">
                                {/* Nickname */}
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">昵称</label>
                                    <input 
                                        className="w-full bg-gray-50 rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-orange-100 focus:bg-white transition-all text-gray-800"
                                        placeholder="请输入昵称"
                                        value={editForm.nickname}
                                        onChange={e => setEditForm({...editForm, nickname: e.target.value})}
                                    />
                                </div>

                                {/* DOB (Optional) */}
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">出生日期 (选填)</label>
                                    <div 
                                        className="relative cursor-pointer"
                                        onClick={() => profileDateRef.current?.showPicker()}
                                    >
                                        <input 
                                            ref={profileDateRef}
                                            type="date"
                                            max={today}
                                            min="1900-01-01"
                                            className="w-full bg-gray-50 rounded-xl px-4 py-3 text-sm outline-none text-gray-800 focus:bg-white focus:ring-2 focus:ring-orange-100 transition-all cursor-pointer"
                                            value={editForm.dob}
                                            onChange={e => setEditForm({...editForm, dob: e.target.value})}
                                            placeholder="请选择"
                                        />
                                        <CalendarDaysIcon className="absolute right-4 top-3 w-5 h-5 text-gray-400 pointer-events-none" />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Footer */}
                        <div className="p-6 border-t border-gray-100 bg-white">
                            <button 
                                onClick={handleSaveProfile}
                                className="w-full bg-orange-500 text-white py-3.5 rounded-full font-bold text-base shadow-lg shadow-orange-200 active:scale-[0.98] transition-transform"
                            >
                                保存
                            </button>
                        </div>
                    </div>
                );

            case 'history':
                return (
                    <div className="flex flex-col h-full bg-gray-50">
                        <div className="bg-white px-4 py-3 border-b border-gray-100 flex items-center gap-2">
                            <button onClick={() => setView('main')} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <span className="font-bold text-gray-800">自测记录</span>
                        </div>
                        <div className="p-4 flex-1 overflow-y-auto pb-24">
                            {MOCK_RECORDS.map(record => (
                                <div key={record.id} className="bg-white p-4 rounded-xl mb-3 shadow-sm">
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="flex items-center gap-2">
                                            <CalendarDaysIcon className="w-4 h-4 text-orange-400" />
                                            <span className="text-sm font-bold text-gray-800">{record.date}</span>
                                        </div>
                                        <span className="bg-orange-50 text-orange-600 px-2 py-0.5 rounded text-[10px] font-bold">{record.childName}</span>
                                    </div>
                                    <p className="text-xs text-gray-400 mb-3">完成了沟通能力自测</p>
                                    <button 
                                        onClick={() => viewRecordDetail(record)}
                                        className="w-full py-2 bg-gray-50 text-gray-600 text-xs font-bold rounded-lg hover:bg-gray-100 transition-colors"
                                    >
                                        查看详情
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                );

            case 'subscription':
                return (
                    <div className="flex flex-col h-full bg-white pb-24">
                        <div className="bg-orange-500 px-4 pt-4 pb-8 text-white relative">
                            <button onClick={() => setView('main')} className="absolute left-4 top-4 text-white/80 p-1 hover:text-white transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <div className="mt-8 flex flex-col items-center">
                                <div className="font-bold text-lg mb-1">会员订阅</div>
                                {subscription.isSub && !isExpired ? (
                                    <div className="text-sm opacity-90 bg-white/20 px-3 py-1 rounded-full">
                                        有效期至 {subscription.expireDate}
                                    </div>
                                ) : (
                                    <div className="text-sm opacity-90 bg-red-500/30 px-3 py-1 rounded-full flex items-center gap-1">
                                        <ClockIcon className="w-4 h-4" /> 未订阅或已过期
                                    </div>
                                )}
                            </div>
                        </div>
                        
                        <div className="flex-1 p-6 -mt-4 bg-white rounded-t-3xl shadow-md overflow-y-auto">
                            <h3 className="font-bold text-gray-800 mb-4">选择套餐</h3>
                            <div className="flex flex-col gap-3 mb-8">
                                {SUBSCRIPTION_PLANS.map(plan => (
                                    <div 
                                        key={plan.id}
                                        onClick={() => setSelectedPlanId(plan.id)}
                                        className={`p-4 rounded-xl border-2 flex justify-between items-center cursor-pointer transition-all ${
                                            selectedPlanId === plan.id 
                                            ? 'border-orange-500 bg-orange-50' 
                                            : 'border-gray-100 hover:border-gray-200'
                                        }`}
                                    >
                                        <div>
                                            <div className="font-bold text-gray-800">{plan.name}</div>
                                            <div className="text-xs text-gray-400">{plan.days}天有效期</div>
                                        </div>
                                        <div className="text-orange-600 font-bold text-lg">
                                            ¥{plan.price}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                        
                        <div className="p-6 border-t border-gray-100 bg-white">
                            <button 
                                disabled={!selectedPlanId}
                                onClick={() => {
                                    const plan = SUBSCRIPTION_PLANS.find(p => p.id === selectedPlanId);
                                    if(plan) handleBuyPlan(plan);
                                }}
                                className={`w-full py-3.5 rounded-full font-bold text-white shadow-lg transition-transform active:scale-[0.98] ${
                                    selectedPlanId 
                                    ? 'bg-green-600 shadow-green-200' 
                                    : 'bg-gray-300 cursor-not-allowed'
                                }`}
                            >
                                微信支付购买套餐
                            </button>
                        </div>
                    </div>
                );

            case 'agreement':
                return (
                    <div className="flex flex-col h-full bg-white">
                        <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2">
                            <button onClick={() => setView('main')} className="p-1 text-gray-500 hover:text-orange-500 transition-colors">
                                <ChevronLeftIcon className="w-6 h-6" />
                            </button>
                            <span className="font-bold text-gray-800">用户协议</span>
                        </div>
                        <div className="p-6 overflow-y-auto prose prose-sm prose-orange pb-24">
                            <div dangerouslySetInnerHTML={{ __html: USER_AGREEMENT_HTML }} />
                        </div>
                    </div>
                );

            case 'history-detail':
                if (!selectedRecord) return null;
                return (
                    <TestResultView 
                        questions={MOCK_QUESTIONS}
                        answers={selectedRecord.answers}
                        onExit={() => setView('history')}
                        title="返回记录"
                        mode="history"
                    />
                );

            case 'main':
            default:
                return (
                    <div className="flex flex-col h-full bg-gray-50 pb-24 overflow-y-auto">
                        {/* Header */}
                        <div className="bg-white p-6 pt-8 pb-8 rounded-b-[2rem] shadow-sm mb-4">
                            <div 
                                className="flex items-center gap-4 cursor-pointer hover:opacity-80 transition-opacity"
                                onClick={handleOpenProfileEdit}
                            >
                                <div className="relative">
                                    <img src={userInfo.avatarUrl} alt="Avatar" className="w-16 h-16 rounded-full border-2 border-white shadow-md" />
                                    <div className="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 shadow-sm border border-gray-100">
                                         <div className="w-5 h-5 bg-gray-100 rounded-full flex items-center justify-center text-gray-400">
                                            <ChevronRightIcon className="w-3 h-3" />
                                         </div>
                                    </div>
                                </div>
                                <div className="flex-1">
                                    <h2 className="text-lg font-bold text-gray-800 flex items-center gap-1">
                                        {userInfo.nickname}
                                        <ChevronRightIcon className="w-4 h-4 text-gray-400" />
                                    </h2>
                                    
                                    <div className="mt-1 flex items-center" onClick={(e) => e.stopPropagation()}>
                                        {subscription.isSub && !isExpired ? (
                                            <div className="flex items-center gap-1 bg-green-50 text-green-600 text-[10px] font-bold px-2 py-0.5 rounded-full border border-green-100">
                                                <CalendarDaysIcon className="w-3 h-3" />
                                                <span>{subscription.expireDate} 到期</span>
                                            </div>
                                        ) : (
                                            <div 
                                                onClick={() => setView('subscription')}
                                                className="flex items-center gap-1 bg-gray-100 text-gray-500 text-[10px] font-bold px-2 py-0.5 rounded-full border border-gray-200 cursor-pointer active:scale-95 transition-transform hover:bg-gray-200"
                                            >
                                                {isExpired && subscription.isSub ? (
                                                    <span className="text-red-500 flex items-center gap-1">
                                                        <ClockIcon className="w-3 h-3" /> 已过期 ({subscription.expireDate})
                                                    </span>
                                                ) : (
                                                    <span>未订阅会员</span>
                                                )}
                                                <span className="text-orange-500 ml-1">去订阅 &gt;</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Menu List */}
                        <div className="px-4 flex flex-col gap-3">
                            <MenuCard 
                                icon={UsersIcon} 
                                title="孩子管理" 
                                subtitle="添加或修改孩子信息"
                                color="text-blue-500"
                                bg="bg-blue-50"
                                onClick={() => setView('children')}
                            />
                            <MenuCard 
                                icon={CalendarDaysIcon} 
                                title="自测记录" 
                                subtitle="查看历史自测分析与建议"
                                color="text-orange-500"
                                bg="bg-orange-50"
                                onClick={() => setView('history')}
                            />
                            {/* Growth Report removed as per request */}
                            <MenuCard 
                                icon={CurrencyYenIcon} 
                                title="订阅管理" 
                                subtitle="查看会员状态或续费"
                                color="text-green-500"
                                bg="bg-green-50"
                                onClick={() => setView('subscription')}
                            />
                            <MenuCard 
                                icon={DocumentTextIcon} 
                                title="用户协议" 
                                subtitle="服务条款与隐私政策"
                                color="text-gray-500"
                                bg="bg-gray-100"
                                onClick={() => setView('agreement')}
                            />
                        </div>
                        
                        <div className="mt-8 text-center">
                             <p className="text-[10px] text-gray-300">Howtotalk App v1.2.0</p>
                        </div>
                    </div>
                );
        }
    };

    return renderContent();
};

// Helper Component for Menu Items
const MenuCard: React.FC<{
    icon: React.FC<any>; 
    title: string; 
    subtitle: string; 
    onClick: () => void;
    color: string;
    bg: string;
}> = ({ icon: Icon, title, subtitle, onClick, color, bg }) => (
    <button 
        onClick={onClick}
        className="w-full bg-white p-4 rounded-xl flex items-center justify-between shadow-sm hover:shadow-md transition-shadow active:scale-[0.99]"
    >
        <div className="flex items-center gap-4">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center ${bg} ${color}`}>
                <Icon className="w-5 h-5" />
            </div>
            <div className="text-left">
                <div className="font-bold text-gray-800 text-sm">{title}</div>
                <div className="text-xs text-gray-400">{subtitle}</div>
            </div>
        </div>
        <ChevronRightIcon className="text-gray-300 w-5 h-5" />
    </button>
);
