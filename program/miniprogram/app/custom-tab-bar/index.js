const { STORAGE_KEYS } = require("../services/config");
const { getStorage } = require("../services/storage");
const { formatDateYmd } = require("../utils/date");

Component({
  data: {
    selected: 0,
  },
  methods: {
    onTap(e) {
      const { index, path } = e.currentTarget.dataset;
      wx.switchTab({ url: path });
      this.setData({ selected: Number(index) });
    },
    onMoodTap() {
      const childId =
        Number(getStorage(STORAGE_KEYS.navHomeSelectedChildId) || 0) ||
        Number(getStorage(STORAGE_KEYS.navCurveSelectedChildId) || 0);

      if (!childId) {
        wx.switchTab({ url: "/pages/home/index" });
        this.setData({ selected: 0 });
        wx.showToast({ title: "请先选择孩子", icon: "none" });
        return;
      }

      const date = formatDateYmd(new Date());
      const url = `/pages/home/detail?childId=${encodeURIComponent(String(childId))}&date=${encodeURIComponent(date)}&open=status`;

      wx.switchTab({
        url: "/pages/home/index",
        success: () => {
          // Ensure the tab switch completes so the next `navigateTo` has a clean stack.
          setTimeout(() => wx.navigateTo({ url }), 50);
        },
      });
      this.setData({ selected: 0 });
    },
  },
});
