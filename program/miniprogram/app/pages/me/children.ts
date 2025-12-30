import { createChild, fetchChildren, getCachedChildren } from "../../services/children";
import { formatDateYmd, calcAge } from "../../utils/date";

Page({
  data: {
    today: formatDateYmd(new Date()),
    children: [] as Array<{ id: number; nickname: string; gender: 0 | 1 | 2; birthDate: string; age: number }>,
    isAdding: false,
    saving: false,
    nickname: "",
    gender: 1 as 0 | 1 | 2,
    birthDate: "",
  },
  onLoad(query: Record<string, string>) {
    if (query.action === "add") {
      this.setData({ isAdding: true });
    }
  },
  async onShow() {
    const cached = getCachedChildren();
    if (cached) {
      this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    }
    await this.reload();
  },
  async reload() {
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
  onAdd() {
    this.setData({ isAdding: true, nickname: "", birthDate: "", gender: 1 });
  },
  onCancel() {
    this.setData({ isAdding: false });
  },
  onNickname(e: WechatMiniprogram.Input) {
    this.setData({ nickname: e.detail.value });
  },
  setBoy() {
    this.setData({ gender: 1 });
  },
  setGirl() {
    this.setData({ gender: 2 });
  },
  onBirthDate(e: WechatMiniprogram.PickerChange) {
    this.setData({ birthDate: e.detail.value });
  },
  async onSave() {
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
    try {
      await createChild({ nickname, gender: this.data.gender, birthDate });
      await this.reload();
      this.setData({ isAdding: false, nickname: "", birthDate: "" });
      wx.showToast({ title: "已保存", icon: "success" });
    } catch {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});

