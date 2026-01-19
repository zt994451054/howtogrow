const { createChild, updateChild, fetchChildren } = require("../../services/children");
const { formatDateYmd, calcAge } = require("../../utils/date");
const { getCachedMe, isProfileComplete } = require("../../services/auth");

function safeText(value) {
  return value == null ? "" : String(value);
}

Page({
  data: {
    today: formatDateYmd(new Date()),
    children: [],
    formMode: "",
    editingChildId: 0,
    saving: false,
    nickname: "",
    gender: 1,
    birthDate: "",
    parentIdentityOptions: ["妈妈", "爸爸", "爷爷", "奶奶", "外公", "外婆"],
    parentIdentityIndex: 0,
    parentIdentity: "妈妈",
    showAuthModal: false,
  },
  onLoad(query) {
    if (query.action === "add") this.openAddForm();
  },
  onShow() {
    this.reload();
  },
  reload() {
    fetchChildren()
      .then((list) => this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) }))
      .catch(() => {});
  },
  onHeaderLeft() {
    if (this.data.formMode) this.onCancel();
    else this.onBack();
  },
  onBack() {
    wx.navigateBack();
  },
  ensureNoActiveForm() {
    if (this.data.formMode) {
      wx.showToast({ title: "请先保存或取消当前编辑", icon: "none" });
      return false;
    }
    return true;
  },
  onAdd() {
    if (!this.ensureNoActiveForm()) return;
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true });
      return;
    }
    this.openAddForm();
  },
  openAddForm() {
    this.setData({
      formMode: "add",
      editingChildId: 0,
      nickname: "",
      birthDate: "",
      gender: 1,
      parentIdentityIndex: 0,
      parentIdentity: "妈妈",
    });
  },
  onEdit(e) {
    if (!this.ensureNoActiveForm()) return;
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true });
      return;
    }
    const { id, nickname, gender, birthDate, parentIdentity } = e.currentTarget.dataset;
    const options = this.data.parentIdentityOptions || [];
    const identityText = String(parentIdentity || "妈妈");
    const idx = options.indexOf(identityText);
    this.setData({
      formMode: "edit",
      editingChildId: Number(id),
      nickname: String(nickname || ""),
      gender: Number(gender) || 1,
      birthDate: String(birthDate || ""),
      parentIdentityIndex: idx >= 0 ? idx : 0,
      parentIdentity: idx >= 0 ? identityText : "妈妈",
    });
  },
  onCancel() {
    this.setData({
      formMode: "",
      editingChildId: 0,
      nickname: "",
      birthDate: "",
      gender: 1,
      parentIdentityIndex: 0,
      parentIdentity: "妈妈",
    });
  },
  onNickname(e) {
    this.setData({ nickname: e.detail.value });
  },
  setBoy() {
    this.setData({ gender: 1 });
  },
  setGirl() {
    this.setData({ gender: 2 });
  },
  onBirthDate(e) {
    this.setData({ birthDate: e.detail.value });
  },
  onPickParentIdentity(e) {
    const value = safeText(e?.currentTarget?.dataset?.value).trim();
    if (!value) return;
    const options = Array.isArray(this.data.parentIdentityOptions) ? this.data.parentIdentityOptions : [];
    const idx = Math.max(0, options.indexOf(value));
    this.setData({ parentIdentity: value, parentIdentityIndex: idx >= 0 ? idx : 0 });
  },
  onParentIdentity(e) {
    const idx = Number(e.detail.value) || 0;
    const options = this.data.parentIdentityOptions || [];
    this.setData({
      parentIdentityIndex: idx,
      parentIdentity: options[idx] || "妈妈",
    });
  },
  onSave() {
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true });
      return;
    }
    if (!this.data.formMode) return;
    const nickname = String(this.data.nickname || "").trim();
    const birthDate = this.data.birthDate;
    const parentIdentity = safeText(this.data.parentIdentity).trim();
    if (!nickname || !birthDate) {
      wx.showToast({ title: "请填写完整信息", icon: "none" });
      return;
    }
    if (!parentIdentity) {
      wx.showToast({ title: "请选择家长身份", icon: "none" });
      return;
    }
    if (birthDate > this.data.today) {
      wx.showToast({ title: "出生日期不能是未来时间", icon: "none" });
      return;
    }
    this.setData({ saving: true });
    const payload = { nickname, gender: this.data.gender, birthDate, parentIdentity };
    const promise =
      this.data.formMode === "add"
        ? createChild(payload)
        : updateChild(this.data.editingChildId, payload);
    promise
      .then(() => this.reload())
      .then(() => {
        this.setData({ formMode: "", editingChildId: 0, nickname: "", birthDate: "" });
        wx.showToast({ title: "已保存", icon: "success" });
      })
      .catch(() => wx.showToast({ title: "保存失败", icon: "none" }))
      .finally(() => this.setData({ saving: false }));
  },
  onAuthSuccess() {
    this.setData({ showAuthModal: false });
  },
  onAuthClose() {
    this.setData({ showAuthModal: false });
  },
});
