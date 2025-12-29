export function formatStatus(status: number): string {
  return status === 1 ? "启用" : "禁用";
}

export function formatMoneyCent(amountCent: number): string {
  const cents = Number.isFinite(amountCent) ? amountCent : 0;
  const yuan = cents / 100;
  return yuan.toFixed(2);
}

function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const yyyy = date.getFullYear();
  const MM = pad2(date.getMonth() + 1);
  const dd = pad2(date.getDate());
  const HH = pad2(date.getHours());
  const mm = pad2(date.getMinutes());
  const ss = pad2(date.getSeconds());
  return `${yyyy}-${MM}-${dd} ${HH}:${mm}:${ss}`;
}
