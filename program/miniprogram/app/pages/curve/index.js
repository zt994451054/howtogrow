const { ensureLoggedIn, getCachedMe } = require("../../services/auth");
const { fetchChildren } = require("../../services/children");
const { getMonthlyAwareness } = require("../../services/awareness");
const { STORAGE_KEYS } = require("../../services/config");
const { getStorage, removeStorage, setStorage } = require("../../services/storage");
const { fetchGrowthReport, fetchAwarenessPersistence } = require("../../services/reports");
const { formatDateYmd, calcAge } = require("../../utils/date");
const { getSystemMetrics } = require("../../utils/system");

const echarts = require("../../components/ec-canvas/echarts");
let chartInstance = null;
let pendingOption = null;
let lastDaysRaw = [];
let awarenessReqSeq = 0;
const MAX_RANGE_DAYS = 180;

function toSafeId(value) {
  const n = Number(value || 0);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

function pickValidChildId(children, preferredId) {
  const list = Array.isArray(children) ? children : [];
  const preferred = toSafeId(preferredId);
  if (preferred && list.some((c) => toSafeId(c?.id) === preferred)) return preferred;
  return list.length ? toSafeId(list[0]?.id) : 0;
}

function hexToRgba(hex, alpha) {
  const match = /^#?([0-9a-fA-F]{6})$/.exec(String(hex || "").trim());
  const aRaw = Number(alpha);
  const a = Number.isFinite(aRaw) ? Math.min(1, Math.max(0, aRaw)) : 0.12;
  if (!match) return `rgba(0, 0, 0, ${a})`;
  const value = parseInt(match[1], 16);
  const r = (value >> 16) & 255;
  const g = (value >> 8) & 255;
  const b = value & 255;
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}

const DIMENSIONS = [
  {
    code: "COMMUNICATION_EXPRESSION",
    label: "沟通表达",
    desc: "是否能倾听、共情、开放式提问、非暴力沟通",
    color: "#8B5CF6",
  },
  {
    code: "EMOTION_MANAGEMENT",
    label: "情绪管理",
    desc: "是否能控制自己情绪，不吼、不急躁",
    color: "#3B82F6",
  },
  {
    code: "RULE_GUIDANCE",
    label: "规则引导",
    desc: "是否能设定清晰规则，并持续执行",
    color: "#EC4899",
  },
  {
    code: "LEARNING_SUPPORT",
    label: "学习支持",
    desc: "是否能关注过程、激发内驱、降低孩子压力",
    color: "#10B981",
  },
  {
    code: "RELATIONSHIP_BUILDING",
    label: "关系建设",
    desc: "是否能接住孩子情绪、建立安全信任的关系",
    color: "#F97316",
  },
].map((d) => ({ ...d, bg: hexToRgba(d.color, 0.12) }));

const DEFAULT_DIM_PILL_BG = hexToRgba("#f08019", 0.12);

const STATUS_IMAGE_BY_LABEL = {
  乐观: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E4%B9%90%E8%A7%82.jpg",
  失望: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%A4%B1%E6%9C%9B.jpg",
  平静: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%B9%B3%E9%9D%99.jpg",
  开心: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%BC%80%E5%BF%83.jpg",
  愤怒: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%84%A4%E6%80%92.jpg",
  担忧: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%8B%85%E5%BF%A7.jpg",
  无奈: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%97%A0%E5%A5%88.jpg",
  欣慰: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%AC%A3%E6%85%B0.jpg",
  绝望: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E7%BB%9D%E6%9C%9B.jpg",
  难过: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E9%9A%BE%E8%BF%87.jpg",
};

const STATUS_DEFS = [
  { moodId: "happy", label: "开心", emoji: "😄", imageUrl: STATUS_IMAGE_BY_LABEL["开心"], color: "#F97316", bg: "rgba(249, 115, 22, 0.12)" },
  { moodId: "optimistic", label: "乐观", emoji: "🙂", imageUrl: STATUS_IMAGE_BY_LABEL["乐观"], color: "#F97316", bg: "rgba(249, 115, 22, 0.12)" },
  { moodId: "calm", label: "平静", emoji: "😌", imageUrl: STATUS_IMAGE_BY_LABEL["平静"], color: "#10B981", bg: "rgba(16, 185, 129, 0.12)" },
  { moodId: "relieved", label: "欣慰", emoji: "🥰", imageUrl: STATUS_IMAGE_BY_LABEL["欣慰"], color: "#EC4899", bg: "rgba(236, 72, 153, 0.12)" },
  { moodId: "sad", label: "难过", emoji: "🥹", imageUrl: STATUS_IMAGE_BY_LABEL["难过"], color: "#3B82F6", bg: "rgba(59, 130, 246, 0.12)" },
  { moodId: "worried", label: "担忧", emoji: "😟", imageUrl: STATUS_IMAGE_BY_LABEL["担忧"], color: "#64748B", bg: "rgba(100, 116, 139, 0.10)" },
  { moodId: "disappointed", label: "失望", emoji: "😞", imageUrl: STATUS_IMAGE_BY_LABEL["失望"], color: "#64748B", bg: "rgba(100, 116, 139, 0.10)" },
  { moodId: "helpless", label: "无奈", emoji: "😮‍💨", imageUrl: STATUS_IMAGE_BY_LABEL["无奈"], color: "#64748B", bg: "rgba(100, 116, 139, 0.10)" },
  { moodId: "angry", label: "愤怒", emoji: "😡", imageUrl: STATUS_IMAGE_BY_LABEL["愤怒"], color: "#EF4444", bg: "rgba(239, 68, 68, 0.12)" },
  { moodId: "desperate", label: "绝望", emoji: "😭", imageUrl: STATUS_IMAGE_BY_LABEL["绝望"], color: "#3B82F6", bg: "rgba(59, 130, 246, 0.12)" },
];

const STATUS_MOOD_ID_BY_LABEL = STATUS_DEFS.reduce((acc, cur) => {
  acc[cur.label] = cur.moodId;
  return acc;
}, {});

function parseMonthValue(value) {
  const v = String(value || "").trim();
  const m = /^(\d{4})-(\d{2})$/.exec(v);
  if (!m) return null;
  const year = Number(m[1]);
  const month = Number(m[2]);
  if (!year || month < 1 || month > 12) return null;
  return { year, month };
}

function formatMonthValue(year, month) {
  const y = Number(year);
  const m = Number(month);
  if (!y || m < 1 || m > 12) return "";
  return `${String(y).padStart(4, "0")}-${String(m).padStart(2, "0")}`;
}

function listMonthValuesBetween(fromYmd, toYmd) {
  const from = String(fromYmd || "").slice(0, 7);
  const to = String(toYmd || "").slice(0, 7);
  const start = parseMonthValue(from);
  const end = parseMonthValue(to);
  if (!start || !end) return [];

  const months = [];
  let y = start.year;
  let m = start.month;
  while (y < end.year || (y === end.year && m <= end.month)) {
    months.push(formatMonthValue(y, m));
    m += 1;
    if (m > 12) {
      m = 1;
      y += 1;
    }
  }
  return months;
}

function buildStatusStats(days, fromYmd, toYmd) {
  const from = String(fromYmd || "").trim();
  const to = String(toYmd || "").trim();
  const counts = {};
  STATUS_DEFS.forEach((s) => {
    counts[s.moodId] = 0;
  });

  (Array.isArray(days) ? days : []).forEach((d) => {
    const date = d && d.recordDate != null ? String(d.recordDate) : "";
    if (!date || date < from || date > to) return;
    const moodId = d && d.parentingStatusMoodId ? String(d.parentingStatusMoodId).trim() : "";
    const code = d && d.parentingStatusCode ? String(d.parentingStatusCode).trim() : "";
    const key = moodId || STATUS_MOOD_ID_BY_LABEL[code] || "";
    if (!key || counts[key] == null) return;
    counts[key] += 1;
  });

  const total = Object.values(counts).reduce((acc, cur) => acc + Number(cur || 0), 0);
  const items = STATUS_DEFS.map((s, idx) => {
    const count = Number(counts[s.moodId] || 0);
    const percentage = total ? Math.round((count / total) * 100) : 0;
    return { ...s, count, percentage, _idx: idx };
  });

  items.sort((a, b) => b.percentage - a.percentage || b.count - a.count || a._idx - b._idx);
  const maxPct = Math.max(...items.map((m) => m.percentage), 1);
  items.forEach((m) => {
    m.barHeight = Math.max(6, Math.round((m.percentage / maxPct) * 56));
  });
  items.forEach((m) => delete m._idx);
  return { total, items };
}

function buildTopTroubles(days, fromYmd, toYmd) {
  const from = String(fromYmd || "").trim();
  const to = String(toYmd || "").trim();
  if (!from || !to) return [];

  const counts = new Map(); // sceneId -> count
  const meta = new Map(); // sceneId -> { name, logoUrl, shortName }

  (Array.isArray(days) ? days : []).forEach((d) => {
    const date = d && d.recordDate != null ? String(d.recordDate) : "";
    if (!date || date < from || date > to) return;
    const scenes = Array.isArray(d?.troubleScenes) ? d.troubleScenes : [];
    scenes.forEach((s) => {
      const id = s && s.id != null ? String(s.id).trim() : "";
      const name = s && s.name != null ? String(s.name).trim() : "";
      if (!id) return;
      counts.set(id, (counts.get(id) || 0) + 1);
      const existing = meta.get(id);
      const logoUrl = s && s.logoUrl != null ? String(s.logoUrl).trim() : "";
      if (!existing) {
        meta.set(id, { name, logoUrl, shortName: name ? name.slice(0, 1) : "?" });
      } else if (!existing.logoUrl && logoUrl) {
        meta.set(id, { ...existing, logoUrl });
      }
    });
  });

  return Array.from(counts.entries())
    .map(([id, count]) => {
      const m = meta.get(id) || { name: "", logoUrl: "", shortName: "?" };
      return { id, count, name: m.name || `场景${id}`, logoUrl: m.logoUrl || "", shortName: m.shortName || "?" };
    })
    .sort((a, b) => b.count - a.count || String(a.name).localeCompare(String(b.name)))
    .slice(0, 3)
    .map((t, idx) => ({ rank: idx + 1, ...t }));
}

function toMmDd(ymd) {
  const parts = String(ymd || "").split("-");
  if (parts.length !== 3) return String(ymd || "");
  return `${String(parts[1]).padStart(2, "0")}.${String(parts[2]).padStart(2, "0")}`;
}

function toXLabel(ymd) {
  const parts = String(ymd || "").split("-");
  if (parts.length !== 3) return String(ymd || "");
  const mm = String(parts[1]).padStart(2, "0");
  const dd = String(parts[2]).padStart(2, "0");
  return `${mm}-${dd}`;
}

function getDaysDiffInclusive(fromYmd, toYmd) {
  const from = new Date(`${fromYmd}T00:00:00`);
  const to = new Date(`${toYmd}T00:00:00`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) return 0;
  const diff = Math.abs(to.getTime() - from.getTime());
  return Math.floor(diff / (24 * 3600 * 1000)) + 1;
}

function addDaysYmd(ymd, deltaDays) {
  const d = new Date(`${String(ymd || "").trim()}T00:00:00`);
  if (Number.isNaN(d.getTime())) return "";
  d.setDate(d.getDate() + Number(deltaDays || 0));
  return formatDateYmd(d);
}

function clampDateRange(fromYmd, toYmd, todayYmd, minYmd, anchor) {
  let from = String(fromYmd || "").trim();
  let to = String(toYmd || "").trim();
  const today = String(todayYmd || "").trim();
  const min = String(minYmd || "").trim();
  const keep = anchor === "from" ? "from" : "to";
  if (!from || !to || !today) return { from: from || "", to: to || "", clamped: false };

  let clamped = false;
  if (min) {
    if (from < min) {
      from = min;
      clamped = true;
    }
    if (to < min) {
      to = min;
      clamped = true;
    }
  }
  if (to > today) {
    to = today;
    clamped = true;
  }
  if (from > today) {
    from = today;
    clamped = true;
  }
  if (from > to) {
    if (keep === "from") to = from;
    else from = to;
    clamped = true;
  }
  const days = getDaysDiffInclusive(from, to);
  if (days > MAX_RANGE_DAYS) {
    if (keep === "from") {
      let nextTo = addDaysYmd(from, MAX_RANGE_DAYS - 1);
      if (nextTo > today) nextTo = today;
      if (min && nextTo < min) nextTo = min;
      to = nextTo;
      if (from > to) {
        from = to;
      }
    } else {
      let nextFrom = addDaysYmd(to, -(MAX_RANGE_DAYS - 1));
      if (min && nextFrom < min) nextFrom = min;
      from = nextFrom;
      if (from > to) {
        to = from;
      }
    }
    clamped = true;
  }
  return { from, to, clamped };
}

function normalizeDays(days) {
  const list = Array.isArray(days) ? [...days] : [];
  list.sort((a, b) => String(a?.bizDate || "").localeCompare(String(b?.bizDate || "")));
  return list;
}

function getDayScore(day, dimensionCode) {
  const scores = Array.isArray(day?.dimensionScores) ? day.dimensionScores : [];
  const match = scores.find((s) => String(s?.dimensionCode || "").trim() === dimensionCode);
  if (!match) return null;
  const n = Number(match.score);
  return Number.isFinite(n) ? n : null;
}

function buildOption(daysRaw, visibleCodes) {
  const days = normalizeDays(daysRaw);
  const xLabels = days.map((d) => toXLabel(d.bizDate));
  const padCount = xLabels.length === 1 ? 5 : 0;
  const xAxisData = padCount ? [xLabels[0], ...Array.from({ length: padCount }, () => "")] : xLabels;
  const visible = Array.isArray(visibleCodes) && visibleCodes.length ? visibleCodes : DIMENSIONS.map((d) => d.code);
  const showSymbol = xLabels.length > 0 && xLabels.length < 2;
  const title = {
    text: "能力分值",
    left: 0,
    top: 0,
    padding: 0,
    textStyle: { color: "#9CA3AF", fontSize: 14, fontWeight: "normal" },
  };
  const grid = { left: "3%", right: "4%", bottom: "8%", top: 36, containLabel: true };

  const allValues = [];
  const series = DIMENSIONS.map((dim) => {
    const isOn = visible.includes(dim.code);
    const data = days.map((d) => {
      if (!isOn) return null;
      const score = getDayScore(d, dim.code);
      if (score !== null) allValues.push(score);
      return score;
    });
    if (padCount) {
      data.push(...Array.from({ length: padCount }, () => null));
    }
    return {
      name: dim.label,
      type: "line",
      smooth: true,
      showSymbol,
      symbol: "circle",
      symbolSize: 6,
      connectNulls: false,
      lineStyle: { width: 3, color: dim.color, opacity: isOn ? 1 : 0 },
      itemStyle: { color: dim.color, opacity: isOn ? 1 : 0 },
      emphasis: { focus: "series" },
      silent: !isOn,
      data,
    };
  });

  const hasAnyPoint = allValues.length > 0;
  if (!hasAnyPoint) {
    const placeholderX = ["", "", "", "", "", ""];
    return {
      title,
      color: ["#F97316"],
      tooltip: { show: false },
      grid,
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: placeholderX,
        axisLine: { show: true, lineStyle: { color: "#6B7280", width: 1 } },
        axisTick: { show: false },
        axisLabel: { show: false },
      },
      yAxis: {
        type: "value",
        min: 0,
        max: 100,
        splitLine: { lineStyle: { color: "#F3F4F6", type: "dashed" } },
        axisLabel: { color: "#9CA3AF", fontSize: 10 },
        axisLine: { show: false },
      },
      series: [
        {
          type: "line",
          smooth: true,
          showSymbol: false,
          silent: true,
          lineStyle: { width: 3, color: "#F97316", type: "dashed", opacity: 0.75 },
          data: placeholderX.map(() => 60),
        },
      ],
    };
  }
  return {
    title,
    color: DIMENSIONS.map((d) => d.color),
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "line", lineStyle: { color: "#D1D5DB", width: 1, type: "dashed" } },
      backgroundColor: "rgba(255,255,255,0.95)",
      borderColor: "#eee",
      textStyle: { color: "#333", fontSize: 12 },
      padding: 10,
      confine: true,
      valueFormatter: (v) => (typeof v === "number" ? String(v) : "-"),
    },
    grid,
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: xAxisData,
      axisLine: { show: true, lineStyle: { color: "#6B7280", width: 1 } },
      axisTick: { show: false },
      axisLabel: { color: "#9CA3AF", fontSize: 10, margin: 12 },
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: "#F3F4F6", type: "dashed" } },
      axisLabel: { color: "#9CA3AF", fontSize: 10 },
      axisLine: { show: false },
    },
    series,
  };
}

Page({
  data: {
    navBarHeight: 0,
    menuOpen: false,
    menuPopoverTopPx: 0,
    children: [],
    childrenLoaded: false,
    childrenLoading: false,
    selectedChildId: 0,
    greetText: "您好",
    progressName: "",
    avatarText: "妈",
    avatarUrl: "",

    today: "",
    dateFrom: "",
    dateTo: "",
    dateFromText: "",
    dateToText: "",
    rangeLabel: "近90天",
    rangeDays: 90,
    persistenceDays: 1,
    customRangeOpen: false,
    customMinDate: "2020-01-01",
    customFromDraft: "",
    customToDraft: "",
    customRangeDays: 0,

    dimensions: DIMENSIONS.map((d, idx) => ({ ...d, _on: idx === 0 })),
    visibleDimensionCodes: DIMENSIONS.length ? [DIMENSIONS[0].code] : [],
    defaultDimPillBg: DEFAULT_DIM_PILL_BG,

    // Keep this JSON-serializable; chart init is handled via `bind:init`.
    // Enable touch so users can drag on chart to inspect daily values.
    ec: { disableTouch: false },
    chartEmpty: false,
    chartLoading: false,

    statusStats: [],
    statusStatsTop: [],
    statusStatsTopLastIndex: 0,
    statusTotalDays: 0,
    statusLoading: false,
    troubleLoading: false,
    topTroubles: [],
  },

  onLoad() {
    const { navBarHeight } = getSystemMetrics();
    const menuRect = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
    const navHeight = menuRect && Number(menuRect.bottom || 0) > 0 ? Number(menuRect.bottom) : Number(navBarHeight || 0);
    const menuPopoverTopPx = menuRect ? Math.max(0, Number(menuRect.bottom || 0) + 56) : Math.max(0, Number(navHeight || 0) + 56);

    const today = new Date();
    const dateTo = formatDateYmd(today);
    const rangeDays = 90;
    const dateFrom = formatDateYmd(new Date(today.getTime() - rangeDays * 24 * 3600 * 1000));

    this.setData({
      navBarHeight: navHeight,
      menuPopoverTopPx,
      today: dateTo,
      dateFrom,
      dateTo,
      dateFromText: toMmDd(dateFrom),
      dateToText: toMmDd(dateTo),
      rangeLabel: "近90天",
      rangeDays,
    });

    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    this.initPage();
  },

  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 1 });

    const cached = getCachedMe();
    if (cached) this.applyMe(cached);

    // As a tab page, refresh children list every time the user opens this page.
    ensureLoggedIn()
      .then((me) => {
        if (me) this.applyMe(me);
        return this.loadChildren();
      })
      .catch(() => {
        this.setData({ childrenLoaded: true });
      });
  },

  onHide() {
    this.setData({ menuOpen: false });
  },

  onUnload() {
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    pendingOption = null;
    lastDaysRaw = [];
  },
  onOpenProfile() {
    ensureLoggedIn()
      .then(() => wx.navigateTo({ url: "/pages/me/profile" }))
      .catch(() => {});
  },

  applyMe(me) {
    const avatarUrl = me && me.avatarUrl != null ? String(me.avatarUrl).trim() : "";
    const nickname = me && me.nickname != null ? String(me.nickname).trim() : "";
    const avatarText = nickname ? nickname.slice(0, 1) : "我";
    this.setData({ avatarUrl, avatarText });
  },

  initPage() {
    ensureLoggedIn()
      .then((me) => {
        if (me) this.applyMe(me);
        return this.loadChildren();
      })
      .catch(() => {
        this.setData({ childrenLoaded: true });
      });
  },

  loadChildren() {
    if (this.data.childrenLoading) return Promise.resolve(this.data.children || []);
    this.setData({ childrenLoading: true });
    return fetchChildren()
      .then((list) => {
        const children = Array.isArray(list) ? list : [];
        const today = new Date();
        const normalized = children.map((c) => ({ ...c, age: calcAge(c.birthDate, today) }));
        const savedId = toSafeId(getStorage(STORAGE_KEYS.navCurveSelectedChildId));
        const selectedChildId = pickValidChildId(normalized, savedId);
        this.setData({ children: normalized, selectedChildId, childrenLoaded: true });
        if (selectedChildId) setStorage(STORAGE_KEYS.navCurveSelectedChildId, selectedChildId);
        else removeStorage(STORAGE_KEYS.navCurveSelectedChildId);
        this.applySelectedChild();
        if (selectedChildId) {
          this.loadPersistenceDays(selectedChildId);
          this.loadCurve(selectedChildId);
          this.loadStatusDistribution(selectedChildId);
        }
        return normalized;
      })
      .catch(() => {
        this.setData({ children: [], selectedChildId: 0, childrenLoaded: true });
        this.applySelectedChild();
        this.setData({ chartEmpty: true });
        if (chartInstance) chartInstance.clear();
        return [];
      })
      .finally(() => {
        this.setData({ childrenLoading: false });
      });
  },

  applySelectedChild() {
    const childId = Number(this.data.selectedChildId || 0);
    const selected = (this.data.children || []).find((c) => Number(c.id) === childId) || null;
    if (!selected) {
      this.setData({ greetText: "您好", progressName: "" });
      return;
    }
    const identity = selected.parentIdentity ? String(selected.parentIdentity) : "妈妈";
    const name = `${selected.nickname}${identity}`;
    this.setData({
      greetText: `${name}，您好`,
      progressName: name,
    });
  },

  toggleChildMenu() {
    this.setData({ menuOpen: !this.data.menuOpen });
  },

  closeChildMenu() {
    this.setData({ menuOpen: false });
  },

  onPickChild(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) return;
    this.setData({ selectedChildId: id, menuOpen: false });
    setStorage(STORAGE_KEYS.navCurveSelectedChildId, id);
    this.applySelectedChild();
    this.loadPersistenceDays(id);
    this.loadCurve(id);
    this.loadStatusDistribution(id);
  },

  onAddChild() {
    this.setData({ menuOpen: false });
    wx.navigateTo({ url: "/pages/me/children?action=add" });
  },

  onPickDateRange() {
    const options = [
      { label: "近7天", days: 7 },
      { label: "近30天", days: 30 },
      { label: "近90天", days: 90 },
      { label: "自定义…", days: 0, custom: true },
    ];

    wx.showActionSheet({
      itemList: options.map((o) => o.label),
      itemColor: "#1f2937",
      success: (res) => {
        const idx = Number(res.tapIndex);
        if (!Number.isFinite(idx) || idx < 0 || idx >= options.length) return;
        const picked = options[idx];
        if (picked.custom) {
          this.openCustomRangePicker();
          return;
        }
        const today = new Date();
        const dateTo = formatDateYmd(today);
        const dateFrom = formatDateYmd(new Date(today.getTime() - picked.days * 24 * 3600 * 1000));
        this.setData({
          dateFrom,
          dateTo,
          dateFromText: toMmDd(dateFrom),
          dateToText: toMmDd(dateTo),
          rangeLabel: picked.label,
          rangeDays: picked.days,
        });
        const childId = Number(this.data.selectedChildId || 0);
        if (childId) {
          this.loadCurve(childId);
          this.loadStatusDistribution(childId);
        }
      },
      fail: () => {},
      complete: () => {},
    });
  },

  openCustomRangePicker() {
    const today = String(this.data.today || "") || formatDateYmd(new Date());
    const currentFrom = String(this.data.dateFrom || "") || today;
    const currentTo = String(this.data.dateTo || "") || today;
    const min = String(this.data.customMinDate || "") || "2020-01-01";
    const { from, to } = clampDateRange(currentFrom, currentTo, today, min, "to");
    this.setData({
      customRangeOpen: true,
      customFromDraft: from,
      customToDraft: to,
      customRangeDays: getDaysDiffInclusive(from, to),
    });
  },

  closeCustomRangePicker() {
    this.setData({ customRangeOpen: false });
  },

  onCustomFromChange(e) {
    const nextFrom = String(e?.detail?.value || "").trim();
    const today = String(this.data.today || "") || formatDateYmd(new Date());
    const to = String(this.data.customToDraft || "").trim() || today;
    const min = String(this.data.customMinDate || "") || "2020-01-01";
    const { from, to: nextTo, clamped } = clampDateRange(nextFrom, to, today, min, "from");
    this.setData({
      customFromDraft: from,
      customToDraft: nextTo,
      customRangeDays: getDaysDiffInclusive(from, nextTo),
    });
    if (clamped) wx.showToast({ title: `最多${MAX_RANGE_DAYS}天，已自动调整`, icon: "none" });
  },

  onCustomToChange(e) {
    const nextTo = String(e?.detail?.value || "").trim();
    const today = String(this.data.today || "") || formatDateYmd(new Date());
    const from = String(this.data.customFromDraft || "").trim() || nextTo || today;
    const min = String(this.data.customMinDate || "") || "2020-01-01";
    const { from: nextFrom, to, clamped } = clampDateRange(from, nextTo, today, min, "to");
    this.setData({
      customFromDraft: nextFrom,
      customToDraft: to,
      customRangeDays: getDaysDiffInclusive(nextFrom, to),
    });
    if (clamped) wx.showToast({ title: `最多${MAX_RANGE_DAYS}天，已自动调整`, icon: "none" });
  },

  onCustomRangeConfirm() {
    const today = String(this.data.today || "") || formatDateYmd(new Date());
    const fromDraft = String(this.data.customFromDraft || "").trim() || today;
    const toDraft = String(this.data.customToDraft || "").trim() || today;
    const min = String(this.data.customMinDate || "") || "2020-01-01";
    const { from, to, clamped } = clampDateRange(fromDraft, toDraft, today, min, "to");
    if (clamped) wx.showToast({ title: `最多${MAX_RANGE_DAYS}天，已自动调整`, icon: "none" });

    this.setData({
      dateFrom: from,
      dateTo: to,
      dateFromText: toMmDd(from),
      dateToText: toMmDd(to),
      rangeLabel: `${toMmDd(from)}-${toMmDd(to)}`,
      rangeDays: getDaysDiffInclusive(from, to),
      customRangeOpen: false,
      customFromDraft: from,
      customToDraft: to,
      customRangeDays: getDaysDiffInclusive(from, to),
    });

    const childId = Number(this.data.selectedChildId || 0);
    if (childId) {
      this.loadCurve(childId);
      this.loadStatusDistribution(childId);
    }
  },

  onToggleDimension(e) {
    const code = String(e?.currentTarget?.dataset?.code || "").trim();
    if (!code) return;

    const currentCode = Array.isArray(this.data.visibleDimensionCodes) ? String(this.data.visibleDimensionCodes[0] || "") : "";
    if (currentCode === code) return;

    const nextCodes = [code];
    const dims = DIMENSIONS.map((d) => ({ ...d, _on: d.code === code }));
    this.setData({ visibleDimensionCodes: nextCodes, dimensions: dims });

    const option = buildOption(lastDaysRaw, nextCodes);
    pendingOption = option;
    if (chartInstance) chartInstance.setOption(option, true);
  },

  onEcInit(e) {
    const { canvas, width, height, dpr, canvasDpr } = e.detail || {};
    const pixelRatio = Number.isFinite(Number(dpr)) ? Number(dpr) : Number.isFinite(Number(canvasDpr)) ? Number(canvasDpr) : 1;
    if (!canvas || !Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
      console.warn("[curve] ec-canvas init invalid size:", { width, height, dpr: pixelRatio });
      return;
    }
    const chart = echarts.init(canvas, null, { width, height, devicePixelRatio: pixelRatio });
    canvas.setChart(chart);
    chartInstance = chart;
    if (pendingOption) {
      chart.setOption(pendingOption, true);
    }
  },

  loadCurve(childId) {
    const from = String(this.data.dateFrom || "");
    const to = String(this.data.dateTo || "");
    if (!childId || !from || !to) return;

    this.setData({ chartLoading: true });
    return fetchGrowthReport(childId, from, to)
      .then((report) => {
        const days = Array.isArray(report?.days) ? report.days : [];
        lastDaysRaw = days;
        const visible = Array.isArray(this.data.visibleDimensionCodes) ? this.data.visibleDimensionCodes : DIMENSIONS.map((d) => d.code);
        const option = buildOption(days, visible);
        pendingOption = option;
        this.setData({ chartEmpty: !days.some((d) => Array.isArray(d?.dimensionScores) && d.dimensionScores.length > 0) });
        if (chartInstance) chartInstance.setOption(option, true);
      })
      .catch(() => {
        pendingOption = null;
        lastDaysRaw = [];
        this.setData({ chartEmpty: true });
        if (chartInstance) chartInstance.clear();
      })
      .finally(() => {
        this.setData({ chartLoading: false });
      });
  },

  loadStatusDistribution(childId) {
    const cid = Number(childId || 0);
    const from = String(this.data.dateFrom || "").trim();
    const to = String(this.data.dateTo || "").trim();
    if (!cid || !from || !to) return Promise.resolve();
    if (this.data.statusLoading || this.data.troubleLoading) return Promise.resolve();

    const reqId = ++awarenessReqSeq;
    this.setData({ statusLoading: true, troubleLoading: true });
    const months = listMonthValuesBetween(from, to);
    return Promise.all(months.map((m) => getMonthlyAwareness(cid, m, { toast: false }).catch(() => null)))
      .then((responses) => {
        if (reqId !== awarenessReqSeq) return;
        const days = (Array.isArray(responses) ? responses : []).reduce((acc, r) => {
          const list = Array.isArray(r?.days) ? r.days : [];
          acc.push(...list);
          return acc;
        }, []);
        const { total, items } = buildStatusStats(days, from, to);
        const top = total > 0 ? items.slice(0, 5) : [];
        let topLastIndex = 0;
        for (let i = top.length - 1; i >= 0; i -= 1) {
          if (Number(top[i]?.count || 0) > 0) {
            topLastIndex = i;
            break;
          }
        }
        const topTroubles = buildTopTroubles(days, from, to);
        this.setData({ statusStats: items, statusStatsTop: top, statusStatsTopLastIndex: topLastIndex, statusTotalDays: total, topTroubles });
      })
      .catch(() => {
        if (reqId !== awarenessReqSeq) return;
        this.setData({ statusStats: [], statusStatsTop: [], statusStatsTopLastIndex: 0, statusTotalDays: 0, topTroubles: [] });
      })
      .finally(() => {
        if (reqId !== awarenessReqSeq) return;
        this.setData({ statusLoading: false, troubleLoading: false });
      });
  },

  loadPersistenceDays(childId) {
    const cid = Number(childId || 0);
    if (!cid) return Promise.resolve();
    return fetchAwarenessPersistence(cid, { toast: false })
      .then((res) => {
        const days = Number(res?.persistenceDays || 0);
        this.setData({ persistenceDays: days > 0 ? days : 1 });
      })
      .catch(() => this.setData({ persistenceDays: 1 }));
  },

  goTroubles() {
    const childId = Number(this.data.selectedChildId || 0);
    const from = String(this.data.dateFrom || "").trim();
    const to = String(this.data.dateTo || "").trim();
    if (!childId || !from || !to) {
      wx.showToast({ title: "请先选择孩子", icon: "none" });
      return;
    }
    wx.navigateTo({ url: `/pages/curve/troubles?childId=${childId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}` });
  },

  goTest() {
    const childId = Number(this.data.selectedChildId || 0);
    const selected = (this.data.children || []).find((c) => Number(c.id) === childId) || null;
    if (!childId || !selected) {
      wx.showToast({ title: "请先选择孩子", icon: "none" });
      return;
    }
    const childName = encodeURIComponent(String(selected.nickname || "孩子"));
    wx.navigateTo({ url: `/pages/test/intro?childId=${childId}&childName=${childName}` });
  },
});
