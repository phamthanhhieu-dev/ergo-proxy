// Small, uncompressed ZIP writer. Subtitle archives are capped by the caller.
// Entry paths come from application-created EP folders; source file names are preserved.
export function subtitleZip(files) {
    const encoder = new TextEncoder();
    const locals = [];
    const directory = [];
    let offset = 0;
    const table = new Uint32Array(256);
    for (let index = 0; index < 256; index++) {
        let crc = index;
        for (let bit = 0; bit < 8; bit++) {
            crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
        }
        table[index] = crc >>> 0;
    }
    for (const file of files) {
        const name = encoder.encode(file.path);
        const bytes = file.bytes;
        let crc = 0xffffffff;
        for (const byte of bytes) {
            crc = (crc >>> 8) ^ table[(crc ^ byte) & 255];
        }
        crc = (crc ^ 0xffffffff) >>> 0;
        const local = new Uint8Array(30 + name.length);
        const localView = new DataView(local.buffer);
        localView.setUint32(0, 0x04034b50, true);
        localView.setUint16(4, 20, true);
        localView.setUint16(6, 0x800, true);
        localView.setUint32(14, crc, true);
        localView.setUint32(18, bytes.length, true);
        localView.setUint32(22, bytes.length, true);
        localView.setUint16(26, name.length, true);
        local.set(name, 30);
        locals.push(local, bytes);
        const central = new Uint8Array(46 + name.length);
        const view = new DataView(central.buffer);
        view.setUint32(0, 0x02014b50, true);
        view.setUint16(4, 20, true);
        view.setUint16(6, 20, true);
        view.setUint16(8, 0x800, true);
        view.setUint32(16, crc, true);
        view.setUint32(20, bytes.length, true);
        view.setUint32(24, bytes.length, true);
        view.setUint16(28, name.length, true);
        view.setUint32(42, offset, true);
        central.set(name, 46);
        directory.push(central);
        offset += local.length + bytes.length;
    }
    const end = new Uint8Array(22);
    const view = new DataView(end.buffer);
    view.setUint32(0, 0x06054b50, true);
    view.setUint16(8, files.length, true);
    view.setUint16(10, files.length, true);
    view.setUint32(
        12,
        directory.reduce((size, item) => size + item.length, 0),
        true,
    );
    view.setUint32(16, offset, true);
    return new Blob([...locals, ...directory, end], {
        type: "application/zip",
    });
}

export function saveBrowserFile(blob, name) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = name;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 30000);
}
