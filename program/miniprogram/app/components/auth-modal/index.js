const { updateProfile } = require("../../services/auth");

Component({
  properties: {
    show: { type: Boolean, value: false },
  },
  data: {
    avatarUrl: "",
    nickname: "",
    formattedNickname: "",
  },
  methods: {
    onClose() {
      this.triggerEvent("close");
    },
    onChooseAvatar(e) {
      const avatarUrl = e?.detail?.avatarUrl || "";
      this.setData({ avatarUrl });
    },
    onNicknameChange(e) {
      const nickname = e?.detail?.value || "";
      this.setData({
        nickname,
        formattedNickname: String(nickname).trim(),
      });
    },
    onSubmit() {
      const avatarUrl = this.data.avatarUrl;
      const nickname = this.data.formattedNickname;
      if (!avatarUrl || !nickname) return;

      wx.showLoading({ title: "保存中..." });
      updateProfile({ nickname, avatarUrl })
        .then(() => {
          wx.showToast({ title: "完善成功", icon: "success" });
          this.triggerEvent("success");
        })
        .catch((err) => {
          console.error(err);
          wx.showToast({ title: "保存失败", icon: "none" });
        })
        .finally(() => wx.hideLoading());
    },
  },
});

