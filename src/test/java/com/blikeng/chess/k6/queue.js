// Load test for WS queue + game cycle (connect → queue → match → resign).
// Tracks game-end notification latency via ws_game_end_latency_ms.
//
// Usage:
//   k6 run -e SESSIONS_FILE=results/sessions.json -e USERS=500 queue.js

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE, WS_BASE, jsonHeaders, makeOptions } from './lib.js';

const SESSIONS_FILE = __ENV.SESSIONS_FILE;
if (!SESSIONS_FILE) throw new Error('SESSIONS_FILE required — run seed.js first');
const _sessions = JSON.parse(open(SESSIONS_FILE));

const TARGET         = __ENV.USERS ? parseInt(__ENV.USERS) : 250;
const gameEndLatency = new Trend('ws_game_end_latency_ms', true);
export const options = {
    ...makeOptions(TARGET),
    thresholds: {
        http_req_duration:      ['p(95)<500'],
        http_req_failed:        ['rate<0.01'],
        ws_game_end_latency_ms: ['p(95)<300'],
    },
};

export function setup() {
    return _sessions.slice(0, TARGET);
}

export default function (data) {
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
