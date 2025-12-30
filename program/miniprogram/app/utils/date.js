function formatDateYmd(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function calcAge(birthDateYmd, now = new Date()) {
  const parts = String(birthDateYmd || "").split("-").map((v) => Number(v));
  if (parts.length !== 3) return 0;
  const [y, m, d] = parts;
  if (!y || !m || !d) return 0;

  let age = now.getFullYear() - y;
  const month = now.getMonth() + 1;
  const day = now.getDate();
  if (month < m || (month === m && day < d)) {
    age -= 1;
  }
  return Math.max(0, age);
}

module.exports = { formatDateYmd, calcAge };

