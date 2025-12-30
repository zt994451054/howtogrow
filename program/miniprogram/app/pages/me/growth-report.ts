import { fetchChildren, getCachedChildren } from "../../services/children";
import { fetchGrowthReport } from "../../services/reports";
import { calcAge, formatDateYmd } from "../../utils/date";

const echarts = require("../../components/ec-canvas/echarts");
let chartInstance: any = null;

function toXLabel(ymd: string): string {
  const parts = ymd.split("-");
  if (parts.length !== 3) return ymd;
  return `${Number(parts[1])}/${parts[2]}`;
}

function buildMockDays(from: string, to: string) {
  const dates = ["2025-11-21", "2025-11-26", "2025-12-01", "2025-12-06", "2025-12-11", "2025-12-16", "2025-12-21", "2025-12-26"];
  const mk = (bizDate: string, scores: number[]) => ({
    bizDate,
    dimensionScores: [
      { dimensionCode: "logic", dimensionName: "逻辑思维", score: scores[0] },
      { dimensionCode: "knowledge", dimensionName: "知识储备", score: scores[1] },
      { dimensionCode: "reaction", dimensionName: "反应速度", score: scores[2] },
      { dimensionCode: "accuracy", dimensionName: "准确度", score: scores[3] },
      { dimensionCode: "creativity", dimensionName: "创造力", score: scores[4] },
    ],
  });
  const data = [
    mk(dates[0], [70, 62, 62, 75, 65]),
    mk(dates[1], [74, 64, 55, 81, 55]),
    mk(dates[2], [68, 68, 56, 79, 54]),
    mk(dates[3], [68, 66, 73, 83, 66]),
    mk(dates[4], [73, 58, 59, 79, 71]),
    mk(dates[5], [67, 58, 82, 80, 51]),
    mk(dates[6], [68, 71, 67, 75, 77]),
    mk(dates[7], [80, 68, 71, 82, 67]),
  ];
  return { childId: 0, from, to, days: data };
}

function buildOption(days: any[]) {
  const palette = ["#F97316", "#EC4899", "#10B981", "#3B82F6", "#8B5CF6"];
  const dims = ["逻辑思维", "知识储备", "反应速度", "准确度", "创造力"];
  const x = days.map((d) => toXLabel(d.bizDate));

  const series = dims.map((name, idx) => ({
    name,
    type: "line",
    smooth: true,
    showSymbol: false,
    lineStyle: { width: 3 },
    data: days.map((d) => {
      const match = d.dimensionScores.find((s: any) => s.dimensionName === name);
      return match ? match.score : 0;
    }),
  }));

  return {
    color: palette,
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(255,255,255,0.95)",
      borderColor: "#eee",
      textStyle: { color: "#333", fontSize: 12 },
      padding: 10,
    },
    legend: {
      data: dims,
      bottom: 0,
      icon: "circle",
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { fontSize: 10, color: "#666" },
    },
    grid: { left: "3%", right: "4%", bottom: "15%", top: "5%", containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: x,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: "#9CA3AF", fontSize: 10, margin: 12 },
    },
    yAxis: {
      type: "value",
      min: 40,
      max: 100,
      splitLine: { lineStyle: { color: "#F3F4F6", type: "dashed" } },
      axisLabel: { color: "#9CA3AF", fontSize: 10 },
    },
    series,
  };
}

Page({
  data: {
    children: [] as any[],
    selectedChildId: 0,
    ec: {
      onInit: (canvas: any, width: number, height: number, dpr: number) => {
        const chart = echarts.init(canvas, null, { width, height, devicePixelRatio: dpr });
        canvas.setChart(chart);
        chartInstance = chart;
        return chart;
      },
    },
    chartOption: null as any,
    recentCards: [] as any[],
  },
  async onShow() {
    const cached = getCachedChildren();
    if (cached) {
      this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    }
    try {
      const list = await fetchChildren();
      this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    } catch {
      // ignore
    }
  },
  onBack() {
    wx.navigateBack();
  },
  async onPickChild(e: WechatMiniprogram.BaseEvent) {
    const childId = Number((e.currentTarget as any).dataset.id);
    this.setData({ selectedChildId: childId });
    await this.loadReport(childId);
  },
  async loadReport(childId: number) {
    const today = new Date();
    const to = formatDateYmd(today);
    const from = formatDateYmd(new Date(today.getTime() - 30 * 24 * 3600 * 1000));
    wx.showLoading({ title: "加载中…" });
    try {
      const report = await fetchGrowthReport(childId, from, to);
      const option = buildOption(report.days);
      const cards = report.days
        .slice(-4)
        .reverse()
        .map((d: any) => ({
          bizDate: d.bizDate,
          title: "预设期待 建立规则感",
        }));
      this.setData({ chartOption: option, recentCards: cards });
      if (chartInstance) chartInstance.setOption(option, true);
    } catch {
      const report = buildMockDays(from, to);
      const option = buildOption(report.days);
      this.setData({
        chartOption: option,
        recentCards: report.days
          .slice(-4)
          .reverse()
          .map((d: any) => ({ bizDate: d.bizDate, title: "预设期待 建立规则感" })),
      });
      if (chartInstance) chartInstance.setOption(option, true);
      wx.showToast({ title: "后端未连接，使用演示数据", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },
  goTest() {
    wx.switchTab({ url: "/pages/test/index" });
  },
});
