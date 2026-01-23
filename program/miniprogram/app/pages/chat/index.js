const { getSystemMetrics } = require("../../utils/system");
const { createChatSession, listChatSessions, listChatQuickQuestions, listChatMessages, sendChatMessage, streamChat } = require("../../services/chat");
const { getCachedMe, isProfileComplete } = require("../../services/auth");
const towxml = require("../../towxml/index");

const DEFAULT_AI_GREETING = "你好！我是你的育儿助手 Howtotalk。今天遇到了什么挑战吗？";

function toAiNodes(markdown) {
  try {
    return towxml(String(markdown ?? ""), "markdown", { theme: "light" });
  } catch {
    return { theme: "light", child: [], _e: { child: [] } };
  }
}

const DEFAULT_AI_GREETING_NODES = toAiNodes(DEFAULT_AI_GREETING);
const EMPTY_AI_NODES = toAiNodes("");

function toUiRole(role) {
  if (role === "user") return "user";
  return "ai";
}

function toUiMessages(descMessages) {
  const items = Array.isArray(descMessages) ? descMessages : [];
  return items
    .slice()
    .reverse()
    .map((m) => ({
      id: `m-${m.messageId}`,
      role: toUiRole(m.role),
      text: m.content || "",
      nodes: toUiRole(m.role) === "ai" ? toAiNodes(m.content || "") : undefined,
      messageId: m.messageId,
      createdAt: m.createdAt,
    }));
}

Page({
  data: {
    statusBarHeight: 20,
    drawerOpen: false,
    sessions: [],
    sessionId: 0,
    messages: [{ id: "m1", role: "ai", text: DEFAULT_AI_GREETING, nodes: DEFAULT_AI_GREETING_NODES }],
    quickQuestions: [],
    quickLoading: false,
    input: "",
    inputTrim: "",
    typing: false,
    scrollIntoId: "bottom",
    showAuthModal: false,
    pendingSend: false,
    tabbarHeight: 0,
    bottomPadding: 0,
    historyLoading: false,
    historyLoadingMore: false,
    historyHasMore: false,
    historyBeforeMessageId: null,
  },
  onLoad() {
    const { statusBarHeight } = getSystemMetrics();
    this.setData({ statusBarHeight });
  },
  onHide() {
    this.stopAiTyping();
    this.stopAiFallbackPoll();
  },
  onUnload() {
    this.stopAiTyping();
    this.stopAiFallbackPoll();
  },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    tab && tab.setData && tab.setData({ selected: 2 });

    if (typeof wx.nextTick === "function") wx.nextTick(() => this.syncBottomLayout());
    else this.syncBottomLayout();

    this.refreshSessions().then(() => {
      if (this.data.sessionId) this.loadSessionMessages();
    });

    this.refreshQuickQuestions();
  },
  refreshQuickQuestions() {
    if (this.data.quickLoading) return Promise.resolve();
    this.setData({ quickLoading: true });
    return listChatQuickQuestions(6)
      .then((items) => {
        const list = Array.isArray(items) ? items : [];
        const normalized = list.map((x) => String(x || "").trim()).filter(Boolean);
        this.setData({ quickQuestions: normalized.slice(0, 6) });
      })
      .catch(() => {
        this.setData({ quickQuestions: [] });
      })
      .finally(() => {
        this.setData({ quickLoading: false });
      });
  },
  syncBottomLayout() {
    const tab = this.getTabBar && this.getTabBar();

    let inputHeight = 0;
    let tabbarHeight = 0;

    const commit = () => {
      const bottomPadding = Math.max(0, Math.ceil(tabbarHeight + inputHeight + 12));
      this.setData({ tabbarHeight: Math.max(0, Math.ceil(tabbarHeight)), bottomPadding });
    };

    wx.createSelectorQuery()
      .select(".input")
      .boundingClientRect((rect) => {
        inputHeight = rect && rect.height ? rect.height : 0;
        commit();
      })
      .exec();

    if (!tab) return;
    wx.createSelectorQuery()
      .in(tab)
      .select(".tabbar")
      .boundingClientRect((rect) => {
        tabbarHeight = rect && rect.height ? rect.height : 0;
        commit();
      })
      .exec();
  },
  refreshSessions() {
    return listChatSessions(20)
      .then((sessions) => this.setData({ sessions }))
      .catch(() => this.setData({ sessions: [] }));
  },
  openDrawer() {
    this.setData({ drawerOpen: true });
  },
  closeDrawer() {
    this.setData({ drawerOpen: false });
  },
  ensureSession() {
    const existing = Number(this.data.sessionId);
    if (existing) return Promise.resolve(existing);

    return createChatSession(null)
      .then((sessionId) => {
        this.setData({ sessionId });
        return sessionId;
      })
      .catch((e) => {
        this.setData({ sessionId: 0 });
        throw e;
      });
  },
  loadSessionMessages(loadMore = false) {
    const sessionId = Number(this.data.sessionId);
    if (!sessionId) return Promise.resolve();
    if (loadMore && (this.data.historyLoading || this.data.historyLoadingMore || !this.data.historyHasMore)) return Promise.resolve();
    if (!loadMore && this.data.historyLoading) return Promise.resolve();

    const limit = 20;
    const beforeMessageId = loadMore ? this.data.historyBeforeMessageId : null;
    const setLoading = loadMore ? { historyLoadingMore: true } : { historyLoading: true };
    this.setData(setLoading);

    return listChatMessages(sessionId, limit, beforeMessageId)
      .then((descMessages) => {
        const uiMessages = toUiMessages(descMessages);
        const oldest = uiMessages.length ? uiMessages[0].messageId : null;
        const hasMore = Array.isArray(descMessages) && descMessages.length === limit;

        if (!loadMore) {
          if (!uiMessages.length) {
            this.setData({
              messages: [{ id: "m1", role: "ai", text: DEFAULT_AI_GREETING, nodes: DEFAULT_AI_GREETING_NODES }],
              historyHasMore: false,
              historyBeforeMessageId: null,
            });
          } else {
            this.setData({ messages: uiMessages, historyHasMore: hasMore, historyBeforeMessageId: oldest });
            this.scrollToBottom();
          }
          return;
        }

        const merged = [...uiMessages, ...this.data.messages];
        this.setData({ messages: merged, historyHasMore: hasMore, historyBeforeMessageId: oldest || this.data.historyBeforeMessageId });
      })
      .catch(() => {
        // ignore
      })
      .finally(() => {
        this.setData({ historyLoading: false, historyLoadingMore: false });
      });
  },
  startNewChat() {
    this.stopAiTyping();
    this.stopAiFallbackPoll();
    this.setData({
      sessionId: 0,
      messages: [
        {
          id: `ai-${Date.now()}`,
          role: "ai",
          text: DEFAULT_AI_GREETING,
          nodes: DEFAULT_AI_GREETING_NODES,
        },
      ],
      input: "",
      inputTrim: "",
      typing: false,
      drawerOpen: false,
      historyLoading: false,
      historyLoadingMore: false,
      historyHasMore: false,
      historyBeforeMessageId: null,
    });
    this.refreshQuickQuestions();
    return this.refreshSessions().then(() => this.scrollToBottom());
  },
  selectSession(e) {
    this.stopAiTyping();
    this.stopAiFallbackPoll();
    const id = Number(e.currentTarget.dataset.id);
    this.setData({
      sessionId: id,
      drawerOpen: false,
      historyHasMore: false,
      historyBeforeMessageId: null,
    });
    this.loadSessionMessages();
  },
  onMessagesScrollToUpper() {
    this.loadSessionMessages(true);
  },
  onInput(e) {
    const v = e.detail.value || "";
    this.setData({ input: v, inputTrim: String(v).trim() });
  },
  onQuickQuestionTap(e) {
    const prompt = String((e && e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.prompt) || "").trim();
    if (!prompt) return;
    this.sendText(prompt);
  },
  scrollToBottom() {
    this.setData({ scrollIntoId: `bottom-${Date.now()}` });
    this.setData({ scrollIntoId: "bottom" });
  },
  stopAiTyping() {
    if (this._aiTypingTimer) {
      clearInterval(this._aiTypingTimer);
      this._aiTypingTimer = null;
    }
    this._aiTypingState = null;
  },
  stopAiFallbackPoll() {
    if (this._aiFallbackTimer) {
      clearInterval(this._aiFallbackTimer);
      this._aiFallbackTimer = null;
    }
    this._aiFallbackState = null;
  },
  startAiFallbackPoll(sessionId, userMessageId, aiMsgId) {
    // Fallback for devices where SSE chunks/done are not delivered reliably.
    this.stopAiFallbackPoll();
    this._aiFallbackState = {
      sessionId: Number(sessionId),
      userMessageId: Number(userMessageId),
      aiMsgId: String(aiMsgId || ""),
      startedAt: Date.now(),
    };

    const MAX_WAIT_MS = 120 * 1000;
    const INTERVAL_MS = 2000;

    this._aiFallbackTimer = setInterval(() => {
      const s = this._aiFallbackState;
      if (!s) return;
      if (!this.data.typing) return;
      if (Date.now() - s.startedAt > MAX_WAIT_MS) {
        // Avoid leaving the UI in an infinite "typing" state when streaming/polling both fail.
        this.failAiTyping("（AI 回复超时。请稍后重试）");
        return;
      }

      listChatMessages(s.sessionId, 20, null)
        .then((descMessages) => {
          const list = Array.isArray(descMessages) ? descMessages : [];
          const assistant = list.find(
            (m) => m && m.role === "assistant" && Number(m.messageId) > s.userMessageId && String(m.content || "").trim()
          );
          if (!assistant) return;

          const content = String(assistant.content || "");
          const idx = Array.isArray(this.data.messages) ? this.data.messages.findIndex((m) => m && m.id === s.aiMsgId) : -1;
          if (idx >= 0) {
            this.setData({ [`messages[${idx}].text`]: content, [`messages[${idx}].nodes`]: toAiNodes(content), typing: false });
          } else {
            // As a fallback, refresh the whole session.
            this.setData({ typing: false });
            this.loadSessionMessages(false);
          }
          this.stopAiTyping();
          this.stopAiFallbackPoll();
          this.refreshSessions();
          this.scrollToBottom();
        })
        .catch(() => {
          // ignore
        });
    }, INTERVAL_MS);
  },
  beginAiTyping(aiMsgId) {
    const msgIndex = Array.isArray(this.data.messages) ? this.data.messages.findIndex((m) => m && m.id === aiMsgId) : -1;
    this.stopAiTyping();
    this._aiTypingState = {
      aiMsgId,
      msgIndex,
      text: "",
      pending: "",
      queue: [],
      streamDone: false,
      lastScrollAt: 0,
    };

    const CHARS_PER_TICK = 6;
    const TICK_MS = 40;
    const SCROLL_THROTTLE_MS = 200;

    this._aiTypingTimer = setInterval(() => {
      const s = this._aiTypingState;
      if (!s) return;
      if (s.msgIndex < 0) return;

      if (!s.pending && s.queue.length) {
        s.pending = String(s.queue.shift() || "");
      }

      if (s.pending) {
        const take = s.pending.slice(0, CHARS_PER_TICK);
        s.pending = s.pending.slice(CHARS_PER_TICK);
        s.text += take;
        this.setData({ [`messages[${s.msgIndex}].text`]: s.text });
        const now = Date.now();
        if (now - (s.lastScrollAt || 0) >= SCROLL_THROTTLE_MS) {
          s.lastScrollAt = now;
          this.scrollToBottom();
        }
        return;
      }

      if (s.streamDone && !s.queue.length) {
        const nodes = toAiNodes(s.text);
        this.setData({ [`messages[${s.msgIndex}].nodes`]: nodes, typing: false });
        this.stopAiTyping();
        this.refreshSessions();
      }
    }, TICK_MS);
  },
  enqueueAiDelta(delta) {
    const s = this._aiTypingState;
    if (!s) return;
    const d = String(delta || "");
    if (!d) return;
    this.stopAiFallbackPoll();
    s.queue.push(d);
  },
  endAiTyping() {
    const s = this._aiTypingState;
    if (!s) return;
    this.stopAiFallbackPoll();
    s.streamDone = true;
    if (!s.pending && !s.queue.length) {
      const nodes = toAiNodes(s.text);
      if (s.msgIndex >= 0) this.setData({ [`messages[${s.msgIndex}].nodes`]: nodes });
      this.setData({ typing: false });
      this.stopAiTyping();
      this.refreshSessions();
    }
  },
  failAiTyping(message) {
    const s = this._aiTypingState;
    if (!s) return;
    this.stopAiFallbackPoll();
    if (!s.text) {
      s.text = String(message || "（网络异常。稍后再试）");
      if (s.msgIndex >= 0) this.setData({ [`messages[${s.msgIndex}].text`]: s.text });
    }
    s.pending = "";
    s.queue = [];
    this.endAiTyping();
  },
  promptSubscribe(message) {
    wx.showModal({
      title: "需要订阅",
      content: message || "未订阅或已过期，请先开通会员",
      confirmText: "去订阅",
      cancelText: "取消",
      success: (res) => {
        if (res && res.confirm) {
          wx.navigateTo({ url: "/pages/me/subscription" });
        }
      },
    });
  },
  sendText(rawText) {
    const text = String(rawText || "").trim();
    if (!text || this.data.typing) return;

    const me = getCachedMe();
    if (!isProfileComplete(me)) {
      this.setData({ showAuthModal: true, pendingSend: true });
      return;
    }

    const ensure = this.data.sessionId ? Promise.resolve(this.data.sessionId) : this.ensureSession();
    ensure
      .then(() => {
        if (!this.data.sessionId) {
          wx.showToast({ title: "无法创建会话", icon: "none", duration: 3000, mask: true });
          return;
        }

        const prevMessages = this.data.messages;
        const prevInput = this.data.input;
        const prevInputTrim = this.data.inputTrim;
        const userMsg = { id: `u-${Date.now()}`, role: "user", text };
        const aiMsgId = `a-${Date.now() + 1}`;
        const aiMsg = { id: aiMsgId, role: "ai", text: "", nodes: EMPTY_AI_NODES };
        this.setData({ messages: [...this.data.messages, userMsg, aiMsg], input: "", inputTrim: "", typing: true });
        this.scrollToBottom();
        this.beginAiTyping(aiMsgId);

        sendChatMessage(this.data.sessionId, text, { toast: false })
          .then((userMessageId) => {
            this.startAiFallbackPoll(this.data.sessionId, userMessageId, aiMsgId);
            const task = streamChat(this.data.sessionId, {
              onDelta: (delta) => {
                this.enqueueAiDelta(delta);
              },
              onDone: () => {
                this.endAiTyping();
              },
              onError: (message, code) => {
                if (code === "NETWORK_ERROR") return;
                this.failAiTyping(`（${String(message || "请求失败")}）`);
              },
            });
          })
          .catch((e) => {
            const code = e && e.code ? String(e.code) : "";
            const message = e && e.message ? String(e.message) : "发送失败";
            this.stopAiTyping();
            this.stopAiFallbackPoll();
            this.setData({ typing: false, messages: prevMessages, input: prevInput, inputTrim: prevInputTrim });
            if (code === "SUBSCRIPTION_REQUIRED") {
              this.promptSubscribe(message);
              return;
            }
            wx.showToast({ title: message || "发送失败", icon: "none", duration: 3000, mask: true });
          });
      })
      .catch(() => {
        wx.showToast({ title: "无法创建会话", icon: "none", duration: 3000, mask: true });
      });
  },
  onSend() {
    this.sendText(this.data.input);
  },
  onAuthSuccess() {
    const shouldSend = Boolean(this.data.pendingSend);
    this.setData({ showAuthModal: false, pendingSend: false });
    if (shouldSend) {
      this.onSend();
    }
  },
  onCopyAi(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.id || "") : "";
    if (!id) return;
    const msg = Array.isArray(this.data.messages) ? this.data.messages.find((m) => m && m.id === id) : null;
    const text = msg && msg.text ? String(msg.text) : "";
    if (!text.trim()) return;
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: "已复制", icon: "success", duration: 1500 });
      },
      fail: () => {
        wx.showToast({ title: "复制失败", icon: "none", duration: 2000 });
      },
    });
  },
});
