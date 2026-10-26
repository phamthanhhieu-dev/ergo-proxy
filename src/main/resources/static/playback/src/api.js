import { requestJson } from '../../common/http.js';
const base = '/api/v1/playback';
export function request(path, options) {
    return requestJson(base + path, options);
}
export function fileUrl(id) {
    return `${base}/files/${encodeURIComponent(id)}`;
}

export function text(node, value) {
    node.textContent = value;
}
