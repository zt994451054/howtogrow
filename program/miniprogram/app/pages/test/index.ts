import { STORAGE_KEYS } from "../../services/config";
import { getCachedChildren, fetchChildren } from "../../services/children";
import { setStorage } from "../../services/storage";
import { calcAge } from "../../utils/date";

Page({
  data: {
    loading: false,
    children: [] as Array<{ id: number; nickname: string; gender: 0 | 1 | 2; birthDate: string; age: number }>,
  },
  async onShow() {
    const tab = (this as any).getTabBar?.();
    tab?.setData?.({ selected: 1 });

    const cached = getCachedChildren();
    if (cached) {
      this.setData({ children: cached.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    }
    await this.loadChildren();
  },
  async loadChildren() {
    this.setData({ loading: true });
    try {
      const list = await fetchChildren();
      this.setData({ children: list.map((c) => ({ ...c, age: calcAge(c.birthDate) })) });
    } catch (error) {
      if (!(this.data.children && this.data.children.length > 0)) {
        this.setData({ children: [] });
      }
    } finally {
      this.setData({ loading: false });
    }
  },
  onAddChild() {
    setStorage(STORAGE_KEYS.navMe, { view: "children", action: "add" });
    wx.switchTab({ url: "/pages/me/index" });
  },
  onSelectChild(e: WechatMiniprogram.BaseEvent) {
    const { id, name } = (e.currentTarget as any).dataset as { id: number; name: string };
    wx.navigateTo({ url: `/pages/test/intro?childId=${id}&childName=${encodeURIComponent(name)}` });
  },
});
