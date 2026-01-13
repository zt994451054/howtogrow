const assert = require("assert");
const { TextEncoder: UtilTextEncoder } = require("util");

const { streamChat } = require("../chat");

function testHttp401() {
  const calls = {
    toast: [],
    abort: 0,
    onError: [],
    removeStorage: [],
  };

  let requestOptions;
  const requestTask = {
    abort: () => {
      calls.abort += 1;
    },
  };

  global.wx = {
    showToast: (options) => calls.toast.push(options),
    removeStorageSync: (key) => calls.removeStorage.push(key),
    request: (options) => {
      requestOptions = options;
      return requestTask;
    },
  };

  streamChat(1, {
    onDelta: () => {},
    onDone: () => {},
    onError: (message) => calls.onError.push(message),
  });

  requestOptions.success({ statusCode: 401, data: JSON.stringify({ message: "未登录" }) });
  requestOptions.fail();

  assert.strictEqual(calls.toast.length, 1);
  assert.strictEqual(calls.toast[0].title, "未登录");
  assert.strictEqual(calls.abort, 1);
  assert.deepStrictEqual(calls.onError, ["未登录"]);
}

function testSseErrorEvent() {
  const calls = {
    toast: [],
    abort: 0,
    onError: [],
    removeStorage: [],
  };

  let chunkHandler;

  global.wx = {
    showToast: (options) => calls.toast.push(options),
    removeStorageSync: (key) => calls.removeStorage.push(key),
    request: (_options) => {
      const requestTask = {
        abort: () => {
          calls.abort += 1;
        },
        onChunkReceived: (cb) => {
          chunkHandler = cb;
        },
      };
      return requestTask;
    },
  };

  streamChat(1, {
    onDelta: () => {},
    onDone: () => {},
    onError: (message) => calls.onError.push(message),
  });

  assert.strictEqual(typeof chunkHandler, "function");

  const sse = 'event: error\ndata: {"code":"UNAUTHORIZED","message":"未登录"}\n\n';
  const encoder = typeof TextEncoder === "function" ? new TextEncoder() : new UtilTextEncoder();
  const buf = encoder.encode(sse).buffer;
  chunkHandler({ data: buf });

  assert.strictEqual(calls.toast.length, 1);
  assert.strictEqual(calls.toast[0].title, "未登录");
  assert.strictEqual(calls.abort, 1);
  assert.deepStrictEqual(calls.onError, ["未登录"]);
  assert.ok(calls.removeStorage.length > 0);
}

function main() {
  testHttp401();
  testSseErrorEvent();
  // eslint-disable-next-line no-console
  console.log("ok");
}

main();
