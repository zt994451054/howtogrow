const { ensureLoggedIn } = require("./services/auth");

App({
  globalData: {
    me: null,
    children: null,
  },
  onLaunch() {
    ensureLoggedIn().catch((err) => {
      console.warn("ensureLoggedIn failed", err);
    });
  },
});

