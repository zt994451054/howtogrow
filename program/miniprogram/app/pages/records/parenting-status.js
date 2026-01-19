const { ensureLoggedIn } = require("../../services/auth");
const { fetchChildren } = require("../../services/children");
const { getDailyParentingStatus, upsertDailyParentingStatus } = require("../../services/parenting-status");
const { formatDateYmd, calcAge } = require("../../utils/date");

const STATUS_OPTIONS = [
  { code: "乐观", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E4%B9%90%E8%A7%82.jpg" },
  { code: "失望", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%A4%B1%E6%9C%9B.jpg" },
  { code: "平静", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%B9%B3%E9%9D%99.jpg" },
  { code: "开心", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E5%BC%80%E5%BF%83.jpg" },
  { code: "愤怒", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%84%A4%E6%80%92.jpg" },
  { code: "担忧", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%8B%85%E5%BF%A7.jpg" },
  { code: "无奈", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%97%A0%E5%A5%88.jpg" },
  { code: "欣慰", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E6%AC%A3%E6%85%B0.jpg" },
  { code: "绝望", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E7%BB%9D%E6%9C%9B.jpg" },
  { code: "难过", imageUrl: "https://howtotalk.oss-cn-beijing.aliyuncs.com/parenting/%E9%9A%BE%E8%BF%87.jpg" },
];

function safeText(value) {
  return value == null ? "" : String(value);
}

function safeDecode(value) {
  const v = safeText(value).trim();
  if (!v) return "";
  try {
    return decodeURIComponent(v);
  } catch {
    return v;
  }
}

function isYmd(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(safeText(value).trim());
}

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

Page({
  data: {
    today: formatDateYmd(new Date()),
    recordDate: formatDateYmd(new Date()),
    children: [],
    childId: 0,
    childName: "",
    statusCode: "",
    saving: false,
    statusOptions: STATUS_OPTIONS,
  },

  onLoad(query) {
    const nowYmd = formatDateYmd(new Date());
    const childId = toSafeId(query?.childId);
    const recordDateRaw = safeText(query?.recordDate).trim();
    const recordDate = isYmd(recordDateRaw) ? recordDateRaw : nowYmd;
    const childName = safeDecode(query?.childName);

    this.setData({
      today: nowYmd,
      recordDate: recordDate <= nowYmd ? recordDate : nowYmd,
      childId,
      childName,
    });

    ensureLoggedIn()
      .then(() => this.reloadChildren({ preferredChildId: childId }))
      .catch(() => wx.showToast({ title: "登录失败", icon: "none" }));
  },

  onBack() {
    wx.navigateBack();
  },

  reloadChildren({ preferredChildId } = {}) {
    return fetchChildren()
      .then((list) => {
        const today = new Date();
        const children = (Array.isArray(list) ? list : []).map((c) => ({ ...c, age: calcAge(c.birthDate, today) }));
        const pickedId = pickValidChildId(children, preferredChildId || this.data.childId);
        const picked = children.find((c) => toSafeId(c?.id) === pickedId) || null;
        this.setData({
          children,
          childId: pickedId,
          childName: picked ? safeText(picked.nickname).trim() : this.data.childName,
          statusCode: "",
        });
        if (pickedId) this.loadExisting();
      })
      .catch(() => this.setData({ children: [], childId: 0 }));
  },

  onPickChild(e) {
    const id = toSafeId(e.currentTarget.dataset.id);
    const name = safeText(e.currentTarget.dataset.name).trim();
    if (!id) return;
    this.setData({ childId: id, childName: name || "孩子", statusCode: "" });
    this.loadExisting();
  },

  onRecordDate(e) {
    const date = safeText(e.detail.value).trim();
    if (!isYmd(date)) return;
    const next = date <= this.data.today ? date : this.data.today;
    this.setData({ recordDate: next });
    if (this.data.childId) this.loadExisting();
  },

  loadExisting() {
    const childId = toSafeId(this.data.childId);
    const recordDate = safeText(this.data.recordDate).trim();
    if (!childId || !isYmd(recordDate)) return;

    getDailyParentingStatus(childId, recordDate, { toast: false })
      .then((res) => {
        const status = safeText(res?.statusCode).trim();
        this.setData({ statusCode: status });
      })
      .catch(() => {});
  },

  onSelectStatus(e) {
    const code = safeText(e.currentTarget.dataset.code).trim();
    if (!code) return;
    this.setData({ statusCode: code });
  },

  onSave() {
    const childId = toSafeId(this.data.childId);
    if (!childId) {
      wx.showToast({ title: "请选择孩子", icon: "none" });
      return;
    }
    const statusCode = safeText(this.data.statusCode).trim();
    if (!statusCode) {
      wx.showToast({ title: "请选择育儿状态", icon: "none" });
      return;
    }
    const recordDate = safeText(this.data.recordDate).trim();
    if (!isYmd(recordDate) || recordDate > this.data.today) {
      wx.showToast({ title: "记录日期不能是未来时间", icon: "none" });
      return;
    }
    if (this.data.saving) return;

    this.setData({ saving: true });
    upsertDailyParentingStatus({ childId, recordDate, statusCode })
      .then(() => wx.showToast({ title: "已保存", icon: "success" }))
      .catch((err) => {
        const message = err && typeof err.message === "string" && err.message.trim() ? err.message.trim() : "保存失败";
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => this.setData({ saving: false }));
  },
});
