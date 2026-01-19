const { getMonthlyAwareness } = require("../../services/awareness");

function safeText(value) {
  return value == null ? "" : String(value);
}

function isYmd(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(safeText(value).trim());
}

function parseMonthValue(value) {
  const v = safeText(value).trim();
  const m = /^(\d{4})-(\d{2})$/.exec(v);
  if (!m) return null;
  const year = Number(m[1]);
  const month = Number(m[2]);
  if (!year || month < 1 || month > 12) return null;
  return { year, month };
}

function formatMonthValue(year, month) {
  const y = Number(year);
  const m = Number(month);
  if (!y || m < 1 || m > 12) return "";
  return `${String(y).padStart(4, "0")}-${String(m).padStart(2, "0")}`;
}

function listMonthValuesBetween(fromYmd, toYmd) {
  const from = safeText(fromYmd).trim().slice(0, 7);
  const to = safeText(toYmd).trim().slice(0, 7);
  const start = parseMonthValue(from);
  const end = parseMonthValue(to);
  if (!start || !end) return [];

  const months = [];
  let y = start.year;
  let m = start.month;
  while (y < end.year || (y === end.year && m <= end.month)) {
    months.push(formatMonthValue(y, m));
    m += 1;
    if (m > 12) {
      m = 1;
      y += 1;
    }
  }
  return months;
}

function buildDistribution(days, fromYmd, toYmd) {
  const from = safeText(fromYmd).trim();
  const to = safeText(toYmd).trim();
  const counts = new Map(); // sceneId -> count
  const meta = new Map(); // sceneId -> { name, logoUrl, shortName }

  (Array.isArray(days) ? days : []).forEach((d) => {
    const date = d && d.recordDate != null ? String(d.recordDate) : "";
    if (!date || date < from || date > to) return;
    const scenes = Array.isArray(d?.troubleScenes) ? d.troubleScenes : [];
    scenes.forEach((s) => {
      const id = s && s.id != null ? String(s.id).trim() : "";
      const name = s && s.name != null ? String(s.name).trim() : "";
      if (!id) return;
      counts.set(id, (counts.get(id) || 0) + 1);
      const existing = meta.get(id);
      const logoUrl = s && s.logoUrl != null ? String(s.logoUrl).trim() : "";
      if (!existing) {
        meta.set(id, { name, logoUrl, shortName: name ? name.slice(0, 1) : "?" });
      } else if (!existing.logoUrl && logoUrl) {
        meta.set(id, { ...existing, logoUrl });
      }
    });
  });

  const sorted = Array.from(counts.entries())
    .map(([id, count]) => {
      const m = meta.get(id) || { name: "", logoUrl: "", shortName: "?" };
      return { id, count, name: m.name || `场景${id}`, logoUrl: m.logoUrl || "", shortName: m.shortName || "?" };
    })
    .filter((x) => x.count > 0)
    .sort((a, b) => b.count - a.count || String(a.name).localeCompare(String(b.name)));

  const maxCount = Math.max(...sorted.map((x) => x.count), 0) || 1;
  return sorted.map((t, idx) => {
    const rank = idx + 1;
    const isTop3 = rank <= 3;
    const pct = Math.max(2, Math.round((t.count / maxCount) * 100));
    const rankClass = rank === 1 ? "top1" : rank === 2 ? "top2" : rank === 3 ? "top3" : "other";
    const barClass = rank === 1 ? "top1" : rank === 2 ? "top2" : rank === 3 ? "top3" : "other";
    return {
      ...t,
      rank,
      pct,
      rankClass,
      barClass,
      iconClass: isTop3 ? "top" : "",
      textClass: isTop3 ? "top" : "",
      countClass: isTop3 ? "top" : "",
    };
  });
}

Page({
  data: {
    childId: 0,
    from: "",
    to: "",
    loading: false,
    items: [],
  },

  onLoad(query) {
    const childId = Number(query?.childId || 0);
    const from = safeText(query?.from).trim();
    const to = safeText(query?.to).trim();
    if (!childId || !isYmd(from) || !isYmd(to)) {
      this.setData({
        childId: childId || 0,
        from,
        to,
        items: [],
      });
      wx.showToast({ title: "参数错误", icon: "none" });
      return;
    }
    this.setData({ childId, from, to });
    this.loadData();
  },

  onBack() {
    wx.navigateBack();
  },

  loadData() {
    const childId = Number(this.data.childId || 0);
    const from = safeText(this.data.from).trim();
    const to = safeText(this.data.to).trim();
    if (!childId || !isYmd(from) || !isYmd(to) || from > to) return;
    if (this.data.loading) return;

    const months = listMonthValuesBetween(from, to);
    this.setData({ loading: true });
    Promise.all(months.map((m) => getMonthlyAwareness(childId, m, { toast: false }).catch(() => null)))
      .then((responses) => {
        const days = (Array.isArray(responses) ? responses : []).reduce((acc, r) => {
          const list = Array.isArray(r?.days) ? r.days : [];
          acc.push(...list);
          return acc;
        }, []);
        this.setData({ items: buildDistribution(days, from, to) });
      })
      .catch(() => this.setData({ items: [] }))
      .finally(() => this.setData({ loading: false }));
  },
});
