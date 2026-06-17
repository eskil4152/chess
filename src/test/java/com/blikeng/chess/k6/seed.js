// Registers USERS users and saves their sessions (username + cookie) to results/sessions.json.
// Registration returns an AUTH cookie directly, so no separate login step needed.
// Run this once before any load test.
//
// Usage:
//   k6 run -e USERS=1000 seed.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE, jsonHeaders, getAuthCookie, PASSWORD } from './lib.js';

const USERS  = __ENV.USERS  ? parseInt(__ENV.USERS)  : 1000;
const RUN_ID = __ENV.RUN_ID || Date.now();

const userList = Array.from({ length: USERS }, (_, i) => ({
    username: `lt_${RUN_ID}_${i}`,
    password: PASSWORD,
}));

export const options = { vus: 1, iterations: 1, setupTimeout: '10m' };

export function setup() {
    const sessions = [];
    const batchSize = 50;

    for (let i = 0; i < userList.length; i += batchSize) {
        const batch = userList.slice(i, i + batchSize).map(u => ({
            method: 'POST',
            url:    `${BASE}/api/auth/register`,
            body:   JSON.stringify(u),
            params: jsonHeaders(),
        }));

        http.batch(batch).forEach((res, j) => {
            const ok     = check(res, { 'registered 201': r => r.status === 201 });
            const cookie = getAuthCookie(res);
            if (ok && cookie) sessions.push({ username: userList[i + j].username, cookie });
            else console.error(`seed: failed for ${userList[i + j].username}: HTTP ${res.status}`);
        });
    }

    console.log(`seed: registered ${sessions.length}/${USERS} users`);
    return sessions;
}

export default function () {}

export function handleSummary(data) {
    return {
        'results/sessions.json': JSON.stringify(data.setup_data, null, 2),
    };
}
