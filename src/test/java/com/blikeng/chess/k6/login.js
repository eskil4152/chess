import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, jsonHeaders, getAuthCookie, makeOptions, PASSWORD } from './lib.js';

const SESSIONS_FILE = __ENV.SESSIONS_FILE;
if (!SESSIONS_FILE) throw new Error('SESSIONS_FILE required — run seed.js first');
const _sessions = JSON.parse(open(SESSIONS_FILE));

const TARGET = __ENV.USERS ? parseInt(__ENV.USERS) : 100;
export const options = {
    ...makeOptions(TARGET),
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed:   ['rate<0.01'],
    },
};

export function setup() {
    return _sessions.slice(0, TARGET);
}

export default function (data) {
    const s = data[(__VU - 1) % data.length];
    if (!s) return;

    const res = http.post(
        `${BASE}/api/auth/login`,
        JSON.stringify({ username: s.username, password: PASSWORD, rememberMe: false }),
        jsonHeaders(),
    );
    check(res, {
        'login 200':        r => r.status === 200,
        'login set cookie': r => getAuthCookie(r) !== null,
    });
    sleep(1);
}
