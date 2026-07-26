// Tracks which user IDs currently have at least one live socket connection.
// Refcounted (not a plain Set) so a user with multiple tabs/devices open only
// goes "offline" once every connection has actually disconnected.
const online = new Map();

export function markOnline(userId) {
    online.set(userId, (online.get(userId) || 0) + 1);
}

export function markOffline(userId) {
    const count = (online.get(userId) || 0) - 1;
    if (count <= 0) online.delete(userId);
    else online.set(userId, count);
}

export function isOnline(userId) {
    return online.has(userId);
}
