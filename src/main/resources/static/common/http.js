export async function requestJson(url, options) {
    const response = await fetch(url, options);
    const body = await response.json().catch(() => null);
    if (!response.ok || body?.status === 1) {
        throw new Error(body?.message || `Lỗi HTTP ${response.status}`);
    }
    if (response.status === 204) {
        return null;
    }
    if (body === null) {
        throw new Error('Máy chủ trả về dữ liệu JSON không hợp lệ.');
    }
    const wrapped = typeof body.status === 'number' && Object.prototype.hasOwnProperty.call(body, 'data');
    return wrapped ? body.data : body;
}
