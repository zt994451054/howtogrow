Component({
  properties: {
    avatarUrl: { type: String, value: "" },
    avatarText: { type: String, value: "我" },
    greetName: { type: String, value: "" },
    greetSuffix: { type: String, value: "您好" },
    greetText: { type: String, value: "您好" },
  },
  methods: {
    onProfileTap() {
      this.triggerEvent("profile");
    },
    onMenuTap() {
      this.triggerEvent("menu");
    },
  },
});
