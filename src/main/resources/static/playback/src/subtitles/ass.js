const styles = `[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
WrapStyle: 0
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Liberation Sans,48,&H00FFFFFF,&H000000FF,&H00101010,&H80000000,0,0,0,0,100,100,0,0,1,2,1,2,40,40,60,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
`;
export function decodeSubtitle(bytes) {
    if (bytes[0] === 255 && bytes[1] === 254) {
        return new TextDecoder("utf-16le").decode(bytes);
    }
    if (bytes[0] === 254 && bytes[1] === 255) {
        return new TextDecoder("utf-16be").decode(bytes);
    }
    try {
        return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    } catch {
        return new TextDecoder("windows-1258").decode(bytes);
    }
}
function seconds(value) {
    const parts = value.replace(",", ".").split(":").map(Number);
    let total = 0;
    for (const part of parts) {
        total = total * 60 + part;
    }
    return total;
}
export function assTime(value) {
    const centiseconds = Math.max(0, Math.round(value * 100));
    const hours = Math.floor(centiseconds / 360000);
    const minutes = Math.floor(centiseconds / 6000) % 60;
    const remainder = Math.floor(centiseconds / 100) % 60;
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}.${String(centiseconds % 100).padStart(2, "0")}`;
}
export function toAss(content, extension) {
    if (extension === "ass" || extension === "ssa") {
        return content;
    }
    const blocks = content
        .replace(/^\uFEFF/, "")
        .replace(/\r/g, "")
        .split(/\n\s*\n/);
    const events = [];
    for (const block of blocks) {
        const lines = block.split("\n");
        const timeIndex = lines.findIndex((line) =>
            /\d{1,2}:\d{2}.*-->/.test(line),
        );
        if (timeIndex < 0) {
            continue;
        }
        const match = lines[timeIndex].match(/([\d:.,]+)\s*-->\s*([\d:.,]+)/);
        if (!match) {
            continue;
        }
        const start = seconds(match[1]);
        const end = seconds(match[2]);
        if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
            continue;
        }
        const body = lines.slice(timeIndex + 1).join("\n");
        const plain =
            new DOMParser().parseFromString(body, "text/html").body
                .textContent || "";
        const escaped = plain
            .replace(/\\/g, "\\u005c")
            .replace(/{/g, "｛")
            .replace(/}/g, "｝")
            .replace(/\n/g, "\\N");
        events.push(
            `Dialogue: 0,${assTime(start)},${assTime(end)},Default,,0,0,0,,${escaped}`,
        );
    }
    if (!events.length) {
        throw new Error(
            "Không đọc được nội dung SRT/VTT. Kiểm tra file phụ đề.",
        );
    }
    return styles + events.join("\n");
}
function splitColumns(value, length) {
    const fields = [];
    let offset = 0;
    for (let index = 0; index < length - 1; index++) {
        const comma = value.indexOf(",", offset);
        if (comma < 0) {
            return null;
        }
        fields.push(value.slice(offset, comma));
        offset = comma + 1;
    }
    fields.push(value.slice(offset));
    return fields;
}
/** Tạo bản xem/xuất riêng, không thay đổi nội dung nguồn. */
export function customizeAss(original, settings, shiftSeconds = 0) {
    let section = "";
    let format = [];
    return original
        .split(/\r?\n/)
        .map((line) => {
            if (/^\s*\[/.test(line)) {
                section = line.trim().toLowerCase();
                format = [];
                return line;
            }
            if (/^format\s*:/i.test(line)) {
                format = line
                    .slice(line.indexOf(":") + 1)
                    .split(",")
                    .map((value) => value.trim().toLowerCase());
                return line;
            }
            const style = /^style\s*:/i.test(line) && /styles/.test(section);
            const event = /^dialogue\s*:/i.test(line) && section === "[events]";
            if ((!style && !event) || !format.length) {
                return line;
            }
            const fields = splitColumns(
                line.slice(line.indexOf(":") + 1).trimStart(),
                format.length,
            );
            if (!fields) {
                return line;
            }
            if (style && !settings.original) {
                const font = format.indexOf("fontname");
                const size = format.indexOf("fontsize");
                if (font >= 0) {
                    fields[font] =
                        settings.font.replace(/[,\r\n]/g, "").slice(0, 80) ||
                        "Liberation Sans";
                }
                if (size >= 0) {
                    fields[size] = String(
                        Math.max(
                            8,
                            (Number(fields[size]) * settings.scale) / 100,
                        ),
                    );
                }
            }
            if (event) {
                if (shiftSeconds) {
                    for (const key of ["start", "end"]) {
                        const index = format.indexOf(key);
                        if (index >= 0) {
                            fields[index] = assTime(
                                seconds(fields[index]) + shiftSeconds,
                            );
                        }
                    }
                }
                const index = format.indexOf("text");
                if (index >= 0 && !settings.original) {
                    fields[index] = fields[index]
                        .replace(
                            /\\fn[^\\}]+/g,
                            "\\fn" + settings.font.replace(/[,\r\n]/g, ""),
                        )
                        .replace(
                            /\\fs(\d+(?:\.\d+)?)/g,
                            (_, size) =>
                                "\\fs" + (Number(size) * settings.scale) / 100,
                        );
                }
            }
            return (
                line.slice(0, line.indexOf(":") + 1) + " " + fields.join(",")
            );
        })
        .join("\n");
}
