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
      wx.switchTab({ url: "/pages/chat/index" });
      this.setData({ selected: 2 });
    },
  },
});
