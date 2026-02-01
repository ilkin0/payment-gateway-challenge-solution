import http from 'k6/http';
import {check, group, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';
import {uuidv4} from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const paymentCreated = new Counter('payments_created');
const paymentAuthorized = new Counter('payments_authorized');
const paymentDeclined = new Counter('payments_declined');
const paymentRejected = new Counter('payments_rejected');
const paymentRetrieved = new Counter('payments_retrieved');
const paymentLatency = new Trend('payment_latency', true);
const successRate = new Rate('success_rate');

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        {duration: '30s', target: 10},
        {duration: '1m', target: 50},
        {duration: '2m', target: 50},
        {duration: '30s', target: 100},
        {duration: '1m', target: 100},
        {duration: '30s', target: 0},
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    success_rate: ['rate>0.95'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

// Card numbers for different scenarios (based on bank_simulator.ejs)
// Last digit: 1,3,5,7,9 = Authorized | 2,4,6,8 = Declined | 0 = Bank error
const CARD_NUMBERS = {
  authorized: [
    '4111111111111111', // ends in 1
    '4111111111111113', // ends in 3
    '4111111111111115', // ends in 5
    '4111111111111117', // ends in 7
    '4111111111111119', // ends in 9
  ],
  declined: [
    '4111111111111112', // ends in 2
    '4111111111111114', // ends in 4
    '4111111111111116', // ends in 6
    '4111111111111118', // ends in 8
  ],
  error: [
    '4111111111111110', // ends in 0 - bank unavailable
  ],
};

function getFutureExpiry() {
  const now = new Date();
  const futureMonth = ((now.getMonth() + 6) % 12) + 1;
  const futureYear = now.getFullYear() + (now.getMonth() + 6 >= 12 ? 1 : 0);
  return {month: futureMonth, year: futureYear};
}

function selectCard() {
  const rand = Math.random();
  if (rand < 0.80) {
    return {
      number: CARD_NUMBERS.authorized[Math.floor(
          Math.random() * CARD_NUMBERS.authorized.length)],
      expectedStatus: 'Authorized',
    };
  } else if (rand < 0.95) {
    return {
      number: CARD_NUMBERS.declined[Math.floor(
          Math.random() * CARD_NUMBERS.declined.length)],
      expectedStatus: 'Declined',
    };
  } else {
    return {
      number: CARD_NUMBERS.error[0],
      expectedStatus: 'Error',
    };
  }
}

function createPayment(idempotencyKey) {
  const card = selectCard();
  const expiry = getFutureExpiry();

  const payload = JSON.stringify({
    card_number: card.number,
    expiry_month: expiry.month,
    expiry_year: expiry.year,
    currency: ['USD', 'EUR', 'GBP'][Math.floor(Math.random() * 3)],
    amount: Math.floor(Math.random() * 10000) + 100, // 100-10100
    cvv: '123',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey || uuidv4(),
    },
    tags: {name: 'CreatePayment'},
    responseCallback: http.expectedStatuses(200, 502),
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/v1/payments`, payload, params);
  const duration = Date.now() - startTime;

  paymentLatency.add(duration);
  paymentCreated.add(1);

  if (card.expectedStatus === 'Error') {
    const success = check(response, {
      'bank error returns 502': (r) => r.status === 502,
    });
    successRate.add(success);
    if (response.status === 502) {
      paymentRejected.add(1);
    }
    return null;
  }

  const success = check(response, {
    'payment created': (r) => r.status === 200,
    'has payment id': (r) => r.json('id') !== undefined,
    'has correct status': (r) => r.json('status') === card.expectedStatus,
  });

  successRate.add(success);

  if (response.status === 200) {
    const status = response.json('status');
    if (status === 'Authorized') {
      paymentAuthorized.add(1);
    } else if (status === 'Declined') {
      paymentDeclined.add(1);
    }
    return response.json('id');
  }

  return null;
}

function getPayment(paymentId) {
  if (!paymentId) {
    return;
  }

  const params = {
    tags: {name: 'GetPayment'},
  };

  const response = http.get(`${BASE_URL}/api/v1/payments/${paymentId}`, params);

  const success = check(response, {
    'payment retrieved': (r) => r.status === 200,
    'has payment data': (r) => r.json('id') === paymentId,
  });

  successRate.add(success);

  if (response.status === 200) {
    paymentRetrieved.add(1);
  }
}

export default function () {
  group('Payment Flow', function () {
    const ik = uuidv4();

    const paymentId = createPayment(ik);

    if (Math.random() < 0.1) {
      sleep(0.1);
      createPayment(ik);
    }

    sleep(0.1);

    if (paymentId && Math.random() < 0.7) {
      getPayment(paymentId);
    }

    sleep(Math.random() * 0.4 + 0.1);
  });
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, {indent: ' ', enableColors: true}),
    'loadtest/summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data, opts) {
  const metrics = data.metrics;

  let summary = `
================================================================================
                         PAYMENT GATEWAY LOAD TEST RESULTS
================================================================================

REQUESTS:
  Total Requests:     ${metrics.http_reqs?.values?.count || 0}
  Request Rate:       ${(metrics.http_reqs?.values?.rate || 0).toFixed(2)}/s
  Failed Requests:    ${((metrics.http_req_failed?.values?.rate || 0)
      * 100).toFixed(2)}%

LATENCY:
  Average:            ${(metrics.http_req_duration?.values?.avg || 0).toFixed(
      2)}ms
  Median (p50):       ${(metrics.http_req_duration?.values['p(50)']
      || 0).toFixed(2)}ms
  p95:                ${(metrics.http_req_duration?.values['p(95)']
      || 0).toFixed(2)}ms
  p99:                ${(metrics.http_req_duration?.values['p(99)']
      || 0).toFixed(2)}ms
  Max:                ${(metrics.http_req_duration?.values?.max || 0).toFixed(
      2)}ms

PAYMENTS:
  Created:            ${metrics.payments_created?.values?.count || 0}
  Authorized:         ${metrics.payments_authorized?.values?.count || 0}
  Declined:           ${metrics.payments_declined?.values?.count || 0}
  Rejected (Bank):    ${metrics.payments_rejected?.values?.count || 0}
  Retrieved:          ${metrics.payments_retrieved?.values?.count || 0}

SUCCESS RATE:         ${((metrics.success_rate?.values?.rate || 0)
      * 100).toFixed(2)}%

================================================================================
`;
  return summary;
}
