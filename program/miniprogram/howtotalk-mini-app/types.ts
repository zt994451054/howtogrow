
export interface CourseCardData {
  id: string;
  title: string;
  subtitle: string;
  imageUrl: string;
  duration?: string;
  ageRange: string;
}

export interface IconProps {
  className?: string;
  color?: string;
}

export interface Child {
  id: string;
  name: string;
  age: number; // calculated from DOB in real app, keeping number for simplicity or adding dob string
  dob?: string;
  gender: 'boy' | 'girl';
  relation?: string; // e.g., "妈妈", "爸爸"
}

export interface Option {
  id: string;
  text: string;
  // New fields for result feedback
  sentiment?: 'positive' | 'negative' | 'neutral'; // positive = green check, negative = red warning
  adviceTitle?: string; // e.g. "示弱赋能 激发胜任感"
  advice?: string; // The long paragraph text
}

export interface Question {
  id: string;
  type: 'single' | 'multiple';
  title: string; // The orange text
  subtitle?: string; // Optional context
  options: Option[];
}

export interface SubscriptionPlan {
  id: string;
  name: string;
  days: number;
  price: number;
}

export interface TestRecord {
  id: string;
  date: string;
  childName: string;
  score?: number; // Optional
  answers: Record<string, string[]>; // Store selections to render the result view
}

export interface GrowthRecord {
  date: string;
  logic: number;      // 逻辑思维
  knowledge: number;  // 知识储备
  reaction: number;   // 反应速度
  accuracy: number;   // 准确度
  creativity: number; // 创造力
}

export interface ObservationRecord {
  id: string;
  date: string; // YYYY-MM-DD
  day: number;
  weekday: string;
  isDone: boolean;
  mood?: string; // emoji character or ID
  frustrationIds?: string[]; // Array of IDs for multiple selection
  imageUrl?: string;
  title?: string;
  content?: string;
  // Detail View Fields
  summaryQuote?: string; // Top orange text
  moodDescription?: string; // Description under mood
  frustrationText?: string; 
  mirrorText?: string;
  diaryText?: string;
  expertText?: string;
}

export interface BannerItem {
  id: string;
  imageUrl: string;
  link?: string;
  // New fields for Article Detail
  title: string;
  content: string; // HTML string
  author?: string;
  publishDate?: string;
}
