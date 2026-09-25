// 앱 전체를 띄우는 Compose UI 테스트는 한 번에 이벤트 루프를 2초 넘게 잡는다. Karma(browserDisconnectTimeout·
// pingTimeout)와 mocha(테스트마다)의 기본 2초에 걸려 Chrome 이 끊긴 것으로 처리되면, 그 뒤 테스트는 돌지도 않는다
// (docs/platform/web.html#test).
config.set({
    browserDisconnectTimeout: 120000,
    browserNoActivityTimeout: 120000,
    pingTimeout: 120000,
});
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
