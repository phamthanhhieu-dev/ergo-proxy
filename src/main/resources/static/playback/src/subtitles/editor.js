import { fileUrl } from "../api";
import { SubtitleRenderer } from "./renderer";
import { SubtitlePreferences } from "./preferences";

const MAX_SUBTITLE_BYTES = 8 * 1024 * 1024;

export class SubtitleEditor {
    constructor(video, root, renderer = new SubtitleRenderer(video)) {
        this.root = root;
        root.innerHTML = `<div class="section-head"><h2>Phụ đề</h2><label class="file-button">Thêm file sub<input id="add-subtitle" type="file" accept=".ass,.ssa,.srt,.vtt" hidden /></label></div>
      <label>Bản phụ đề <select id="local-subtitles"><option value="">Tắt phụ đề</option></select></label>
      <div class="subtitle-settings">
        <label><input id="keep-ass-style" type="checkbox" checked /> Giữ kiểu ASS/SSA gốc</label>
        <label>Font chữ<input id="subtitle-font" value="Liberation Sans" maxlength="80" list="font-list" /><datalist id="font-list"><option value="Liberation Sans"></option><option value="Arial"></option><option value="Roboto"></option><option value="Noto Sans"></option></datalist></label>
        <label>Cỡ chữ <output id="font-scale-value">100%</output><input id="font-scale" type="range" min="50" max="250" value="100" step="5" /></label>
        <div class="subtitle-delay-setting">
          <div class="subtitle-delay-heading"><label for="subtitle-delay">Độ trễ phụ đề (giây)</label><label class="remember-delay"><input id="remember-subtitle-delay" type="checkbox" /> Lưu độ trễ</label></div>
          <input id="subtitle-delay" type="number" min="-3600" max="3600" value="0" step="0.1" />
        </div>
      </div>
      <div class="playback-actions"><button id="sub-earlier" class="secondary">Sớm hơn 0,25s</button><button id="sub-later" class="secondary">Muộn hơn 0,25s</button><label class="file-button secondary">Thêm font TTF/OTF<input id="add-font" type="file" accept=".ttf,.otf,.woff,.woff2" multiple hidden /></label><button id="reset-subtitles" class="text">Khôi phục mặc định</button><button id="export-subtitles" class="secondary">Xuất bản ASS đã chỉnh</button></div>
      <p class="hint">Độ trễ dương = sub xuất hiện muộn hơn; âm = sớm hơn. Thay đổi font/cỡ chữ sẽ bỏ chế độ giữ kiểu gốc. Font chưa có sẽ dùng font dự phòng; thêm file font để hiển thị đúng. Các chỉnh sửa chỉ áp dụng trên player; file sub gốc được giữ nguyên.</p>
      <p id="subtitle-editor-status" role="status" aria-live="polite"></p>
    `;
        this.renderer = renderer;
        this.preferences = new SubtitlePreferences((message) =>
            this.report(message),
        );
        this.version = 0;
        this.trackVersion = 0;
        this.tracks = [];
        this.fonts = [];
        this.timer = null;
        this.controller = null;
        this.setControls(this.preferences.read());
        this.renderer.settings = this.settings();
        this.bindControls();
    }
    get(id) {
        return this.root.querySelector("#" + id);
    }
    report(message) {
        this.get("subtitle-editor-status").textContent = message;
    }
    settings() {
        return {
            original: this.get("keep-ass-style").checked,
            font: this.get("subtitle-font").value.trim() || "Liberation Sans",
            scale: Number(this.get("font-scale").value),
            delay: Math.max(
                -3600,
                Math.min(3600, Number(this.get("subtitle-delay").value) || 0),
            ),
        };
    }
    setControls(value) {
        this.get("keep-ass-style").checked = value.original;
        this.get("subtitle-font").value = value.font;
        this.get("font-scale").value = String(value.scale);
        this.get("font-scale-value").textContent = value.scale + "%";
        this.get("remember-subtitle-delay").checked = value.rememberDelay;
        this.get("subtitle-delay").value = String(value.delay);
    }
    save() {
        this.preferences.save(
            this.settings(),
            this.get("remember-subtitle-delay").checked,
        );
    }
    async apply() {
        const settings = this.settings();
        this.save();
        this.get("font-scale-value").textContent = settings.scale + "%";
        try {
            await this.renderer.apply(settings);
        } catch (error) {
            this.report(error.message);
        }
    }
    async clear() {
        const version = ++this.version;
        this.trackVersion++;
        this.controller?.abort();
        clearTimeout(this.timer);
        try {
            await this.renderer.clear();
        } catch (error) {
            if (version === this.version) {
                this.report(error.message);
            }
        }
    }
    async prepare(entry) {
        const pending = this.clear();
        const version = this.version;
        await pending;
        if (version !== this.version) {
            return;
        }
        this.tracks = entry.subtitles.map((file) => ({
            ...file,
            url: file.url || fileUrl(file.id),
        }));
        this.fonts = entry.fonts.map((file) => file.url || fileUrl(file.id));
        if (!this.get("remember-subtitle-delay").checked) {
            this.get("subtitle-delay").value = "0";
        }
        this.renderer.settings = this.settings();
        this.get("local-subtitles").replaceChildren(
            new Option("Tắt phụ đề", ""),
            ...this.tracks.map(
                (file, index) => new Option(file.name, String(index)),
            ),
        );
        this.report("");
    }
    async loadFirst() {
        if (!this.tracks.length) {
            return;
        }
        this.get("local-subtitles").value = "0";
        await this.choose();
    }
    async choose() {
        const version = ++this.trackVersion;
        this.controller?.abort();
        this.controller = new AbortController();
        const value = this.get("local-subtitles").value;
        if (value === "") {
            await this.renderer.clear();
            if (version === this.trackVersion) {
                this.report("Đã tắt phụ đề.");
            }
            return;
        }
        const file = this.tracks[Number(value)];
        if (!file) {
            return;
        }
        this.report("Đang tải phụ đề…");
        try {
            let bytes = file.bytes;
            if (!bytes) {
                const response = await fetch(file.url, {
                    signal: this.controller.signal,
                });
                if (!response.ok) {
                    throw new Error(
                        "Không đọc được file phụ đề. Hãy chọn lại thư mục.",
                    );
                }
                if (
                    Number(response.headers.get("Content-Length") || 0) >
                    MAX_SUBTITLE_BYTES
                ) {
                    throw new Error("Phụ đề vượt giới hạn 8 MB.");
                }
                bytes = new Uint8Array(await response.arrayBuffer());
            }
            if (bytes.length > MAX_SUBTITLE_BYTES) {
                throw new Error("Phụ đề vượt giới hạn 8 MB.");
            }
            if (version !== this.trackVersion) {
                return;
            }
            await this.renderer.load(bytes, file.name, this.fonts);
            if (version === this.trackVersion) {
                this.report("Đang dùng: " + file.name);
            }
        } catch (error) {
            if (version === this.trackVersion && error.name !== "AbortError") {
                this.report("Không hiển thị được phụ đề: " + error.message);
            }
        }
    }
    refreshLayout() {
        return this.renderer.refreshLayout();
    }
    stepDelay(step) {
        this.get("subtitle-delay").value = String(
            Math.round((this.settings().delay + step) * 100) / 100,
        );
        void this.apply();
    }
    bindControls() {
        this.get("local-subtitles").onchange = () => this.choose();
        for (const id of [
            "keep-ass-style",
            "subtitle-font",
            "font-scale",
            "subtitle-delay",
        ]) {
            this.get(id).oninput = () => {
                if (id === "subtitle-font" || id === "font-scale") {
                    this.get("keep-ass-style").checked = false;
                }
                this.save();
                clearTimeout(this.timer);
                this.timer = setTimeout(() => this.apply(), 120);
            };
        }
        this.get("remember-subtitle-delay").onchange = () => this.save();
        this.get("sub-earlier").onclick = () => this.stepDelay(-0.25);
        this.get("sub-later").onclick = () => this.stepDelay(0.25);
        this.get("reset-subtitles").onclick = () => {
            clearTimeout(this.timer);
            this.setControls(this.preferences.normalize());
            void this.apply();
        };
        this.get("export-subtitles").onclick = () => {
            try {
                this.renderer.export();
                this.report("Đã xuất bản ASS đã chỉnh, file nguồn giữ nguyên.");
            } catch (error) {
                this.report(error.message);
            }
        };
        this.get("add-font").onchange = async (event) => {
            try {
                await this.renderer.addFonts(event.target.files);
                this.report(
                    "Đã nạp font. Nhập đúng tên font trong ô Font chữ.",
                );
            } catch (error) {
                this.report(error.message);
            } finally {
                event.target.value = "";
            }
        };
        this.get("add-subtitle").onchange = async (event) => {
            const file = event.target.files[0];
            if (!file) {
                return;
            }
            const version = this.version;
            try {
                if (file.size > MAX_SUBTITLE_BYTES) {
                    throw new Error("Phụ đề vượt giới hạn 8 MB.");
                }
                const bytes = new Uint8Array(await file.arrayBuffer());
                if (version !== this.version) {
                    return;
                }
                this.tracks.push({ name: file.name, bytes });
                this.get("local-subtitles").append(
                    new Option(file.name, String(this.tracks.length - 1)),
                );
                this.get("local-subtitles").value = String(
                    this.tracks.length - 1,
                );
                await this.choose();
            } catch (error) {
                if (version === this.version) {
                    this.report(error.message);
                }
            } finally {
                event.target.value = "";
            }
        };
    }
}
