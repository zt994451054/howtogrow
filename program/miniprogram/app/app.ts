import { ensureLoggedIn } from "./services/auth";

App<IAppOption>({
  globalData: {
    me: null,
    children: null,
  },
  async onLaunch() {
    try {
      await ensureLoggedIn();
    } catch (error) {
      console.warn("ensureLoggedIn failed", error);
    }
  },
});

interface IAppOption {
  globalData: {
    me: import("./services/types").MiniprogramUserView | null;
    children: import("./services/types").ChildView[] | null;
  };
  onLaunch: () => void;
}

