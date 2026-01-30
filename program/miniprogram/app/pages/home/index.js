const { fetchChildren } = require("../../services/children");
const { getMonthlyAwareness } = require("../../services/awareness");
const { getDailyRecordDetail } = require("../../services/assessments");
const { listBanners } = require("../../services/banners");
const { ensureLoggedIn, getCachedMe } = require("../../services/auth");
const { STORAGE_KEYS } = require("../../services/config");
const { getStorage, removeStorage, setStorage } = require("../../services/storage");
const { formatDateYmd, calcAge } = require("../../utils/date");
const { getSystemMetrics } = require("../../utils/system");

const MIN_MONTH = "2025-06";

const DEFAULT_FOOT_TITLE = "你忘记觉察了哦";
const DEFAULT_FOOT_CONTENT = "你没有收获到任何建议。";

const STATUS_IMAGE_BY_CODE = {
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

function parseMonthValue(value) {
  const v = String(value || "").trim();
  const m = /^(\d{4})-(\d{1,2})$/.exec(v);
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

function normalizeMonthValue(value) {
  const p = parseMonthValue(value);
  if (!p) return "";
  return formatMonthValue(p.year, p.month);
}

function toPickerStartDate(monthValue) {
  const v = normalizeMonthValue(monthValue);
  return v ? `${v}-01` : "";
}

function toPickerEndDate(monthValue) {
  const p = parseMonthValue(monthValue);
  if (!p) return "";
  const lastDay = new Date(p.year, p.month, 0).getDate();
  return `${formatMonthValue(p.year, p.month)}-${String(lastDay).padStart(2, "0")}`;
}

function compareMonthValue(a, b) {
  const pa = parseMonthValue(a);
  const pb = parseMonthValue(b);
  if (!pa || !pb) return 0;
  if (pa.year !== pb.year) return pa.year - pb.year;
  return pa.month - pb.month;
}

function normalizeBanners(list) {
  const items = Array.isArray(list) ? list : [];
  return items
    .map((b) => ({
      id: b && b.id != null ? b.id : "",
      title: b && b.title != null ? String(b.title) : "",
      imageUrl:
        (b && (b.imageUrl || b.coverUrl || b.cover || b.url)) != null ? String(b.imageUrl || b.coverUrl || b.cover || b.url) : "",
    }))
    .filter((b) => b.id !== "" && b.imageUrl);
}

function toMonthValue(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  return `${yyyy}-${mm}`;
}

function toMonthText(date) {
  return `${date.getFullYear()}年 ${date.getMonth() + 1}月`;
}

function dayOfWeekText(date) {
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  return weekdays[date.getDay()] || "";
}

function addDays(date, deltaDays) {
  const d = date instanceof Date ? new Date(date.getTime()) : new Date();
  d.setDate(d.getDate() + Number(deltaDays || 0));
  return d;
}

function toRelativeDayText(recordDate, dayNumber, todayYmd, yesterdayYmd) {
  if (recordDate && todayYmd && recordDate === todayYmd) return "今天";
  if (recordDate && yesterdayYmd && recordDate === yesterdayYmd) return "昨天";
  return String(dayNumber || 0).padStart(2, "0");
}

function isRelativeDay(recordDate, todayYmd, yesterdayYmd) {
  return Boolean(recordDate && ((todayYmd && recordDate === todayYmd) || (yesterdayYmd && recordDate === yesterdayYmd)));
}

function clampMonthValue(value, minMonth, maxMonth) {
  const v = normalizeMonthValue(value);
  const min = normalizeMonthValue(minMonth);
  const max = normalizeMonthValue(maxMonth);
  if (min && compareMonthValue(v, min) < 0) return min;
  if (max && compareMonthValue(v, max) > 0) return max;
  return v;
}

function isMonthBefore(value, bound) {
  const v = normalizeMonthValue(value);
  const b = normalizeMonthValue(bound);
  if (!v || !b) return false;
  return compareMonthValue(v, b) < 0;
}

const MOOD_STYLE_BY_ID = {
  disappointed: { emoji: "😞", cls: "mood-gray" },
  calm: { emoji: "😌", cls: "mood-green" },
  optimistic: { emoji: "🙂", cls: "mood-orange" },
  happy: { emoji: "😄", cls: "mood-orange" },
  sad: { emoji: "🥹", cls: "mood-blue" },
  worried: { emoji: "😟", cls: "mood-gray" },
  helpless: { emoji: "😮‍💨", cls: "mood-gray" },
  angry: { emoji: "😡", cls: "mood-red" },
  relieved: { emoji: "🥰", cls: "mood-pink" },
  desperate: { emoji: "😭", cls: "mood-blue" },
};

const STATUS_CODE_BY_MOOD_ID = {
  optimistic: "乐观",
  disappointed: "失望",
  calm: "平静",
  happy: "开心",
  angry: "愤怒",
  worried: "担忧",
  helpless: "无奈",
  relieved: "欣慰",
  desperate: "绝望",
  sad: "难过",
};

function toNonEmptyText(value) {
  const s = value != null ? String(value).trim() : "";
  return s ? s : "";
}

function pickFirstBySortNo(options) {
  const list = Array.isArray(options) ? options : [];
  if (!list.length) return null;
  const sorted = list
    .map((o) => ({
      sortNo: Number(o?.sortNo),
      option: o,
    }))
    .sort((a, b) => {
      const sa = Number.isFinite(a.sortNo) ? a.sortNo : Number.MAX_SAFE_INTEGER;
      const sb = Number.isFinite(b.sortNo) ? b.sortNo : Number.MAX_SAFE_INTEGER;
      return sa - sb;
    });
  return sorted[0]?.option || null;
}

function buildAssessmentPreviewFromDetail(detail) {
  const items = Array.isArray(detail?.items) ? detail.items : [];
  if (!items.length) return null;
  const firstItem =
    items
      .slice()
      .sort((a, b) => Number(a?.displayOrder || 0) - Number(b?.displayOrder || 0))[0] || null;
  if (!firstItem) return null;

  const title = toNonEmptyText(firstItem.content);
  if (!title) return null;

  const firstOption = pickFirstBySortNo(firstItem.options);
  const tip = toNonEmptyText(firstOption?.improvementTip) || toNonEmptyText(detail?.aiSummary) || DEFAULT_FOOT_CONTENT;

  return { title, content: tip };
}

function uniqPositiveNumbers(values) {
  const list = Array.isArray(values) ? values : [];
  const seen = new Set();
  const out = [];
  for (const v of list) {
    const n = Number(v || 0);
    if (!Number.isFinite(n) || n <= 0) continue;
    if (seen.has(n)) continue;
    seen.add(n);
    out.push(n);
  }
  return out;
}

function promisePool(inputs, worker, concurrency) {
  const list = Array.isArray(inputs) ? inputs : [];
  const limit = Math.max(1, Number(concurrency || 1));
  const results = new Array(list.length);
  let cursor = 0;
  let active = 0;

  return new Promise((resolve) => {
    function next() {
      if (cursor >= list.length && active === 0) {
        resolve(results);
        return;
      }
      while (active < limit && cursor < list.length) {
        const idx = cursor++;
        active += 1;
        Promise.resolve()
          .then(() => worker(list[idx], idx))
          .then((res) => {
            results[idx] = res;
          })
          .catch(() => {
            results[idx] = null;
          })
          .finally(() => {
            active -= 1;
            next();
          });
      }
    }
    next();
  });
}

function buildDayListFromMonthlyApi(monthly, childId, today = new Date()) {
  const month = monthly && monthly.month ? String(monthly.month) : "";
  const days = Array.isArray(monthly?.days) ? monthly.days : [];
  if (!month) return [];

  const todayYmd = formatDateYmd(today);
  const yesterdayYmd = formatDateYmd(addDays(today, -1));
  const thisMonth = toMonthValue(today);
  const isCurrentMonth = month === thisMonth;

  const normalized = days
    .map((d) => {
      const recordDate = d && d.recordDate ? String(d.recordDate) : "";
      if (!recordDate) return null;
      if (isCurrentMonth && recordDate > todayYmd) return null;

      const dt = new Date(`${recordDate}T00:00:00`);
      const day = dt.getDate();

      const statusCode = d.parentingStatusCode ? String(d.parentingStatusCode).trim() : "";
      const moodId = d.parentingStatusMoodId ? String(d.parentingStatusMoodId).trim() : "";
      const moodStyle = moodId && MOOD_STYLE_BY_ID[moodId] ? MOOD_STYLE_BY_ID[moodId] : null;
      const statusCodeFromMood = moodId && STATUS_CODE_BY_MOOD_ID[moodId] ? STATUS_CODE_BY_MOOD_ID[moodId] : "";
      const statusKey = STATUS_IMAGE_BY_CODE[statusCode] ? statusCode : statusCodeFromMood;
      const hasStatus = Boolean(statusCode || statusCodeFromMood);
      const moodImageUrl = statusKey && STATUS_IMAGE_BY_CODE[statusKey] ? STATUS_IMAGE_BY_CODE[statusKey] : "";

      const troubleScenes = Array.isArray(d.troubleScenes) ? d.troubleScenes : [];
      const troubleNames = troubleScenes.map((s) => (s && s.name ? String(s.name) : "")).filter(Boolean);

      const assessmentId = d.assessment && d.assessment.assessmentId ? Number(d.assessment.assessmentId) : 0;
      const hasAssessment = Boolean(assessmentId);

      const diaryContent = d.diary && d.diary.content ? String(d.diary.content) : "";
      const diaryImageUrl = d.diary && d.diary.imageUrl ? String(d.diary.imageUrl) : "";
      const diaryText = diaryContent.trim();
      const diaryImageTrim = diaryImageUrl.trim();
      const hasDiaryText = Boolean(diaryText);
      const hasDiary = hasDiaryText || Boolean(diaryImageTrim);

      const isDone =
        hasStatus ||
        troubleNames.length > 0 ||
        Boolean(assessmentId) ||
        hasDiary;

      let title = DEFAULT_FOOT_TITLE;
      if (hasAssessment) {
        title = "已完成自测";
      }

      let content = DEFAULT_FOOT_CONTENT;
      if (hasDiaryText) {
        content = diaryText;
      }

      const imageUrl = diaryImageTrim;

      return {
        id: `obs-${recordDate}`,
        date: recordDate,
        dayText: toRelativeDayText(recordDate, day, todayYmd, yesterdayYmd),
        day,
        weekday: dayOfWeekText(dt),
        isToday: recordDate === todayYmd,
        isRelativeDayText: isRelativeDay(recordDate, todayYmd, yesterdayYmd),
        isDone,
        assessmentId,
        hasAssessment,
        imageUrl: isDone ? imageUrl : "",
        title,
        content,
        titleOn: hasAssessment,
        contentOn: hasDiaryText,
        hasStatus,
        moodEmoji: hasStatus && moodStyle ? moodStyle.emoji : "",
        moodCls: hasStatus && moodStyle ? moodStyle.cls : "",
        moodImageUrl,
      };
    })
    .filter(Boolean);

  // UI shows latest day first
  normalized.sort((a, b) => String(b.date).localeCompare(String(a.date)));
  return normalized;
}

function buildEmptyMonthDayList(targetMonth, today = new Date()) {
  const year = targetMonth.getFullYear();
  const month = targetMonth.getMonth();
  const isCurrentMonth = year === today.getFullYear() && month === today.getMonth();
  const maxDay = isCurrentMonth ? today.getDate() : new Date(year, month + 1, 0).getDate();

  const todayYmd = formatDateYmd(today);
  const yesterdayYmd = formatDateYmd(addDays(today, -1));
  const list = [];
  for (let day = maxDay; day >= 1; day -= 1) {
    const d = new Date(year, month, day);
    const date = formatDateYmd(d);
    list.push({
      id: `obs-${date}`,
      date,
      dayText: toRelativeDayText(date, day, todayYmd, yesterdayYmd),
      day,
      weekday: dayOfWeekText(d),
      isToday: date === todayYmd,
      isRelativeDayText: isRelativeDay(date, todayYmd, yesterdayYmd),
      isDone: false,
      assessmentId: 0,
      hasAssessment: false,
      imageUrl: "",
      title: DEFAULT_FOOT_TITLE,
      content: DEFAULT_FOOT_CONTENT,
      titleOn: false,
      contentOn: false,
      hasStatus: false,
      moodEmoji: "",
      moodCls: "",
      moodImageUrl: "",
    });
  }
  return list;
}

Page({
  data: {
    navBarHeight: 0,
    banners: [],
    bannerIndex: 0,
    menuOpen: false,
    menuPopoverTopPx: 0,
    children: [],
    selectedChildId: 0,
    greetText: "您好",
    greetName: "",
    greetSuffix: "您好",
    avatarText: "妈",
    avatarUrl: "",
    monthText: "",
    monthValue: "",
    minMonth: MIN_MONTH,
    minMonthStart: toPickerStartDate(MIN_MONTH),
    maxMonth: "",
    maxMonthEnd: "",
    dayList: [],
    monthLoading: false,
  },
  onUnload() {
    // Prevent late async results from updating another instance of this page.
    this._assessmentPreviewReqId = (this._assessmentPreviewReqId || 0) + 1;
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 0 });

    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    ensureLoggedIn()
      .then((me) => {
        if (me) this.applyMe(me);
        return this.loadChildren();
      })
      .catch(() => {});
  },
  onLoad() {
    const { navBarHeight } = getSystemMetrics();
    const menuRect = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
    const navHeight = menuRect && Number(menuRect.bottom || 0) > 0 ? Number(menuRect.bottom) : Number(navBarHeight || 0);
    const today = new Date();
    const maxMonth = toMonthValue(today);
    const monthValue = clampMonthValue(toMonthValue(today), MIN_MONTH, maxMonth);
    const parsed = parseMonthValue(monthValue);
    const monthDate = parsed ? new Date(parsed.year, parsed.month - 1, 1) : today;
    this.setData({
      navBarHeight: navHeight,
      menuPopoverTopPx: menuRect ? Math.max(0, Number(menuRect.bottom || 0) + 56) : Math.max(0, Number(navHeight || 0) + 56),
      monthText: toMonthText(monthDate),
      monthValue,
      minMonth: MIN_MONTH,
      minMonthStart: toPickerStartDate(MIN_MONTH),
      maxMonth,
      maxMonthEnd: toPickerEndDate(maxMonth),
      dayList: buildEmptyMonthDayList(monthDate, today),
    });

    const cached = getCachedMe();
    if (cached) this.applyMe(cached);
    this.initPage();
  },

  loadAssessmentPreviews(dayList) {
    const currentList = Array.isArray(dayList) ? dayList : [];
    const assessmentIds = uniqPositiveNumbers(currentList.map((d) => d && d.assessmentId));
    if (!assessmentIds.length) return;

    if (!this._assessmentPreviewCache) this._assessmentPreviewCache = new Map();
    const reqId = (this._assessmentPreviewReqId || 0) + 1;
    this._assessmentPreviewReqId = reqId;

    const toFetch = assessmentIds.filter((id) => !this._assessmentPreviewCache.has(id));
    if (!toFetch.length) {
      this.applyAssessmentPreviewsFromCache();
      return;
    }

    promisePool(
      toFetch,
      (assessmentId) =>
        getDailyRecordDetail(assessmentId, { toast: false })
          .then((detail) => {
            const preview = buildAssessmentPreviewFromDetail(detail);
            return preview ? { assessmentId, preview } : null;
          })
          .catch(() => null),
      4
    ).then((results) => {
      if (this._assessmentPreviewReqId !== reqId) return;
      for (const item of results) {
        if (!item) continue;
        this._assessmentPreviewCache.set(item.assessmentId, item.preview);
      }
      this.applyAssessmentPreviewsFromCache();
    });
  },

  applyAssessmentPreviewsFromCache() {
    if (!this._assessmentPreviewCache || this._assessmentPreviewCache.size === 0) return;
    const list = Array.isArray(this.data.dayList) ? this.data.dayList : [];
    const next = list.map((d) => {
      const assessmentId = Number(d?.assessmentId || 0);
      if (!assessmentId) return d;
      const preview = this._assessmentPreviewCache.get(assessmentId);
      if (!preview) return d;
      return { ...d, title: preview.title };
    });
    this.setData({ dayList: next });
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
        return Promise.all([this.loadChildren(), this.loadBanners()]);
      })
      .catch(() => {});
  },

  loadChildren() {
    return fetchChildren()
      .then((list) => {
        const children = Array.isArray(list) ? list : [];
        this.setChildren(children);
        return children;
      })
      .catch(() => {
        this.setChildren([]);
        return [];
      });
  },

  setChildren(children) {
    const today = new Date();
    const normalized = (Array.isArray(children) ? children : []).map((c) => ({
      ...c,
      age: calcAge(c.birthDate, today),
    }));
    const savedId = toSafeId(getStorage(STORAGE_KEYS.navHomeSelectedChildId));
    const selectedChildId = pickValidChildId(normalized, savedId);

    this.setData({ children: normalized, selectedChildId });
    if (selectedChildId) setStorage(STORAGE_KEYS.navHomeSelectedChildId, selectedChildId);
    else removeStorage(STORAGE_KEYS.navHomeSelectedChildId);
    this.applySelectedChild();
    this.refreshMonthData();
  },

  applySelectedChild() {
    const childId = Number(this.data.selectedChildId || 0);
    const selected = (this.data.children || []).find((c) => Number(c.id) === childId) || null;
    if (!selected) {
      this.setData({ greetText: "您好", greetName: "", greetSuffix: "您好" });
      return;
    }
    const identity = selected.parentIdentity ? String(selected.parentIdentity) : "妈妈";
    const greetName = `${selected.nickname}${identity}`;
    const greetSuffix = "您好";
    this.setData({
      greetText: `${greetName}，${greetSuffix}`,
      greetName,
      greetSuffix,
    });
  },

  loadBanners() {
    return listBanners()
      .then((list) => {
        const banners = normalizeBanners(list);
        this.setData({ banners, bannerIndex: 0 });
      })
      .catch(() => this.setData({ banners: [], bannerIndex: 0 }));
  },
  onBannerTap(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) return;
    wx.navigateTo({ url: `/pages/banner/detail?id=${id}` });
  },
  onBannerChange(e) {
    const idx = e && e.detail ? Number(e.detail.current || 0) : 0;
    const count = Array.isArray(this.data.banners) ? this.data.banners.length : 0;
    if (!count) {
      this.setData({ bannerIndex: 0 });
      return;
    }
    const safe = Math.max(0, Math.min(idx, count - 1));
    if (safe !== this.data.bannerIndex) {
      this.setData({ bannerIndex: safe });
    }
  },

  toggleChildMenu() {
    this.setData({ menuOpen: !this.data.menuOpen });
  },
  onOpenProfile() {
    ensureLoggedIn()
      .then(() => wx.navigateTo({ url: "/pages/me/profile" }))
      .catch(() => {});
  },

  closeChildMenu() {
    this.setData({ menuOpen: false });
  },

  onPickChild(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) return;
    this.setData({ selectedChildId: id, menuOpen: false });
    setStorage(STORAGE_KEYS.navHomeSelectedChildId, id);
    this.applySelectedChild();
    this.refreshMonthData();
  },

  onAddChild() {
    this.setData({ menuOpen: false });
    wx.navigateTo({ url: "/pages/me/children?action=add" });
  },

  onMonthChange(e) {
    const raw = String(e.detail.value || "");
    if (!raw) return;
    const monthValue = clampMonthValue(raw, this.data.minMonth, this.data.maxMonth);
    if (!monthValue) return;
    if (isMonthBefore(raw, this.data.minMonth)) {
      wx.showToast({ title: "最早仅支持 2025年6月", icon: "none" });
    }
    const parts = monthValue.split("-").map((x) => Number(x));
    if (parts.length < 2) return;
    const [yyyy, mm] = parts;
    if (!yyyy || !mm) return;
    const target = new Date(yyyy, mm - 1, 1);
    this.setData({ monthValue, monthText: toMonthText(target) });
    this.refreshMonthData();
  },

  refreshMonthData() {
    const childId = Number(this.data.selectedChildId || 0);
    const monthValue = normalizeMonthValue(this.data.monthValue);
    if (!childId || !monthValue) return;
    if (this.data.monthLoading) return;
    this.setData({ monthLoading: true });

    getMonthlyAwareness(childId, monthValue)
      .then((monthly) => {
        const today = new Date();
        const days = buildDayListFromMonthlyApi(monthly, childId, today);
        if (days.length) {
          this.setData({ dayList: days });
          this.loadAssessmentPreviews(days);
          return;
        }
        const parts = monthValue.split("-").map((x) => Number(x));
        const [yyyy, mm] = parts;
        const monthDate = yyyy && mm ? new Date(yyyy, mm - 1, 1) : today;
        this.setData({ dayList: buildEmptyMonthDayList(monthDate, today) });
      })
      .catch(() => {
        const today = new Date();
        const parts = monthValue.split("-").map((x) => Number(x));
        const [yyyy, mm] = parts;
        const monthDate = yyyy && mm ? new Date(yyyy, mm - 1, 1) : today;
        this.setData({ dayList: buildEmptyMonthDayList(monthDate, today) });
      })
      .finally(() => this.setData({ monthLoading: false }));
  },

  onOpenDay(e) {
    const childId = Number(this.data.selectedChildId || 0);
    const date = String(e.currentTarget.dataset.date || "");
    if (!childId) {
      wx.showToast({ title: "请先添加孩子", icon: "none" });
      return;
    }
    if (!date) return;
    wx.navigateTo({ url: `/pages/home/detail?childId=${childId}&date=${encodeURIComponent(date)}` });
  },
});
