const { fetchChildren } = require("../../services/children");
const { getMonthlyAwareness } = require("../../services/awareness");
const { ensureLoggedIn } = require("../../services/auth");
const { getDailyRecordDetail } = require("../../services/assessments");
const { getDailyParentingStatus, upsertDailyParentingStatus } = require("../../services/parenting-status");
const { listTroubleScenes } = require("../../services/trouble-scenes");
const { getDailyTroubleRecord, upsertDailyTroubleRecord } = require("../../services/daily-troubles");
const { getDailyDiary, upsertDailyDiary } = require("../../services/parenting-diary");
const { uploadDiaryImage } = require("../../services/uploads");
const { setDailySession } = require("../../services/daily-session");
const { fetchRandomQuote } = require("../../services/quotes");
const { formatDateYmd } = require("../../utils/date");
const { getSystemMetrics } = require("../../utils/system");

const OVERLAY_SAFE_TOP_GAP_PX = 12;
const PAGE_PADDING_RPX = 32;
const MOOD_OPTION_SIZE_RPX = 120;

const DEFAULT_QUOTE = "你记下的每个烦躁瞬间\n都是写给孩子未来的一封信\n“看，爸爸妈妈也在学着长大”";
const QUOTE_SCENE_DAILY_OBSERVATION = "每日觉察";
const QUOTE_SCENE_PARENTING_STATUS = "育儿状态";
const QUOTE_SCENE_TROUBLE_ARCHIVE = "烦恼档案";
const QUOTE_SCENE_PARENTING_DIARY = "育儿日记";
const DEFAULT_PARENTING_STATUS_QUOTE = "今天的你是充满能量，还是快没电了\n我们会悄悄记住你的辛苦\n并为你点亮一盏理解的灯";
const DEFAULT_TROUBLE_QUOTE = "把烦恼写下来\n不是为了反复咀嚼\n而是为了找到出口";

function getMenuButtonBottomPx() {
  if (!wx.getMenuButtonBoundingClientRect) return 0;
  const rect = wx.getMenuButtonBoundingClientRect();
  if (!rect) return 0;
  const bottom = Number(rect.bottom || 0);
  if (bottom > 0) return bottom;
  const top = Number(rect.top || 0);
  const height = Number(rect.height || 0);
  const computed = top + height;
  return computed > 0 ? computed : 0;
}

function getOverlaySafeTopPx() {
  const metrics = getSystemMetrics();
  const menuBottom = getMenuButtonBottomPx();
  const navBarHeight = Number(metrics.navBarHeight || 0);
  const base = Math.max(menuBottom, navBarHeight);
  return Math.max(0, base + OVERLAY_SAFE_TOP_GAP_PX);
}

const MOOD_STYLE_BY_ID = {
  disappointed: { emoji: "😞" },
  calm: { emoji: "😌" },
  optimistic: { emoji: "🙂" },
  happy: { emoji: "😄" },
  sad: { emoji: "🥹" },
  worried: { emoji: "😟" },
  helpless: { emoji: "😮‍💨" },
  angry: { emoji: "😡" },
  relieved: { emoji: "🥰" },
  desperate: { emoji: "😭" },
};

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

const STATUS_OPTIONS = [
  { code: "失望", moodId: "disappointed" },
  { code: "平静", moodId: "calm" },
  { code: "乐观", moodId: "optimistic" },
  { code: "难过", moodId: "sad" },
  { code: "无奈", moodId: "helpless" },
  { code: "愤怒", moodId: "angry" },
  { code: "欣慰", moodId: "relieved" },
  { code: "担忧", moodId: "worried" },
  { code: "开心", moodId: "happy" },
  { code: "绝望", moodId: "desperate" },
].map((x) => ({
  ...x,
  emoji: MOOD_STYLE_BY_ID[x.moodId]?.emoji || "🙂",
  imageUrl: STATUS_IMAGE_BY_CODE[x.code] || "",
}));

const DIARY_PROMPTS = [
  "今天虽然很累，但看到孩子的笑容觉得一切都值得。",
  "发火后很后悔，但也许这也是我成长的机会。",
  "孩子的一个小进步，让我惊喜了好久。",
  "不仅是养育孩子，也是在养育那个曾经小小的自己。",
  "放慢脚步，听听孩子心里的话。",
  "最好的爱是陪伴，今天我做到了吗？",
];

const TIMELINE_ICON_BY_ID = {
  parentingStatus: "/assets/timeline/status.svg",
  troubles: "/assets/timeline/troubles.svg",
  mirror: "/assets/timeline/mirror.svg",
  diary: "/assets/timeline/diary.svg",
  expert: "/assets/timeline/expert.svg",
};

function normalizeText(value) {
  return safeText(value).replace(/\r\n/g, "\n");
}

function findStatusOption(code) {
  const value = safeText(code).trim();
  return STATUS_OPTIONS.find((o) => o.code === value) || STATUS_OPTIONS[0];
}

function getOrbitRadiusPercent() {
  const metrics = getSystemMetrics();
  const windowWidth = Number(metrics.windowWidth || 0);
  if (!windowWidth) return 42;
  const rpxToPx = windowWidth / 750;
  const orbitSizePx = windowWidth - PAGE_PADDING_RPX * rpxToPx * 2;
  const optionSizePx = MOOD_OPTION_SIZE_RPX * rpxToPx;
  if (orbitSizePx <= 0) return 42;
  const radius = 50 - (optionSizePx / 2 / orbitSizePx) * 100;
  return Math.min(50, Math.max(0, Number(radius.toFixed(2))));
}

function buildOrbitOptions(options) {
  const list = Array.isArray(options) ? options : [];
  const total = list.length || 1;
  const radius = getOrbitRadiusPercent();
  return list.map((mood, index) => {
    const angleDeg = index * (360 / total) - 90;
    const angleRad = (angleDeg * Math.PI) / 180;
    const x = 50 + radius * Math.cos(angleRad);
    const y = 50 + radius * Math.sin(angleRad);
    return { ...mood, left: Number(x.toFixed(2)), top: Number(y.toFixed(2)) };
  });
}

function safeText(value) {
  return value == null ? "" : String(value);
}

function applySelectedFlag(items, selectedIds) {
  const list = Array.isArray(items) ? items : [];
  const ids = Array.isArray(selectedIds) ? selectedIds.map((x) => safeText(x).trim()).filter(Boolean) : [];
  const selectedSet = new Set(ids);
  return list.map((item) => {
    const id = safeText(item?.id).trim();
    return { ...item, selected: selectedSet.has(id) };
  });
}

function toDateText(ymd) {
  const raw = safeText(ymd).trim();
  if (!raw) return "";
  const d = new Date(`${raw}T00:00:00`);
  if (Number.isNaN(d.getTime())) return raw.replace(/-/g, "/");
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  const w = weekdays[d.getDay()] || "";
  return `${yyyy}/${mm}/${dd} ${w}`;
}

function toDateSlash(ymd) {
  const raw = safeText(ymd).trim();
  if (!raw) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw.replace(/-/g, "/");
  return raw.replace(/-/g, "/");
}

function toMonthValue(ymd) {
  const raw = safeText(ymd).trim();
  if (!raw || raw.length < 7) return "";
  return raw.slice(0, 7);
}

function isToday(ymd) {
  const today = formatDateYmd(new Date());
  return safeText(ymd).trim() === today;
}

function clampDesc(value, maxLen) {
  const text = safeText(value).trim();
  if (!text) return "";
  const n = Number(maxLen) > 0 ? Number(maxLen) : 60;
  return text.length > n ? `${text.slice(0, n)}…` : text;
}

function toAnswerMap(answerViews) {
  const map = {};
  (answerViews || []).forEach((a) => {
    const qid = a && a.questionId != null ? String(a.questionId) : "";
    if (!qid) return;
    map[qid] = Array.isArray(a.optionIds) ? a.optionIds : [];
  });
  return map;
}

function buildItems(record, date) {
  const statusCode = safeText(record.parentingStatusCode).trim();
  const statusDone = Boolean(statusCode);
  const statusImageUrl = statusCode && STATUS_IMAGE_BY_CODE[statusCode] ? STATUS_IMAGE_BY_CODE[statusCode] : "";

  const troubleScenes = Array.isArray(record.troubleScenes) ? record.troubleScenes : [];
  const troubleNames = troubleScenes.map((s) => safeText(s && s.name).trim()).filter(Boolean);
  const troubleDone = troubleNames.length > 0;

  const assessmentId = Number(record.assessmentId || 0);
  const assessmentDone = assessmentId > 0;

  const diaryContent = safeText(record.diaryContent).trim();
  const diaryDone = Boolean(diaryContent) || Boolean(safeText(record.diaryImageUrl).trim());

  const aiSummary = safeText(record.aiSummary).trim();
  const expertDone = Boolean(aiSummary);

  const canStartAssessment = isToday(date);

  const items = [
    {
      id: "parentingStatus",
      title: "育儿状态",
      done: statusDone,
      statusImageUrl,
      iconImageUrl: statusImageUrl || TIMELINE_ICON_BY_ID.parentingStatus,
      desc: statusDone ? `已记录：${statusCode}` : "今天的你是温柔耐心的爸妈，还是被气到想“重启系统”？",
      isLast: false,
    },
    {
      id: "troubles",
      title: "烦恼存档",
      done: troubleDone,
      iconImageUrl: TIMELINE_ICON_BY_ID.troubles,
      desc: troubleDone ? `已记录：${troubleNames.join("、")}` : "拖拉磨蹭，情绪失控，隔代教育矛盾不断",
      isLast: false,
    },
    {
      id: "mirror",
      title: "行为镜子",
      done: assessmentDone,
      iconImageUrl: TIMELINE_ICON_BY_ID.mirror,
      desc: assessmentDone
        ? "已完成每日自测，点击查看答题与建议"
        : canStartAssessment
          ? "一条建议，照亮明天的方向。我们不教你应该怎样，只陪你一起发现“原来我还可以这样”"
          : "每日自测仅支持当天完成",
      isLast: false,
    },
    {
      id: "diary",
      title: "育儿日记",
      done: diaryDone,
      iconImageUrl: TIMELINE_ICON_BY_ID.diary,
      desc: diaryDone
        ? clampDesc(diaryContent || "已记录日记配图", 60)
        : "别一个人扛，写下来，不是抱怨，而是一次自我梳理，也可能是改变的起点",
      isLast: false,
    },
    {
      id: "expert",
      title: "继续深度咨询",
      done: expertDone,
      iconImageUrl: TIMELINE_ICON_BY_ID.expert,
      desc: expertDone ? clampDesc(aiSummary, 60) : "点击进入马上沟通，继续深度咨询",
      isLast: true,
    },
  ];

  return items;
}

Page({
  data: {
    childId: 0,
    childName: "",
    greetText: "",
    date: "",
    dateSlash: "",
    dateText: "",
    title: "我的觉察",
    quote: DEFAULT_QUOTE,
    overlaySafeTopPx: 0,
    statusQuote: DEFAULT_PARENTING_STATUS_QUOTE,
    troubleQuote: DEFAULT_TROUBLE_QUOTE,
    loading: false,
    timelineHasData: false,
    record: {
      parentingStatusCode: "",
      moodId: "",
      troubleScenes: [],
      assessmentId: 0,
      aiSummary: "",
      diaryContent: "",
      diaryImageUrl: "",
    },
    items: [],

    // sheets
    showStatusSheet: false,
    statusOptions: STATUS_OPTIONS,
    statusDraftCode: "",
    statusSelected: STATUS_OPTIONS[0],
    statusOrbitOptions: buildOrbitOptions(STATUS_OPTIONS),
    statusSaving: false,

    showTroubleSheet: false,
    troubleLoading: false,
    troubleSaving: false,
    troubleScenes: [],
    troubleDraftIds: [],

    showDiarySheet: false,
    diarySaving: false,
    diaryDraftContent: "",
    diaryDraftImageUrl: "",
    diaryDraftLength: 0,
    diaryPromptIdx: 0,
    diaryPromptText: DIARY_PROMPTS[0],
    diaryPromptLoading: false,
    diaryCanSubmit: false,
  },
  onLoad(query) {
    const childId = Number(query.childId || 0);
    const date = String(query.date || "");
    const open = safeText(query.open).trim();
    if (!childId || !date) {
      wx.showToast({ title: "参数错误", icon: "none" });
      wx.navigateBack();
      return;
    }
    this.setData({
      childId,
      date,
      dateSlash: toDateSlash(date),
      dateText: toDateText(date),
      childName: "孩子",
      greetText: "",
      title: "我的觉察",
      quote: DEFAULT_QUOTE,
      overlaySafeTopPx: getOverlaySafeTopPx(),
      statusQuote: DEFAULT_PARENTING_STATUS_QUOTE,
      troubleQuote: DEFAULT_TROUBLE_QUOTE,
    });

    const initAfterLogin = () => {
      this.loadChildName();
      this.loadQuote({ skipEnsure: true });
      this.loadRecord({ skipEnsure: true });
      if (open === "status") this.openStatusSheet();
    };

    ensureLoggedIn()
      .then(initAfterLogin)
      .catch(() => {
        if (open === "status") this.openStatusSheet();
        wx.showToast({ title: "登录失败", icon: "none" });
      });
  },
  onShow() {
    if (this.data.childId && this.data.date) {
      this.loadRecord();
    }
  },
  onBack() {
    wx.navigateBack();
  },

  loadChildName() {
    const childId = Number(this.data.childId || 0);
    if (!childId) return;
    fetchChildren()
      .then((children) => {
        const list = Array.isArray(children) ? children : [];
        const child = list.find((c) => Number(c.id) === childId) || null;
        if (!child) return;
        const nickname = safeText(child.nickname).trim() || "孩子";
        const parentIdentity = safeText(child.parentIdentity).trim() || "妈妈";
        this.setData({ childName: nickname, greetText: `${nickname}${parentIdentity}，您好` });
      })
      .catch(() => {});
  },

  loadQuote(options) {
    const childId = Number(this.data.childId || 0);
    if (!childId) return Promise.resolve();

    const fetchQuote = () => fetchRandomQuote(childId, QUOTE_SCENE_DAILY_OBSERVATION, { toast: false });
    const task = options?.skipEnsure ? fetchQuote() : ensureLoggedIn().then(fetchQuote);

    return task
      .then((content) => {
        const quote = normalizeText(content).trim();
        if (!quote) return;
        this.setData({ quote });
      })
      .catch(() => {});
  },

  loadRecord(options) {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    const month = toMonthValue(date);
    if (!childId || !date || !month) return;
    if (this.data.loading) return;
    this.setData({ loading: true });

    const fetchMonthly = () => getMonthlyAwareness(childId, month);
    const task = options?.skipEnsure ? fetchMonthly() : ensureLoggedIn().then(fetchMonthly);

    return task
      .then((monthly) => {
        const days = Array.isArray(monthly?.days) ? monthly.days : [];
        const day = days.find((d) => safeText(d && d.recordDate).trim() === date) || null;

        const parentingStatusCode = safeText(day && day.parentingStatusCode).trim();
        const moodId = safeText(day && day.parentingStatusMoodId).trim();

        const troubleScenes = Array.isArray(day?.troubleScenes) ? day.troubleScenes : [];
        const assessmentId = Number(day?.assessment?.assessmentId || 0);
        const aiSummary = safeText(day?.assessment?.aiSummary).trim();

        const diaryContent = safeText(day?.diary?.content).trim();
        const diaryImageUrl = safeText(day?.diary?.imageUrl).trim();

        const coverUrl = diaryImageUrl || "";

        const record = {
          parentingStatusCode,
          moodId,
          troubleScenes,
          assessmentId,
          aiSummary,
          diaryContent,
          diaryImageUrl,
        };

        const items = buildItems(record, date);
        this.setData({
          record,
          items,
          timelineHasData: items.some((item) => item.done),
        });
      })
      .catch(() => {
        wx.showToast({ title: "加载失败", icon: "none" });
      })
      .finally(() => this.setData({ loading: false }));
  },

  onTapItem(e) {
    const id = safeText(e?.currentTarget?.dataset?.id).trim();
    if (!id) return;
    if (id === "parentingStatus") {
      this.openStatusSheet();
      return;
    }
    if (id === "troubles") {
      this.openTroubleSheet();
      return;
    }
    if (id === "mirror") {
      this.openMirror();
      return;
    }
    if (id === "diary") {
      this.openDiarySheet();
      return;
    }
    if (id === "expert") {
      this.openExpert();
    }
  },

  closeSheets() {
    this.setData({ showStatusSheet: false, showTroubleSheet: false, showDiarySheet: false });
  },

  openStatusSheet() {
    const childId = Number(this.data.childId || 0);
    if (!childId) return;
    const date = safeText(this.data.date).trim();
    if (!date) return;
    const current = safeText(this.data.record?.parentingStatusCode).trim();
    const initial = current || "开心";
    const selected = findStatusOption(initial);
    this.setData({
      showStatusSheet: true,
      statusDraftCode: selected.code,
      statusSelected: selected,
      statusOrbitOptions: buildOrbitOptions(STATUS_OPTIONS),
      statusQuote: DEFAULT_PARENTING_STATUS_QUOTE,
    });

    ensureLoggedIn()
      .then(() => Promise.all([getDailyParentingStatus(childId, date), fetchRandomQuote(childId, QUOTE_SCENE_PARENTING_STATUS, { toast: false })]))
      .then(([res, quote]) => {
        const status = safeText(res?.statusCode).trim();
        if (status) {
          const opt = findStatusOption(status);
          this.setData({ statusDraftCode: opt.code, statusSelected: opt });
        }
        const text = normalizeText(quote).trim();
        if (text) this.setData({ statusQuote: text });
      })
      .catch(() => {});
  },

  onPickStatus(e) {
    const code = safeText(e?.currentTarget?.dataset?.code).trim();
    if (!code) return;
    const opt = findStatusOption(code);
    this.setData({ statusDraftCode: opt.code, statusSelected: opt });
  },

  onSaveStatus() {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    const statusCode = safeText(this.data.statusDraftCode).trim();
    if (!childId || !date) return;
    if (!statusCode) {
      wx.showToast({ title: "请选择育儿状态", icon: "none" });
      return;
    }
    if (date > formatDateYmd(new Date())) {
      wx.showToast({ title: "记录日期不能是未来时间", icon: "none" });
      return;
    }
    if (this.data.statusSaving) return;
    this.setData({ statusSaving: true });
    ensureLoggedIn()
      .then(() => upsertDailyParentingStatus({ childId, recordDate: date, statusCode }))
      .then(() => {
        this.closeSheets();
        wx.showToast({ title: "已保存", icon: "success" });
        this.loadRecord();
      })
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "保存失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => this.setData({ statusSaving: false }));
  },

  openTroubleSheet() {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    if (!childId || !date) return;
    if (this.data.troubleLoading) return;
    const fromMonthly = Array.isArray(this.data.record?.troubleScenes) ? this.data.record.troubleScenes : [];
    const monthlyIds = fromMonthly.map((s) => safeText(s?.id).trim()).filter(Boolean);
    this.setData({
      showTroubleSheet: true,
      troubleDraftIds: monthlyIds,
      troubleLoading: true,
      troubleQuote: DEFAULT_TROUBLE_QUOTE,
    });
    ensureLoggedIn()
      .then(() =>
        Promise.all([
          listTroubleScenes(),
          getDailyTroubleRecord(childId, date),
          fetchRandomQuote(childId, QUOTE_SCENE_TROUBLE_ARCHIVE, { toast: false }),
        ])
      )
      .then(([scenes, record, quote]) => {
        const list = (Array.isArray(scenes) ? scenes : []).map((s) => {
          const id = safeText(s?.id).trim();
          const name = safeText(s?.name).trim();
          return {
            ...s,
            id,
            name,
            shortName: name.slice(0, 1),
          };
        });
        const selected = Array.isArray(record?.scenes) ? record.scenes.map((s) => safeText(s?.id).trim()).filter(Boolean) : monthlyIds;
        const q = normalizeText(quote).trim();
        this.setData({
          troubleScenes: applySelectedFlag(list, selected),
          troubleDraftIds: selected,
          troubleQuote: q || DEFAULT_TROUBLE_QUOTE,
        });
      })
      .catch(() => {
        if (!this.data.troubleScenes || this.data.troubleScenes.length === 0) this.setData({ troubleScenes: [] });
      })
      .finally(() => this.setData({ troubleLoading: false }));
  },

  onToggleTrouble(e) {
    const id = safeText(e?.currentTarget?.dataset?.id).trim();
    if (!id) return;
    const selected = Array.isArray(this.data.troubleDraftIds) ? this.data.troubleDraftIds.slice() : [];
    const idx = selected.indexOf(id);
    if (idx >= 0) selected.splice(idx, 1);
    else selected.push(id);
    this.setData({ troubleDraftIds: selected, troubleScenes: applySelectedFlag(this.data.troubleScenes, selected) });
  },

  onSaveTroubles() {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    const rawIds = Array.isArray(this.data.troubleDraftIds) ? this.data.troubleDraftIds.map((v) => safeText(v).trim()).filter(Boolean) : [];
    if (!childId || !date) return;
    if (!rawIds.length) {
      wx.showToast({ title: "请至少选择 1 个烦恼场景", icon: "none" });
      return;
    }
    const ids = rawIds.map((v) => Number(v)).filter((v) => v > 0);
    if (date > formatDateYmd(new Date())) {
      wx.showToast({ title: "记录日期不能是未来时间", icon: "none" });
      return;
    }
    if (this.data.troubleSaving) return;
    this.setData({ troubleSaving: true });
    ensureLoggedIn()
      .then(() => upsertDailyTroubleRecord({ childId, recordDate: date, sceneIds: ids }))
      .then(() => {
        this.closeSheets();
        wx.showToast({ title: "已保存", icon: "success" });
        this.loadRecord();
      })
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "保存失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => this.setData({ troubleSaving: false }));
  },

  openDiarySheet() {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    if (!childId || !date) return;
    const draftContent = normalizeText(this.data.record?.diaryContent).trim();
    const draftImageUrl = safeText(this.data.record?.diaryImageUrl).trim();
    this.setData({
      showDiarySheet: true,
      diaryDraftContent: draftContent,
      diaryDraftImageUrl: draftImageUrl,
      diaryDraftLength: draftContent.length,
      diaryPromptText: DIARY_PROMPTS[this.data.diaryPromptIdx || 0] || DIARY_PROMPTS[0],
      diaryPromptLoading: false,
    });
    this.syncDiaryCanSubmit();
    ensureLoggedIn()
      .then(() => {
        this.setData({ diaryPromptLoading: true });
        return Promise.all([getDailyDiary(childId, date), fetchRandomQuote(childId, QUOTE_SCENE_PARENTING_DIARY, { toast: false })]);
      })
      .then(([res, quote]) => {
        if (!res) return;
        const content = normalizeText(res?.content).trim();
        const imageUrl = safeText(res?.imageUrl).trim();
        if (content || imageUrl) {
          this.setData({
            diaryDraftContent: content,
            diaryDraftImageUrl: imageUrl,
            diaryDraftLength: content.length,
          });
          this.syncDiaryCanSubmit();
        }

        const prompt = normalizeText(quote).trim();
        if (prompt) this.setData({ diaryPromptText: prompt });
      })
      .catch(() => {})
      .finally(() => this.setData({ diaryPromptLoading: false }));
  },

  onDiaryInput(e) {
    const value = normalizeText(e?.detail?.value);
    this.setData({ diaryDraftContent: value, diaryDraftLength: value.length });
    this.syncDiaryCanSubmit();
  },

  syncDiaryCanSubmit() {
    const content = safeText(this.data.diaryDraftContent).trim();
    const hasImage = Boolean(safeText(this.data.diaryDraftImageUrl).trim());
    this.setData({ diaryCanSubmit: Boolean(content) || hasImage });
  },

  onUseDiaryPrompt() {
    const current = normalizeText(this.data.diaryPromptText).trim();
    const fallback = DIARY_PROMPTS[Number(this.data.diaryPromptIdx || 0)] || DIARY_PROMPTS[0];
    const prompt = normalizeText(current || fallback).trim();
    if (!prompt) return;

    const maxLen = 200;
    const existing = normalizeText(this.data.diaryDraftContent);
    const base = existing || "";
    const sep = base && !base.endsWith("\n") ? "\n" : "";

    if (base.length >= maxLen) {
      wx.showToast({ title: "字数已达上限", icon: "none" });
      return;
    }

    let next = `${base}${sep}${prompt}`;
    if (next.length > maxLen) next = next.slice(0, maxLen);

    this.setData({ diaryDraftContent: next, diaryDraftLength: next.length });
    this.syncDiaryCanSubmit();
  },

  onNextDiaryPrompt() {
    const childId = Number(this.data.childId || 0);
    if (!childId) return;
    if (this.data.diaryPromptLoading) return;

    const nextFallback = () => {
      const next = (Number(this.data.diaryPromptIdx || 0) + 1) % DIARY_PROMPTS.length;
      const prompt = DIARY_PROMPTS[next] || DIARY_PROMPTS[0];
      this.setData({ diaryPromptIdx: next, diaryPromptText: prompt });
    };

    this.setData({ diaryPromptLoading: true });
    ensureLoggedIn()
      .then(() => fetchRandomQuote(childId, QUOTE_SCENE_PARENTING_DIARY, { toast: false }))
      .then((content) => {
        const prompt = normalizeText(content).trim();
        if (prompt) {
          this.setData({ diaryPromptText: prompt });
          return;
        }
        nextFallback();
      })
      .catch(nextFallback)
      .finally(() => this.setData({ diaryPromptLoading: false }));
  },

  onPickDiaryImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const path = res?.tempFilePaths && res.tempFilePaths[0] ? String(res.tempFilePaths[0]) : "";
        if (!path) return;
        wx.showLoading({ title: "上传中…" });
        ensureLoggedIn()
          .then(() => uploadDiaryImage(path))
          .then((url) => {
            this.setData({ diaryDraftImageUrl: safeText(url).trim() });
            this.syncDiaryCanSubmit();
          })
          .catch((err) => {
            const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "上传失败";
            wx.showToast({ title: message, icon: "none" });
          })
          .finally(() => wx.hideLoading());
      },
    });
  },

  onRemoveDiaryImage() {
    this.setData({ diaryDraftImageUrl: "" });
    this.syncDiaryCanSubmit();
  },

  onPreviewDiaryImage() {
    const url = safeText(this.data.diaryDraftImageUrl).trim();
    if (!url) return;
    wx.previewImage({ urls: [url] });
  },

  onSaveDiary() {
    const childId = Number(this.data.childId || 0);
    const date = safeText(this.data.date).trim();
    const content = safeText(this.data.diaryDraftContent).trim();
    const imageUrl = safeText(this.data.diaryDraftImageUrl).trim();
    if (!childId || !date) return;
    if (!content && !imageUrl) return;
    if (date > formatDateYmd(new Date())) {
      wx.showToast({ title: "记录日期不能是未来时间", icon: "none" });
      return;
    }
    if (this.data.diarySaving) return;
    this.setData({ diarySaving: true });
    ensureLoggedIn()
      .then(() => upsertDailyDiary({ childId, recordDate: date, content, imageUrl: imageUrl || null }))
      .then(() => {
        this.closeSheets();
        wx.showToast({ title: "已保存", icon: "success" });
        this.loadRecord();
      })
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "保存失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => this.setData({ diarySaving: false }));
  },

  openMirror() {
    const assessmentId = Number(this.data.record?.assessmentId || 0);
    if (assessmentId > 0) {
      this.openAssessmentHistory(assessmentId);
      return;
    }
    if (!isToday(this.data.date)) {
      wx.showToast({ title: "每日自测仅支持当天完成", icon: "none" });
      return;
    }
    const childId = Number(this.data.childId || 0);
    if (!childId) return;
    wx.navigateTo({ url: `/pages/test/intro?childId=${childId}&childName=${encodeURIComponent(this.data.childName)}` });
  },

  openExpert() {
    wx.switchTab({ url: "/pages/chat/index" });
  },

  openAssessmentHistory(assessmentId) {
    const id = Number(assessmentId || 0);
    if (!id) return;
    wx.showLoading({ title: "加载中…" });
    ensureLoggedIn()
      .then(() => getDailyRecordDetail(id))
      .then((detail) => {
        const answers = toAnswerMap(detail.answers);
        setDailySession({
          sessionId: `history-${id}`,
          childId: detail.childId || 0,
          childName: detail.childName || "（未知）",
          items: detail.items || [],
          answers,
          submitResult: null,
          assessmentId: detail.assessmentId || id,
          aiSummary: detail.aiSummary || safeText(this.data.record?.aiSummary).trim() || "",
        });
        wx.navigateTo({ url: "/pages/test/result?mode=history" });
      })
      .catch(() => wx.showToast({ title: "加载失败", icon: "none" }))
      .finally(() => wx.hideLoading());
  },
});
