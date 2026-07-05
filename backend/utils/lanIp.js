import os from "os";

// Adapter name patterns that are virtual / not the real WiFi-LAN uplink
// (VirtualBox host-only, VMware, Hyper-V vEthernet, WSL, Docker, etc.).
// Phones on the real WiFi network cannot reach these, so we must skip them
// when picking the LAN IP to advertise via /api/config.
const VIRTUAL_ADAPTER_PATTERNS = [
    /virtualbox/i,
    /vmware/i,
    /vethernet/i,
    /hyper-v/i,
    /docker/i,
    /wsl/i,
    /loopback/i,
    /tap/i,
    /vpn/i
];

/**
 * Returns the machine's real LAN IPv4 address (the one reachable by other
 * devices on the same WiFi/router network, e.g. 192.168.x.x from a WiFi or
 * Ethernet adapter — NOT a virtual adapter created by VirtualBox/VMware/
 * Hyper-V/Docker/WSL, which have similar-looking private IPs but are not
 * reachable from a phone on the same WiFi).
 * Falls back to "localhost" if none found.
 */
// Known private-IP ranges that VirtualBox host-only / VMware adapters
// commonly default to. These ranges are NOT reachable from a phone on the
// home WiFi, so an address in these ranges should be de-prioritized even if
// the adapter's OS-reported name looks generic (e.g. "Ethernet 2").
const SUSPECT_VIRTUAL_RANGES = [
    /^192\.168\.56\./,   // VirtualBox default host-only network
    /^192\.168\.99\./,   // Docker Toolbox / other VM defaults
    /^172\.1[6-9]\./, /^172\.2[0-9]\./, /^172\.3[0-1]\./ // Docker/Hyper-V default range
];

function collectCandidates() {
    const interfaces = os.networkInterfaces();
    const candidates = [];
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === "IPv4" && !iface.internal) {
                candidates.push({ name, address: iface.address });
            }
        }
    }
    return candidates;
}

export function getLanIp() {
    const candidates = collectCandidates();
    if (candidates.length === 0) return "localhost";

    const isVirtualName = (name) => VIRTUAL_ADAPTER_PATTERNS.some((p) => p.test(name));
    const isSuspectRange = (address) => SUSPECT_VIRTUAL_RANGES.some((p) => p.test(address));
    const isWifiName = (name) => /wi-?fi|wlan/i.test(name);

    // Priority 1: adapter explicitly named Wi-Fi/WLAN (most reliable for
    // "device on the same home WiFi" scenario) with a non-suspect IP.
    let match = candidates.find((c) => isWifiName(c.name) && !isSuspectRange(c.address));
    if (match) return match.address;

    // Priority 2: any adapter that isn't virtual-named and isn't in a
    // suspect virtual IP range (covers real Ethernet uplinks).
    match = candidates.find((c) => !isVirtualName(c.name) && !isSuspectRange(c.address));
    if (match) return match.address;

    // Priority 3: any adapter not virtual-named (ignore IP range check).
    match = candidates.find((c) => !isVirtualName(c.name));
    if (match) return match.address;

    // Priority 4: last resort — first available candidate.
    return candidates[0].address;
}

export default getLanIp;
