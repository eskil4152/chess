export const BASE    = __ENV.BASE_URL || 'http://localhost:8081';
export const WS_BASE = BASE.replace('http://', 'ws://').replace('https://', 'wss://');
export const PASSWORD = 'Loadtest1!';

export function jsonHeaders(cookie = null) {
    const h = { 'Content-Type': 'application/json' };
    if (cookie) h.Cookie = `AUTH=${cookie}`;
    return { headers: h };
}

export function getAuthCookie(res) {
    const c = res.cookies.AUTH;
    return c && c.length > 0 ? c[0].value : null;
}

export function makeOptions(target) {
    return {
        stages: [
            { duration: '30s', target: Math.ceil(target * 0.25) },
            { duration: '1m',  target },
            { duration: '2m',  target },
            { duration: '30s', target: 0 },
        ],
    };
}
