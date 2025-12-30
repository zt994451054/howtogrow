export function formatDateYmd(date: Date): string {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

export function calcAge(birthDateYmd: string, now = new Date()): number {
  const [y, m, d] = birthDateYmd.split("-").map((v) => Number(v));
  if (!y || !m || !d) return 0;

  let age = now.getFullYear() - y;
  const month = now.getMonth() + 1;
  const day = now.getDate();
  if (month < m || (month === m && day < d)) {
    age -= 1;
  }
  return Math.max(0, age);
}

