const base = '/api/v1/playback';
export async function request(path, options = {}) {
    const response = await fetch(base + path, options);
    const data = await response.json().catch(() => null);
    if (!response.ok || data?.status === 1) {
        throw new Error(data?.message || `Lỗi HTTP ${response.status}`);
    }
    return data?.data ?? data;
}
export function fileUrl(id) {
    return `${base}/files/${encodeURIComponent(id)}`;
}

export function text(node, value) {
    node.textContent = value;
}
