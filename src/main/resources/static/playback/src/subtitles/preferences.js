import { createStorage } from "../../../common/storage.js";

const STORAGE_KEY = "ergo.playback.subtitle-preferences.v1";
const DEFAULT_FONT = "Liberation Sans";

export class SubtitlePreferences {
    constructor(onError) {
        this.onError = onError;
        this.warned = false;
        this.store = createStorage("localStorage", () => this.warn());
    }
    normalize(value = {}) {
        const rememberDelay = value.rememberDelay === true;
        const font =
            typeof value.font === "string"
                ? value.font.trim().slice(0, 80)
                : DEFAULT_FONT;
        return {
            version: 1,
            original:
                typeof value.original === "boolean" ? value.original : true,
            font: font || DEFAULT_FONT,
            scale: Number.isFinite(value.scale)
                ? Math.max(50, Math.min(250, value.scale))
                : 100,
            rememberDelay,
            delay:
                rememberDelay && Number.isFinite(value.delay)
                    ? Math.max(-3600, Math.min(3600, value.delay))
                    : 0,
        };
    }
    read() {
        const value = this.store.read(STORAGE_KEY, {});
        return this.normalize(
            value && typeof value === "object" && !Array.isArray(value)
                ? value
                : {},
        );
    }
    save(settings, rememberDelay) {
        this.store.write(
            STORAGE_KEY,
            this.normalize({ ...settings, rememberDelay }),
        );
    }
    warn() {
        if (this.warned) {
            return;
        }
        this.warned = true;
        this.onError(
            "Không lưu được tùy chỉnh phụ đề trong trình duyệt. Bạn vẫn có thể chỉnh cho lần xem hiện tại.",
        );
    }
}
