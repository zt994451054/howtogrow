
import { RawTestResult, BatchedResult, FeedbackData } from './types';

const SCENARIOS: FeedbackData[] = [
  {
    scenario: "当孩子因为弄丢玩具而大哭时...",
    yourOldWay: "“别哭了，再买一个不就行了。”",
    insightTitle: "示弱赋能 激发胜任感",
    insightBody: "你一把抢过鞋子替他穿好，孩子心里可能会觉得：“妈妈觉得我不行，只有她做才对。”久而久之，他就不再下次你可以试试蹲下来，轻声说：“妈妈相信你能穿好，需要时喊我‘救援队’哦！”然后忍住不插手。你这样做，孩子会感受到“原来我可以做到”，他的小脸上会慢慢浮现出“我能行”的光芒，动作反而越来越快。",
    yourNewWay: "“看到心爱的玩具不见了，你一定很难过。我们一起回想一下它可能在哪里？”"
  },
  {
    scenario: "当孩子拒绝穿衣服磨磨蹭蹭时...",
    yourOldWay: "“快点！再不穿好上学就要迟到了！”",
    insightTitle: "有限选择 培养自主权",
    insightBody: "催促往往会引发孩子的对抗心理。通过提供“蓝色这件还是红色这件”的有限选择，将控制权交还给孩子，能有效激发他们的自主意愿，减少晨间的权力争夺。",
    yourNewWay: "“你是想先穿这件蓝色的毛衣，还是先穿这条黑色的裤子呢？”"
  },
  {
    scenario: "当孩子在餐厅大声喧哗时...",
    yourOldWay: "“安静点！再吵我们现在就回家！”",
    insightTitle: "预设期待 建立规则感",
    insightBody: "公共场合的规则需要在进入之前达成共识。威胁通常只能带来短暂的服从，而提前约定的“悄悄话游戏”则能让孩子在参与中学习社交礼仪。",
    yourNewWay: "“在餐厅我们要用‘图书馆的声音’说话，如果你能坚持到吃完饭，我们就奖励一个拥抱。”"
  }
];

export const generateMockData = (count: number): RawTestResult[] => {
  const data: RawTestResult[] = [];
  const now = new Date();
  
  for (let i = 0; i < count; i++) {
    const testDate = new Date();
    testDate.setDate(now.getDate() - (count - i));
    
    data.push({
      id: `test-${i}`,
      date: testDate.toISOString().split('T')[0],
      scores: {
        dimension1: Math.floor(Math.random() * 40) + 40 + (i / count) * 20,
        dimension2: Math.floor(Math.random() * 30) + 50,
        dimension3: Math.floor(Math.random() * 50) + 20 + (i / count) * 30,
        dimension4: Math.floor(Math.random() * 20) + 70,
        dimension5: Math.floor(Math.random() * 60) + 30,
      },
      feedback: SCENARIOS[i % SCENARIOS.length]
    });
  }
  return data;
};

export const processBatches = (rawTests: RawTestResult[]): BatchedResult[] => {
  const batches: BatchedResult[] = [];
  const sortedTests = [...rawTests].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  for (let i = 0; i < sortedTests.length; i += 5) {
    const chunk = sortedTests.slice(i, i + 5);
    if (chunk.length === 5) {
      const avg = {
        dimension1: chunk.reduce((acc, curr) => acc + curr.scores.dimension1, 0) / 5,
        dimension2: chunk.reduce((acc, curr) => acc + curr.scores.dimension2, 0) / 5,
        dimension3: chunk.reduce((acc, curr) => acc + curr.scores.dimension3, 0) / 5,
        dimension4: chunk.reduce((acc, curr) => acc + curr.scores.dimension4, 0) / 5,
        dimension5: chunk.reduce((acc, curr) => acc + curr.scores.dimension5, 0) / 5,
      };

      batches.push({
        batchId: Math.floor(i / 5) + 1,
        completionDate: chunk[4].date,
        avgScores: avg,
        tests: chunk
      });
    }
  }
  return batches;
};
