const { createChild, fetchChildren, getCachedChildren } = require("../../services/children");
const { formatDateYmd, calcAge } = require("../../utils/date");
const { getCachedMe, isProfileComplete } = require("../../services/auth");

Page({
  data: {
    today: formatDateYmd(new Date()),
    children: [],
    isAdding: false,
    saving: false,
    nickname: "",
    gender: 1,
    birthDate: "",
    showAuthModal: false,
  },
  onLoad(query) {
    if (query.action === "add") this.setData({ isAdding: true });
  },
  onShow() {
    const cached = getCachedChildren();
    if (cached) this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    this.reload();
  },
  reload() {
    fetchChildren()
      .then((list) => this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) }))
      .catch(() => {});
  },
  onBack() {
    wx.navigateBack();
  },
  onAdd() {
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true });
      return;
    }
    this.setData({ isAdding: true, nickname: "", birthDate: "", gender: 1 });
  },
  onCancel() {
    this.setData({ isAdding: false });
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
  onSave() {
    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true });
      return;
    }
    const nickname = String(this.data.nickname || "").trim();
    const birthDate = this.data.birthDate;
    if (!nickname || !birthDate) {
      wx.showToast({ title: "请填写完整信息", icon: "none" });
      return;
    }
    if (birthDate > this.data.today) {
      wx.showToast({ title: "出生日期不能是未来时间", icon: "none" });
      return;
    }
    this.setData({ saving: true });
    createChild({ nickname, gender: this.data.gender, birthDate })
      .then(() => this.reload())
      .then(() => {
        this.setData({ isAdding: false, nickname: "", birthDate: "" });
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
