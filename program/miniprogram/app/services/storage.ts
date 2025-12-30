export function getStorage<T>(key: string): T | null {
  try {
    return (wx.getStorageSync(key) as T) ?? null;
  } catch {
    return null;
  }
}

export function setStorage<T>(key: string, value: T): void {
  wx.setStorageSync(key, value);
}

export function removeStorage(key: string): void {
  wx.removeStorageSync(key);
}

