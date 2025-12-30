Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: "/pages/home/index",
        text: "首页",
        iconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E%3Cpath d='M11.47 3.84a.75.75 0 0 1 1.06 0l8.69 8.69a.75.75 0 1 0 1.06-1.06l-8.689-8.69a2.25 2.25 0 0 0-3.182 0l-8.69 8.69a.75.75 0 0 0 1.061 1.06l8.69-8.69Z'/%3E%3Cpath d='M12 5.432 5.15 12.283A1.196 1.196 0 0 1 5.378 13.9a1.193 1.193 0 0 1-1.128.537h.575v5.188a2.25 2.25 0 0 0 2.25 2.25h9.75a2.25 2.25 0 0 0 2.25-2.25V14.44h.572a1.195 1.195 0 0 1-1.124-.535 1.196 1.196 0 0 1 .225-1.621L12 5.432Z'/%3E%3C/svg%3E",
        selectedIconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23f97316'%3E%3Cpath d='M11.47 3.84a.75.75 0 0 1 1.06 0l8.69 8.69a.75.75 0 1 0 1.06-1.06l-8.689-8.69a2.25 2.25 0 0 0-3.182 0l-8.69 8.69a.75.75 0 0 0 1.061 1.06l8.69-8.69Z'/%3E%3Cpath d='M12 5.432 5.15 12.283A1.196 1.196 0 0 1 5.378 13.9a1.193 1.193 0 0 1-1.128.537h.575v5.188a2.25 2.25 0 0 0 2.25 2.25h9.75a2.25 2.25 0 0 0 2.25-2.25V14.44h.572a1.195 1.195 0 0 1-1.124-.535 1.196 1.196 0 0 1 .225-1.621L12 5.432Z'/%3E%3C/svg%3E"
      },
      {
        pagePath: "/pages/test/index",
        text: "每日自测",
        iconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E%3Cpath fill-rule='evenodd' d='M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 1.5h-3c.621 0 1.16.376 1.376.923a51.13 51.13 0 0 0 .248 0C12.34 1.876 12.879 1.5 13.5 1.5Zm-7.875 6h9.75a2.25 2.25 0 0 1 2.25 2.25v7.5a2.25 2.25 0 0 1-2.25 2.25h-9.75a2.25 2.25 0 0 1-2.25-2.25v-7.5a2.25 2.25 0 0 1 2.25-2.25Z' clip-rule='evenodd'/%3E%3C/svg%3E",
        selectedIconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23f97316'%3E%3Cpath fill-rule='evenodd' d='M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 1.5h-3c.621 0 1.16.376 1.376.923a51.13 51.13 0 0 0 .248 0C12.34 1.876 12.879 1.5 13.5 1.5Zm-7.875 6h9.75a2.25 2.25 0 0 1 2.25 2.25v7.5a2.25 2.25 0 0 1-2.25 2.25h-9.75a2.25 2.25 0 0 1-2.25-2.25v-7.5a2.25 2.25 0 0 1 2.25-2.25Z' clip-rule='evenodd'/%3E%3C/svg%3E"
      },
      {
        pagePath: "/pages/chat/index",
        text: "马上沟通",
        iconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E%3Cpath fill-rule='evenodd' d='M4.804 21.644A6.707 6.707 0 0 0 6 21.75a6.721 6.721 0 0 0 3.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 0 1-.814 1.686.75.75 0 0 0 .44 1.223ZM8.25 9.75a.75.75 0 0 1 .75-.75h6a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1-.75-.75Zm0 4.5a.75.75 0 0 1 .75-.75h6a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1-.75-.75Z' clip-rule='evenodd'/%3E%3C/svg%3E",
        selectedIconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23f97316'%3E%3Cpath fill-rule='evenodd' d='M4.804 21.644A6.707 6.707 0 0 0 6 21.75a6.721 6.721 0 0 0 3.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 0 1-.814 1.686.75.75 0 0 0 .44 1.223ZM8.25 9.75a.75.75 0 0 1 .75-.75h6a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1-.75-.75Zm0 4.5a.75.75 0 0 1 .75-.75h6a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1-.75-.75Z' clip-rule='evenodd'/%3E%3C/svg%3E"
      },
      {
        pagePath: "/pages/me/index",
        text: "我的",
        iconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E%3Cpath fill-rule='evenodd' d='M7.5 6a4.5 4.5 0 1 1 9 0 4.5 4.5 0 0 1-9 0ZM3.751 20.105a8.25 8.25 0 0 1 16.498 0 .75.75 0 0 1-.437.695A18.683 18.683 0 0 1 12 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 0 1-.437-.695Z' clip-rule='evenodd'/%3E%3C/svg%3E",
        selectedIconPath: "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23f97316'%3E%3Cpath fill-rule='evenodd' d='M7.5 6a4.5 4.5 0 1 1 9 0 4.5 4.5 0 0 1-9 0ZM3.751 20.105a8.25 8.25 0 0 1 16.498 0 .75.75 0 0 1-.437.695A18.683 18.683 0 0 1 12 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 0 1-.437-.695Z' clip-rule='evenodd'/%3E%3C/svg%3E"
      },
    ],
  },
  methods: {
    onTap(e: WechatMiniprogram.BaseEvent) {
      const { index, path } = (e.currentTarget as any).dataset as { index: number; path: string };
      wx.switchTab({ url: path });
      this.setData({ selected: index });
    },
  },
});
