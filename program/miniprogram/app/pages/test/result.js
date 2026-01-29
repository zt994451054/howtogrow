const { fetchAiSummary } = require("../../services/assessments");
const { clearDailySession, getDailySession, setDailySession } = require("../../services/daily-session");
const { getSystemMetrics } = require("../../utils/system");

function normalizeInlineText(value) {
  if (value == null) return "";
  return String(value).replace(/\\n/g, "").trim();
}

function normalizeMultilineText(value) {
  if (value == null) return "";
  return String(value).replace(/\\n/g, "\n").trim();
}

function parseDateInput(value) {
  if (!value) return null;
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  const s = String(value).trim();
  if (!s) return null;

  const ymd = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (ymd) {
    const year = Number(ymd[1]);
    const month = Number(ymd[2]);
    const day = Number(ymd[3]);
    if (Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)) {
      return new Date(year, month - 1, day);
    }
  }

  const dt = new Date(s);
  if (Number.isNaN(dt.getTime())) return null;
  return dt;
}

function formatReviewTitle(date) {
  const dt = parseDateInput(date);
  const safe = dt || new Date();
  const y = safe.getFullYear();
  const m = safe.getMonth() + 1;
  const d = safe.getDate();
  return `${y}年${m}月${d}日自测建议`;
}

function getPageRoute(page) {
  if (!page) return "";
  return String(page.route || page.__route__ || "").trim();
}

function findBackDeltaToRoute(targetRoute) {
  const pages = typeof getCurrentPages === "function" ? getCurrentPages() : [];
  for (let i = pages.length - 2; i >= 0; i -= 1) {
    if (getPageRoute(pages[i]) === targetRoute) return pages.length - 1 - i;
  }
  return 0;
}

function findBackDeltaToEntryPage() {
  const pages = typeof getCurrentPages === "function" ? getCurrentPages() : [];
  for (let i = pages.length - 2; i >= 0; i -= 1) {
    if (getPageRoute(pages[i]) === "pages/test/intro") {
      if (i <= 0) return 0;
      return pages.length - i;
    }
  }
  return 0;
}

function normalizeSuggestFlag(value) {
  const n = Number(value);
  return n === 0 ? 0 : 1;
}

Page({
  data: {
    navBarHeight: 0,
    mode: "complete",
    doneText: "完成自测",
    reviewTitle: "",
    reviewItems: [],
    canGenerateAiSummary: false,
    aiLoading: false,
    aiSummary: "",
    scrollIntoView: "",
  },
  onLoad(query) {
    const { navBarHeight } = getSystemMetrics();
    const menuRect = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
    const navHeight = menuRect && Number(menuRect.bottom || 0) > 0 ? Number(menuRect.bottom) : Number(navBarHeight || 0);
    const mode = query.mode || "complete";
    this.setData({
      navBarHeight: navHeight,
      mode,
      doneText: mode === "history" ? "返回记录" : "完成自测",
    });
  },
  onBack() {
    this.onDone();
  },
  onShow() {
    const session = getDailySession();
    if (!session) {
      wx.showToast({ title: "无结果数据", icon: "none" });
      wx.switchTab({ url: "/pages/home/index" });
      return;
    }
    const existingAiSummary = typeof session.aiSummary === "string" ? session.aiSummary : "";
    const reviewTitle = formatReviewTitle(
      session.submittedAt || session.recordDate || session.date || (session.submitResult && session.submitResult.submittedAt) || null,
    );
    const reviewItems = (session.items || [])
      .map((it) => {
        const optionIds = session.answers[it.questionId] || [];
        if (!optionIds.length) return null;
        const options = (it.options || [])
          .map((o) => ({
            optionId: o.optionId,
            content: normalizeInlineText(o.content),
            sortNo: o.sortNo,
            suggestFlag: normalizeSuggestFlag(o.suggestFlag),
            improvementTip: normalizeMultilineText(o.improvementTip),
            selected: optionIds.includes(o.optionId),
          }))
          .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0) || (a.optionId || 0) - (b.optionId || 0));
        const selectedOptions = options.filter((o) => o.selected);
        const selectedText = selectedOptions
          .filter((o) => o.selected)
          .map((o) => o.content)
          .filter(Boolean)
          .join("、") || "（未选择）";

        const adviceText = selectedOptions
          .map((o) => o.improvementTip)
          .filter(Boolean)
          .join("\n\n");

        return {
          questionId: it.questionId,
          question: normalizeInlineText(it.content),
          selectedText,
          adviceText,
        };
      })
      .filter(Boolean);
    this.setData({
      reviewTitle,
      reviewItems,
      aiSummary: existingAiSummary,
      canGenerateAiSummary:
        typeof session.assessmentId === "number" && session.assessmentId > 0 && !existingAiSummary,
      scrollIntoView: "",
    });
  },
  onDone() {
    if (this.data.mode === "history") {
      wx.navigateBack();
      return;
    }
    clearDailySession();

    const deltaToDailyDetail = findBackDeltaToRoute("pages/home/detail");
    if (deltaToDailyDetail > 0) {
      wx.navigateBack({ delta: deltaToDailyDetail });
      return;
    }
    const deltaToEntry = findBackDeltaToEntryPage();
    if (deltaToEntry > 0) {
      wx.navigateBack({ delta: deltaToEntry });
      return;
    }
    wx.switchTab({ url: "/pages/home/index" });
  },
  onGenerateAiSummary() {
    const session = getDailySession();
    if (!session || !session.assessmentId) return;
    if (this.data.aiLoading) return;
    this.setData({ aiLoading: true });
    wx.showLoading({ title: "生成中", mask: true });
    fetchAiSummary(session.assessmentId)
      .then((content) => {
        session.aiSummary = content;
        setDailySession(session);
        this.setData({ aiSummary: content, canGenerateAiSummary: false, scrollIntoView: "" }, () => {
          wx.nextTick(() => this.setData({ scrollIntoView: "tail-anchor" }));
        });
      })
      .catch((err) => {
        wx.hideLoading();
        const code = err && err.code ? String(err.code) : "";
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "生成失败";
        if (code === "SUBSCRIPTION_REQUIRED") {
          wx.showModal({
            title: "需要订阅",
            content: message || "未订阅或已过期，请先开通会员",
            confirmText: "去订阅",
            cancelText: "取消",
            success: (res) => {
              if (res && res.confirm) {
                wx.navigateTo({ url: "/pages/me/subscription" });
              }
            },
          });
          return;
        }
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ aiLoading: false });
      });
  },
});
