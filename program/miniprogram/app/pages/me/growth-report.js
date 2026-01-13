const { fetchChildren, getCachedChildren } = require("../../services/children");
const { fetchGrowthReport } = require("../../services/reports");
const { calcAge, formatDateYmd } = require("../../utils/date");

const echarts = require("../../components/ec-canvas/echarts");
let chartInstance = null;
let pendingOption = null;

function toXLabel(ymd) {
  const parts = String(ymd || "").split("-");
  if (parts.length !== 3) return String(ymd || "");
  return `${Number(parts[1])}/${parts[2]}`;
}

function normalizeDays(days) {
  const list = Array.isArray(days) ? [...days] : [];
  list.sort((a, b) => String(a?.bizDate || "").localeCompare(String(b?.bizDate || "")));
  return list;
}

function resolveDimensionsFromApi(days) {
  // Render strictly based on backend payload order (first appearance wins).
  const ordered = [];
  const seen = new Set();
  for (const day of days || []) {
    const scores = Array.isArray(day?.dimensionScores) ? day.dimensionScores : [];
    for (const s of scores) {
      const code = String(s?.dimensionCode || "").trim();
      if (!code || seen.has(code)) continue;
      seen.add(code);
      ordered.push({ code, name: String(s?.dimensionName || code) });
    }
  }
  return ordered;
}

function getDayScore(day, dimensionCode) {
  const scores = Array.isArray(day?.dimensionScores) ? day.dimensionScores : [];
  const match = scores.find((s) => String(s?.dimensionCode || "").trim() === dimensionCode);
  if (!match) return null;
  const n = Number(match.score);
  return Number.isFinite(n) ? n : null;
}

function buildOption(daysRaw) {
  const days = normalizeDays(daysRaw);
  const x = days.map((d) => toXLabel(d.bizDate));
  const dims = resolveDimensionsFromApi(days);
  const showSymbol = x.length > 0 && x.length < 2;

  const palette = [
    "#F97316",
    "#3B82F6",
    "#10B981",
    "#EC4899",
    "#8B5CF6",
    "#14B8A6",
    "#A855F7",
    "#64748B",
  ];

  const allValues = [];
  const series = dims.map((dim, idx) => {
    const data = days.map((d) => {
      const score = getDayScore(d, dim.code);
      if (score !== null) allValues.push(score);
      return score;
    });
    return {
      name: dim.name,
      type: "line",
      smooth: true,
      showSymbol,
      symbol: "circle",
      symbolSize: 6,
      connectNulls: false,
      lineStyle: { width: 3 },
      itemStyle: { color: palette[(idx + 1) % palette.length] },
      data,
    };
  });

  const maxScore = allValues.length ? Math.max(...allValues) : 0;
  const yMax = Math.max(5, Math.ceil(maxScore * 1.2));

  return {
    color: palette,
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(255,255,255,0.95)",
      borderColor: "#eee",
      textStyle: { color: "#333", fontSize: 12 },
      padding: 10,
      valueFormatter: (v) => (typeof v === "number" ? String(v) : "-"),
    },
    legend: {
      type: "scroll",
      data: series.map((s) => s.name),
      bottom: 0,
      icon: "circle",
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { fontSize: 10, color: "#666" },
    },
    grid: { left: "3%", right: "4%", bottom: "18%", top: "8%", containLabel: true },
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
      min: 0,
      max: yMax,
      minInterval: 1,
      splitLine: { lineStyle: { color: "#F3F4F6", type: "dashed" } },
      axisLabel: { color: "#9CA3AF", fontSize: 10 },
    },
    series,
  };
}

Page({
  data: {
    children: [],
    selectedChildId: 0,
    // Keep this JSON-serializable; chart init is handled via `bind:init`.
    ec: {},
    chartOption: null,
    reportLoading: false,
    reportEmpty: false,
  },
  onShow() {
    const cached = getCachedChildren();
    if (cached) this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    fetchChildren()
      .then((list) => this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) }))
      .catch(() => {});
  },
  onBack() {
    wx.navigateBack();
  },
  onPickChild(e) {
    const childId = Number(e.currentTarget.dataset.id);
    this.setData({ selectedChildId: childId });
    this.loadReport(childId);
  },
  onEcInit(e) {
    const { canvas, width, height, dpr, canvasDpr } = e.detail || {};
    const pixelRatio = Number.isFinite(Number(dpr)) ? Number(dpr) : Number.isFinite(Number(canvasDpr)) ? Number(canvasDpr) : 1;
    if (!canvas || !Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
      console.warn("[growth-report] ec-canvas init invalid size:", { width, height, dpr: pixelRatio });
      return;
    }
    const chart = echarts.init(canvas, null, { width, height, devicePixelRatio: pixelRatio });
    canvas.setChart(chart);
    chartInstance = chart;
    if (pendingOption) {
      chart.setOption(pendingOption, true);
    }
  },
  loadReport(childId) {
    const today = new Date();
    const to = formatDateYmd(today);
    const from = formatDateYmd(new Date(today.getTime() - 30 * 24 * 3600 * 1000));
    this.setData({ reportLoading: true, reportEmpty: false });
    wx.showLoading({ title: "加载中…" });
    fetchGrowthReport(childId, from, to)
      .then((report) => {
        const days = Array.isArray(report?.days) ? report.days : [];
        const option = buildOption(days);
        pendingOption = option;
        const hasAnyPoint = days.some((d) => Array.isArray(d?.dimensionScores) && d.dimensionScores.length > 0);
        this.setData({ chartOption: option, reportEmpty: !hasAnyPoint });
        if (chartInstance) chartInstance.setOption(option, true);
      })
      .catch(() => {
        pendingOption = null;
        this.setData({ chartOption: null, reportEmpty: true });
        if (chartInstance) chartInstance.clear();
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ reportLoading: false });
      });
  },
  goTest() {
    wx.switchTab({ url: "/pages/test/index" });
  },
});
