import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE, WS_BASE, jsonHeaders } from './lib.js';

const SESSIONS_FILE = __ENV.SESSIONS_FILE;
if (!SESSIONS_FILE) throw new Error('SESSIONS_FILE required — run seed.js first');
const _sessions = JSON.parse(open(SESSIONS_FILE));

const TARGET    = __ENV.USERS ? parseInt(__ENV.USERS) : 500;
const DURATION  = '4m';
const RAMP      = '30s';

const QUEUE_VUS   = Math.round(TARGET * 0.50);
const PROFILE_VUS = Math.round(TARGET * 0.30);
const STATS_VUS   = TARGET - QUEUE_VUS - PROFILE_VUS;

const gameEndLatency = new Trend('ws_game_end_latency_ms', true);

export const options = {
    scenarios: {
        queue: {
            executor: 'ramping-vus',
            stages: [
                { duration: RAMP, target: QUEUE_VUS },
                { duration: DURATION, target: QUEUE_VUS },
                { duration: RAMP, target: 0 },
            ],
            gracefulRampDown: '30s',
            exec: 'queueScenario',
        },
        profile: {
            executor: 'ramping-vus',
            stages: [
                { duration: RAMP, target: PROFILE_VUS },
                { duration: DURATION, target: PROFILE_VUS },
                { duration: RAMP, target: 0 },
            ],
            gracefulRampDown: '30s',
            exec: 'profileScenario',
        },
        stats: {
            executor: 'ramping-vus',
            stages: [
                { duration: RAMP, target: STATS_VUS },
                { duration: DURATION, target: STATS_VUS },
                { duration: RAMP, target: 0 },
            ],
            gracefulRampDown: '30s',
            exec: 'statsScenario',
        },
    },
    thresholds: {
        http_req_duration:      ['p(95)<500'],
        http_req_failed:        ['rate<0.01'],
        ws_game_end_latency_ms: ['p(95)<300'],
    },
};

export function setup() {
    return _sessions.slice(0, TARGET);
}

export function queueScenario(data) {
    const s = data[(__VU - 1) % data.length];
    if (!s) return;

    let resignSentAt = null;

    const res = ws.connect(`${WS_BASE}/ws`, { headers: { Cookie: `AUTH=${s.cookie}` } }, function (socket) {
        socket.on('open', () => {
            http.post(`${BASE}/api/queue`, JSON.stringify({ timeControl: 'RAPID_10_0' }), jsonHeaders(s.cookie));
        });

        socket.on('message', raw => {
            let msg;
            try { msg = JSON.parse(raw); } catch { return; }

            if (msg.type === 'GAME_STARTED' && msg.whiteUsername === s.username) {
                socket.setTimeout(() => {
                    resignSentAt = Date.now();
                    socket.send(JSON.stringify({ type: 'RESIGN', gameId: msg.gameId }));
                }, 50);
            }

            if (msg.type === 'GAME_ENDED') {
                if (resignSentAt) gameEndLatency.add(Date.now() - resignSentAt);
                socket.close();
            }
        });

        socket.on('error', e => console.error(`ws error [${s.username}]: ${e}`));
        socket.setTimeout(() => socket.close(), 45000);
    });

    check(res, { 'ws upgraded 101': r => r && r.status === 101 });
    sleep(1);
}

export function profileScenario(data) {
    const s = data[(__VU - 1) % data.length];
    if (!s) return;

    const res = http.get(`${BASE}/api/user/${s.username}`, jsonHeaders(s.cookie));
    check(res, { 'profile 200': r => r.status === 200 });
    sleep(1);
}

export function statsScenario(data) {
    const s = data[(__VU - 1) % data.length];
    if (!s) return;

    const res = http.get(`${BASE}/api/user/${s.username}/stats/RAPID`, jsonHeaders(s.cookie));
    check(res, { 'stats 200': r => r.status === 200 });
    sleep(1);
}
