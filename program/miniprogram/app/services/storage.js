function getStorage(key) {
  try {
    return wx.getStorageSync(key) || null;
  } catch {
    return null;
  }
}

function setStorage(key, value) {
  wx.setStorageSync(key, value);
}

function removeStorage(key) {
  wx.removeStorageSync(key);
}

module.exports = { getStorage, setStorage, removeStorage };

