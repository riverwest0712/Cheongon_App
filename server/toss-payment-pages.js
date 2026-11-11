const json = value => JSON.stringify(value).replace(/</g, '\\u003c');

function shell(content, script = '', sdk = false) {
  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>청온 테스트 결제</title>
  <style>
    body { margin:0; background:#f7f8fa; font-family:system-ui,sans-serif; color:#191f28; }
    main { max-width:420px; margin:48px auto; padding:28px; background:white; border-radius:20px; }
    h1 { font-size:24px; } p { line-height:1.7; } .badge { color:#2563eb; }
    button { width:100%; padding:16px; border:0; border-radius:12px; background:#2563eb; color:white; font-size:16px; }
    button:disabled { opacity:.5; } #message { white-space:pre-line; color:#667085; }
    @media(max-width:480px) { main { margin:20px 16px; } }
  </style>
  ${sdk ? '<script src="https://js.tosspayments.com/v2/standard"></script>' : ''}
</head>
<body>
  <main><p class="badge">청온 · 테스트 결제</p>${content}</main>
  <script>${script}</script>
</body>
</html>`;
}

function checkout(order, clientKey) {
  return shell(`
    <h1>취업 도구 영구 이용권</h1>
    <h2>990원</h2>
    <p>1회 결제 · 영구 이용<br>테스트 결제로 실제 금액은 청구되지 않습니다.</p>
    <button id="pay">토스 결제창 열기</button>
    <p id="message" role="status">토스 결제창으로 이동합니다.</p>`, `
    const order = ${json({ id: order.order_id, token: order.checkout_token })};
    const clientKey = ${json(clientKey)};
    const button = document.getElementById('pay');
    async function start() {
      button.disabled = true;
      try {
        const payment = TossPayments(clientKey).payment({ customerKey: order.id });
        await payment.requestPayment({
          method: 'CARD',
          amount: { currency: 'KRW', value: 990 },
          orderId: order.id,
          orderName: '청온 취업 도구 영구 이용권',
          successUrl: location.origin + '/payments/jobtool/success?token=' + encodeURIComponent(order.token),
          failUrl: location.origin + '/payments/jobtool/fail',
        });
      } catch (error) {
        document.getElementById('message').textContent = '결제창을 열지 못했거나 결제가 취소됐어요. 다시 시도해주세요.';
        button.disabled = false;
      }
    }
    button.addEventListener('click', start);
    start();`, true);
}

function success() {
  return shell(`
    <h1 id="title">결제 확인 중</h1>
    <p id="message" role="status">서버에서 결제 승인을 확인하고 있어요.</p>
    <button id="retry" hidden>결제 결과 다시 확인</button>`, `
    const params = new URLSearchParams(location.search);
    const body = { token: params.get('token'), paymentKey: params.get('paymentKey'),
      orderId: params.get('orderId'), amount: Number(params.get('amount')) };
    // Keep recovery parameters until approval is stored, including across a page reload.
    const retry = document.getElementById('retry');
    async function confirm() {
      retry.disabled = true;
      try {
        const response = await fetch('/payments/jobtool/confirm', {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        });
        const result = await response.json();
        if (!response.ok || result.purchased !== true) throw new Error(result.message || '결제 확인에 실패했습니다.');
        document.getElementById('title').textContent = '테스트 결제 완료';
        document.getElementById('message').textContent = '취업 도구 이용권이 활성화됐어요. 앱으로 돌아가면 바로 이용할 수 있습니다.';
        retry.hidden = true;
        history.replaceState(null, '', location.pathname);
      } catch (error) {
        document.getElementById('title').textContent = '결제 확인이 필요해요';
        document.getElementById('message').textContent = error.message;
        retry.hidden = false;
        retry.disabled = false;
      }
    }
    retry.addEventListener('click', confirm);
    confirm();`);
}

const finished = () => shell('<h1>이미 결제한 이용권이에요</h1><p>앱으로 돌아가 취업 도구를 이용해주세요.</p>');
const cancelled = () => shell('<h1>결제가 완료되지 않았어요</h1><p>결제가 취소되었거나 실패했습니다. 앱으로 돌아가 다시 시도해주세요.</p>');
const error = () => shell('<h1>결제 페이지를 열 수 없어요</h1><p>앱에서 다시 시도해주세요. 문제가 계속되면 결제 서버 설정을 확인해주세요.</p>');

module.exports = { checkout, success, finished, cancelled, error };
