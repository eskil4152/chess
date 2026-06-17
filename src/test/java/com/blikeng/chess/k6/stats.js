// Load test for GET /api/user/{username}/stats/{tc}
//
// Usage:
//   k6 run -e SESSIONS_FILE=results/sessions.json -e USERS=250 -e TC=RAPID stats.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, jsonHeaders, makeOptions } from './lib.js';

const SESSIONS_FILE = __ENV.SESSIONS_FILE;
if (!SESSIONS_FILE) throw new Error('SESSIONS_FILE required — run seed.js first');
const _sessions = JSON.parse(open(SESSIONS_FILE));

const TARGET = __ENV.USERS ? parseInt(__ENV.USERS) : 100;
const TC     = __ENV.TC    || 'RAPID';
export const options = makeOptions(TARGET);

export function setup() {
    return _sessions.slice(0, TARGET);
}

export default function (data) {
    const s = data[(__VU - 1) % data.length];
    if (!s) return;

    const res = http.get(`${BASE}/api/user/${s.username}/stats/${TC}`, jsonHeaders(s.cookie));
    check(res, { 'stats 200': r => r.status === 200 });
    sleep(1);
}
