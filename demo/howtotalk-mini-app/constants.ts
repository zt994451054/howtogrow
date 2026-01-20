
import { CourseCardData, Child, Question, SubscriptionPlan, TestRecord, GrowthRecord, BannerItem } from "./types";

export const APP_TITLE = "Howtotalk";

export const HERO_QUOTE = "爱不是替他做所有事，是忍住不做\n让他发现自己有多棒";

export const ABOUT_TEXT = "Parenting and communication expert Heleen de Hertog is the founder of Howtotalk. Using the Howtotalk method, she teaches you how to deal with every imaginable parenting challenge -...";

export const USER_AGREEMENT_HTML = `
  <h3 class="font-bold mb-2">1. 服务条款</h3>
  <p class="mb-4 text-gray-600">欢迎使用 Howtotalk。在使用本应用之前，请仔细阅读以下条款...</p>
  <h3 class="font-bold mb-2">2. 隐私政策</h3>
  <p class="mb-4 text-gray-600">我们非常重视您的隐私。您的所有数据仅存储于本地或加密传输...</p>
  <h3 class="font-bold mb-2">3. 订阅说明</h3>
  <p class="mb-4 text-gray-600">订阅服务为虚拟商品，一经售出概不退款。请在购买前确认套餐内容...</p>
`;

export const SUBSCRIPTION_PLANS: SubscriptionPlan[] = [
  { id: '1', name: '月度会员', days: 30, price: 29.9 },
  { id: '2', name: '季度会员', days: 90, price: 79.9 },
  { id: '3', name: '年度会员', days: 365, price: 199.9 },
];

export const MOCK_RECORDS: TestRecord[] = [
  { 
    id: 'r1', 
    date: new Date().toISOString().split('T')[0], // Today's date for testing "Done" state
    childName: '七仔', 
    answers: { 'q1': ['c'], 'q2': ['b'] } 
  },
  { 
    id: 'r2', 
    date: '2023-10-23', 
    childName: '七仔', 
    answers: { 'q1': ['a'], 'q3': ['b'] } 
  },
  { 
    id: 'r3', 
    date: '2023-10-20', 
    childName: '米娅', 
    answers: { 'q2': ['a'] } 
  }
];

export const MOCK_GROWTH_DATA: GrowthRecord[] = [
  { date: '11/21', logic: 70, knowledge: 62, reaction: 62, accuracy: 75, creativity: 65 },
  { date: '11/26', logic: 74, knowledge: 64, reaction: 55, accuracy: 81, creativity: 55 },
  { date: '12/01', logic: 68, knowledge: 68, reaction: 56, accuracy: 79, creativity: 54 },
  { date: '12/06', logic: 68, knowledge: 66, reaction: 73, accuracy: 83, creativity: 66 },
  { date: '12/11', logic: 73, knowledge: 58, reaction: 59, accuracy: 79, creativity: 71 },
  { date: '12/16', logic: 67, knowledge: 58, reaction: 82, accuracy: 80, creativity: 51 },
  { date: '12/21', logic: 68, knowledge: 71, reaction: 67, accuracy: 75, creativity: 77 },
  { date: '12/26', logic: 80, knowledge: 68, reaction: 71, accuracy: 82, creativity: 67 },
];

// Helper to generate dates
const generateParentCurveData = () => {
  const data = [];
  const today = new Date();
  // Generate data for the last 180 days (approx 6 months)
  for (let i = 180; i >= 0; i -= 2) { // Every 2 days
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().split('T')[0]; // YYYY-MM-DD
    
    // Create somewhat random smooth-ish curves
    const base = 50 + Math.sin(i / 20) * 20;
    
    data.push({
      date: dateStr,
      relation: Math.min(100, Math.max(20, Math.floor(base + Math.random() * 15 - 5))),
      rules: Math.min(100, Math.max(20, Math.floor(base - 10 + Math.random() * 20))),
      study: Math.min(100, Math.max(20, Math.floor(base + 5 + Math.sin(i/10)*10))),
      emotion: Math.min(100, Math.max(20, Math.floor(40 + Math.random() * 40))),
      communication: Math.min(100, Math.max(20, Math.floor(base + 10 + Math.random() * 10)))
    });
  }
  return data;
};

// Mock Data for Parent Curve - Generated for last 6 months
export const MOCK_PARENT_CURVE_DATA = generateParentCurveData();

export const COURSES: CourseCardData[] = [
  {
    id: "1",
    title: "Effective Communication with children",
    subtitle: "OF 2-12 YEARS",
    ageRange: "2-12",
    imageUrl: "https://picsum.photos/id/342/400/600", 
  },
  {
    id: "2",
    title: "Potty training",
    subtitle: "FOR 1-4 YEARS",
    ageRange: "1-4",
    imageUrl: "https://picsum.photos/id/338/400/600",
  },
  {
    id: "3",
    title: "Everything toddler",
    subtitle: "FOR 2-5 YEARS",
    ageRange: "2-5",
    imageUrl: "https://picsum.photos/id/1027/400/600",
  }
];

export const MOCK_CHILDREN: Child[] = [
  { id: '1', name: '七仔', age: 6, gender: 'boy', dob: '2017-05-20', relation: '妈妈' },
  { id: '2', name: '米娅', age: 4, gender: 'girl', dob: '2019-08-15', relation: '妈妈' }
];

export const MOCK_QUESTIONS: Question[] = [
  {
    id: "q1",
    type: "single",
    title: "当孩子因为动作慢、穿不好鞋子而大哭时...",
    options: [
      { 
        id: "a", 
        text: "“别哭了，我来帮你穿好就行了。”",
        sentiment: 'negative',
        adviceTitle: "示弱赋能 激发胜任感",
        advice: "你一把抢过鞋子替他穿好，孩子心里可能会觉得：“妈妈觉得我不行，只有她做才对。”久而久之，他就再也不尝试了。\n\n下次你可以试试蹲下来，轻声说：“妈妈相信你能穿好，需要时喊我‘救援队’哦！”然后忍住不插手。你这样做了，孩子会感受到“原来我可以做到”，他的小脸上会慢慢浮现出“我能行”的光芒，动作反而越来越快。"
      },
      { 
        id: "b", 
        text: "“哭也没用，自己想办法穿上。”",
        sentiment: 'negative',
        adviceTitle: "接纳情绪 避免冷漠",
        advice: "冷漠的回应会切断与孩子的连接。孩子哭泣是因为挫败感，建议先拥抱他，说：“鞋子确实有点难穿，我们一起来看看哪里卡住了。”"
      },
      { 
        id: "c", 
        text: "“这双鞋确实有点难穿，我们要不要试试松开鞋带？”",
        sentiment: 'positive',
        adviceTitle: "共情并提供支架",
        advice: "非常棒！你没有直接代劳，而是认可了任务的难度，并给出了具体的解决策略（松鞋带），这能帮助孩子建立解决问题的信心。"
      }
    ]
  },
  {
    id: "q2",
    type: "single", // Changed to single for clearer demo flow in results
    title: "当孩子拒绝分享玩具时，\n你通常会怎么说？",
    options: [
      { 
        id: "a", 
        text: "“你要学会分享，不能这么小气。”",
        sentiment: 'negative',
        adviceTitle: "强迫分享",
        advice: "强迫分享会让孩子觉得自己的物权没有被尊重，反而会更护食。孩子在6岁前主要是自我中心阶段，物权意识很强。"
      },
      { 
        id: "b", 
        text: "“这是你的玩具，你有权决定借不借。如果小朋友想玩，我们可以问问他愿不愿意拿他的玩具交换玩？”",
        sentiment: 'positive',
        adviceTitle: "尊重物权并引导交换",
        advice: "非常好！你首先确认了孩子的物权，让他感到安全。然后提供了解决问题的策略（交换），这有助于发展孩子的社交技能。"
      }
    ]
  }
];

export const SPARE_QUESTIONS: Question[] = [
    {
        id: "sq1",
        type: "single",
        title: "孩子在超市因为想买零食而撒泼打滚...",
        options: [
            {
                id: "a",
                text: "“太丢人了，赶紧起来！下次不带你出来了。”",
                sentiment: 'negative',
                adviceTitle: "恐吓与否定",
                advice: "这会让孩子感到被羞辱和被抛弃的恐惧。孩子哭闹是因为欲望没得到满足，而非故意让你丢脸。"
            },
            {
                id: "b",
                text: "“我知道你很想吃这个糖，看起来确实很好吃。但我们出门前约定过今天不买零食。我们可以把它记在愿望清单里，下次作为奖励买。”",
                sentiment: 'positive',
                adviceTitle: "共情并温和坚定",
                advice: "很棒！你先接纳了孩子的情绪和欲望（共情），然后坚持了原则（不买），最后给出了替代方案（愿望清单），既守住了底线又安抚了孩子。"
            }
        ]
    }
];

export const MOCK_BANNERS: BannerItem[] = [
    {
        id: '1',
        imageUrl: 'https://picsum.photos/seed/banner1/600/300',
        title: '如何应对孩子的Terrible Two？',
        content: `
            <h3>孩子两岁了，突然变了一个人？</h3>
            <p>很多父母会发现，孩子在两岁左右突然变得不可理喻。曾经乖巧的小天使，变成了一个动不动就说“不”的小恶魔。其实，这就是著名的“Terrible Two”（可怕的两岁）。</p>
            
            <h4 style="color:#F97316; margin-top:20px;">为什么会有Terrible Two？</h4>
            <p>这实际上是孩子<b>自我意识萌芽</b>的关键期。当孩子说“不”的时候，他其实是在表达：“我是独立的，我有自己的想法。”这不仅不是坏事，反而是孩子心理健康发展的标志。</p>
            
            <h4 style="color:#F97316; margin-top:20px;">家长应该如何应对？</h4>
            <ul style="list-style-type: disc; padding-left: 20px; color: #4B5563;">
                <li style="margin-bottom: 8px;"><b>提供有限的选择：</b> 不要问“你要不要洗澡？”，而是问“你想先洗澡还是先刷牙？”这让孩子感到自己有掌控权，同时也达成了你的目的。</li>
                <li style="margin-bottom: 8px;"><b>接纳情绪，限制行为：</b> 当孩子发脾气时，先接纳他的愤怒：“我知道你很想吃糖，不能吃让你很生气。”然后再重申规则：“但是我们快要吃饭了。”</li>
                <li style="margin-bottom: 8px;"><b>转移注意力：</b> 2岁的孩子注意力很短，当他们执着于某件危险物品时，用一个有趣的玩具吸引他的注意往往比讲道理更有效。</li>
            </ul>

            <h4 style="color:#F97316; margin-top:20px;">给父母的建议</h4>
            <p>保持耐心是关键。记住，孩子不是故意要激怒你，他只是在学习如何处理复杂的情绪和不断膨胀的自我意识。深呼吸，告诉自己：“这只是一个阶段，很快就会过去。”</p>
        `,
        author: 'Howtotalk 专家组',
        publishDate: '2023-10-01'
    },
    {
        id: '2',
        imageUrl: 'https://picsum.photos/seed/banner2/600/300',
        title: '亲子有效沟通的五个黄金技巧',
        content: `
            <h3>你说的话，孩子真的听进去了吗？</h3>
            <p>很多时候，我们觉得自己在跟孩子沟通，其实只是在单方面输出。真正的沟通是双向的流动。以下是五个简单实用的技巧，帮助你建立更亲密的亲子连接。</p>
            
            <h4 style="color:#F97316; margin-top:20px;">1. 蹲下来，平视眼睛</h4>
            <p>居高临下的姿态会给孩子带来压迫感。当你蹲下来，看着孩子的眼睛说话时，传递的信息是：“我尊重你，我愿意认真听你说话。”</p>
            
            <h4 style="color:#F97316; margin-top:20px;">2. 积极倾听与复述</h4>
            <p>当孩子跟你诉苦时，不要急着给建议或讲大道理。试着复述他的话：“哦，听起来你因为朋友拿走了你的玩具感到很伤心，是吗？”这让孩子感到被理解。</p>
            
            <h4 style="color:#F97316; margin-top:20px;">3. 用“我”字句代替“你”字句</h4>
            <p>❌ 错误：“你太磨蹭了！”（指责）<br/>✅ 正确：“看到我们快迟到了，我感到很着急。”（表达感受）<br/>这样可以避免孩子产生防御心理，更愿意配合。</p>

            <h4 style="color:#F97316; margin-top:20px;">4. 赋予孩子幻想中的满足</h4>
            <p>当孩子想要一个无法得到的东西时，用幻想满足他往往能平息情绪。<br/>孩子：“我要吃冰淇淋！”<br/>家长：“如果我现在能变出一个像房子那么大的冰淇淋给你吃就好了！”<br/>孩子可能会破涕为笑。</p>

            <h4 style="color:#F97316; margin-top:20px;">5. 寻找共同解决办法</h4>
            <p>遇到冲突时，邀请孩子一起想办法：“我们有一个问题，你想看电视，我想让你睡觉，我们该怎么解决呢？”这能培养孩子解决问题的能力。</p>
        `,
        author: 'Heleen de Hertog',
        publishDate: '2023-10-05'
    }
];
