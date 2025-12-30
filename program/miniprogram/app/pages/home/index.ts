import { fetchRandomQuote } from "../../services/quotes";

Page({
  data: {
    heroQuote: "爱不是替他做所有事，是忍住不做\n让他发现自己有多棒",
    aboutText:
      "Parenting and communication expert Heleen de Hertog is the founder of Howtotalk. Using the Howtotalk method, she teaches you how to deal with every imaginable parenting challenge -...",
    courses: [
      {
        id: 1,
        title: "Effective Communication with children",
        subtitle: "OF 2-12 YEARS",
        imageUrl: "https://picsum.photos/id/342/400/600",
      },
      { id: 2, title: "Potty training", subtitle: "FOR 1-4 YEARS", imageUrl: "https://picsum.photos/id/338/400/600" },
      { id: 3, title: "Everything toddler", subtitle: "FOR 2-5 YEARS", imageUrl: "https://picsum.photos/id/1027/400/600" },
    ],
  },
  onShow() {
    const tab = (this as any).getTabBar?.();
    tab?.setData?.({ selected: 0 });
  },
  async onLoad() {
    // try {
    //   const quote = await fetchRandomQuote();
    //   if (quote) {
    //     this.setData({ heroQuote: quote });
    //   }
    // } catch {
    //   // ignore
    // }
  },
  onBookmark() {
    wx.showToast({ title: "已收藏（占位）", icon: "none" });
  },
});
