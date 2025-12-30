import { getSystemMetrics } from "../../utils/system";

Component({
  properties: {
    title: { type: String, value: "" },
    left: { type: String, value: "" },
    right: { type: String, value: "" },
  },
  data: {
    statusBarHeight: 0,
    navContentHeight: 44,
  },
  lifetimes: {
    attached() {
      const { statusBarHeight, navBarHeight } = getSystemMetrics();
      this.setData({
        statusBarHeight,
        navContentHeight: Math.max(44, navBarHeight - statusBarHeight),
      });
    },
  },
  methods: {
    onLeft() {
      this.triggerEvent("left");
    },
    onRight() {
      this.triggerEvent("right");
    },
  },
});

