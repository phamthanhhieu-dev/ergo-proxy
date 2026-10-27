import { api, API_ROUTES, title } from "../common/anime-api.js";
import { saveBrowserFile, subtitleZip } from "../common/subtitle-zip.js";

const MAX_BYTES = 8 * 1024 * 1024;
const MAX_ARCHIVE_BYTES = 100 * 1024 * 1024;
const SUBTITLE = new Set(["ass", "ssa", "srt", "vtt"]);
const FONT = new Set(["ttf", "otf", "woff", "woff2"]);
const extension = (name) => name.split(".").pop().toLowerCase();
const safeName = (name) =>
    name
        .replace(/[\\/:*?"<>|\x00-\x1f]/g, "_")
        .replace(/[. ]+$/, "")
        .slice(0, 160) || "Anime";
const normalize = (name) =>
    name
        .normalize("NFKD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9]/g, "");
const get = (id) => document.getElementById(id);
export const subtitleDownloadMarkup = `<details class="subtitle-editor" open><summary>Tải phụ đề OpenSubtitles</summary>
<div class="playback-actions"><label>Nguồn <select id="online-server"><option>HD-2</option><option>HD-1</option></select></label><label>Âm thanh <select id="online-audio"><option value="sub">Tiếng Nhật</option><option value="dub">Tiếng Anh</option></select></label><label>Ngôn ngữ <select id="online-language"><option value="vi">Tiếng Việt</option><option value="en">Tiếng Anh</option><option value="ja">Tiếng Nhật</option></select></label><label>Mùa <input id="online-season" type="number" min="1" max="1000" value="1"></label></div>
<div class="playback-actions"><button id="online-search">Tìm sub tập đang xem</button><button id="online-all" class="secondary">Tải sub toàn bộ anime</button><button id="online-cancel" class="secondary" hidden>Hủy</button></div>
<select id="online-results" aria-label="Kết quả phụ đề"></select><div class="playback-actions"><button id="online-use" disabled>Tải sub đã chọn</button><button id="online-more" class="secondary" hidden>Trang tiếp</button></div>
<p id="online-sub-status" role="status"></p><p class="hint">Chọn thư mục phụ đề để đọc Tên anime/EP…/file sub và tự áp dụng khi đổi tập. Sau khi mở lại trang cần chọn lại thư mục. Nếu tải ZIP, giải nén rồi chọn thư mục đó.</p></details>`;

export class OnlineSubtitles {
    constructor({ editor, selection, currentEpisode, episodes, onChanged }) {
        Object.assign(this, {
            editor,
            selection,
            currentEpisode,
            episodes,
            onChanged,
        });
        this.library = new Map();
        this.urls = [];
        this.results = [];
        this.page = 1;
        this.version = 0;
        this.applyVersion = 0;
        this.busy = false;
        this.controller = null;
        get("online-search").onclick = () => void this.search();
        get("online-more").onclick = () => void this.search(this.page + 1);
        get("online-use").onclick = () => void this.downloadOne();
        get("online-all").onclick = () => void this.downloadAll();
        get("online-cancel").onclick = () => this.controller?.abort();
        const addFile = this.editor.get("add-subtitle");
        const addSubtitle = addFile.onchange;
        addFile.onchange = async (event) => {
            const number = this.currentEpisode();
            await addSubtitle(event);
            if (number === this.currentEpisode()) {
                this.library.set(number, {
                    subtitles: [...this.editor.tracks],
                    fonts: [...(this.library.get(number)?.fonts || [])],
                });
                this.onChanged();
            }
        };
    }
    report(message) {
        get("online-sub-status").textContent = message;
    }
    setBusy(busy) {
        this.busy = busy;
        for (const id of [
            "online-search",
            "online-all",
            "online-language",
            "online-season",
            "online-results",
        ]) {
            get(id).disabled = busy;
        }
        get("online-more").disabled = busy;
        get("online-use").disabled = busy || !this.results.length;
        get("online-cancel").hidden = !busy;
    }
    count(number) {
        return this.library.get(number)?.subtitles.length || 0;
    }
    cancelSearch() {
        ++this.version;
        if (!this.busy) {
            this.controller?.abort();
        }
        this.results = [];
        get("online-results").replaceChildren();
        get("online-use").disabled = true;
        get("online-more").hidden = true;
    }
    settings() {
        const season = Number(get("online-season").value);
        if (!Number.isInteger(season) || season < 1 || season > 1000) {
            throw new Error("Mùa không hợp lệ.");
        }
        return {
            q: title(this.selection.anime),
            language: get("online-language").value,
            season,
            movie: this.selection.anime.format === "MOVIE",
        };
    }
    async find(number, settings, page = 1) {
        const query = new URLSearchParams({
            ...settings,
            episode: number,
            page,
        });
        return api(API_ROUTES.subtitle + "/search?" + query, {
            signal: this.controller.signal,
        });
    }
    async search(page = 1) {
        if (this.busy || !this.currentEpisode()) {
            return;
        }
        this.controller?.abort();
        this.controller = new AbortController();
        const version = ++this.version;
        this.results = [];
        get("online-use").disabled = true;
        get("online-results").replaceChildren();
        this.report("Đang tìm phụ đề…");
        try {
            const data = await this.find(
                this.currentEpisode(),
                this.settings(),
                page,
            );
            if (version !== this.version) {
                return;
            }
            this.results = data.results;
            this.page = page;
            get("online-results").replaceChildren(
                ...this.results.map(
                    (file, index) =>
                        new Option(
                            `${file.title} • ${file.fileName || file.release}`,
                            String(index),
                        ),
                ),
            );
            get("online-more").hidden = page >= data.totalPages;
            get("online-use").disabled = !this.results.length;
            this.report(
                this.results.length
                    ? "Chọn bản sub rồi tải. Sub đã tải xuất hiện ở Bản phụ đề bên dưới."
                    : "Không tìm thấy phụ đề.",
            );
        } catch (error) {
            if (version === this.version && error.name !== "AbortError") {
                this.report(error.message);
            }
        }
    }
    async fetchFile(candidate) {
        const response = await fetch(API_ROUTES.subtitle + "/download", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fileId: candidate.fileId }),
            signal: this.controller.signal,
        });
        if (
            !response.ok ||
            response.headers.get("Content-Type")?.includes("application/json")
        ) {
            const error = await response.json().catch(() => null);
            throw new Error(
                error?.message || `Không tải được sub: HTTP ${response.status}`,
            );
        }
        if (Number(response.headers.get("Content-Length")) > MAX_BYTES) {
            throw new Error("Sub vượt giới hạn 8 MB.");
        }
        const bytes = new Uint8Array(await response.arrayBuffer());
        if (bytes.length > MAX_BYTES) {
            throw new Error("Sub vượt giới hạn 8 MB.");
        }
        const disposition = response.headers.get("Content-Disposition") || "";
        let name =
            disposition.match(/filename="([^"]+)"/i)?.[1] || candidate.fileName;
        const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i);
        if (encoded) {
            try {
                name = decodeURIComponent(encoded[1]);
            } catch {
                /* Preserve plain source name. */
            }
        }
        if (
            !name ||
            !SUBTITLE.has(extension(name)) ||
            /[\\/\x00-\x1f]/.test(name)
        ) {
            throw new Error(
                "Nguồn trả về tên hoặc định dạng phụ đề không hợp lệ.",
            );
        }
        return { name, bytes };
    }
    remember(number, file) {
        let entry = this.library.get(number);
        if (!entry) {
            entry = { subtitles: [], fonts: [] };
            this.library.set(number, entry);
        }
        const index = entry.subtitles.findIndex(
            (item) => item.name === file.name,
        );
        if (index >= 0) {
            entry.subtitles[index] = file;
        } else {
            entry.subtitles.push(file);
        }
        this.onChanged();
    }
    async apply(number) {
        const version = ++this.applyVersion;
        const entry = this.library.get(number) || { subtitles: [], fonts: [] };
        await this.editor.prepare(entry);
        if (version !== this.applyVersion || number !== this.currentEpisode()) {
            return;
        }
        await this.editor.loadFirst();
    }
    async downloadOne() {
        if (this.busy) {
            return;
        }
        const candidate = this.results[Number(get("online-results").value)];
        if (!candidate) {
            return;
        }
        const number = this.currentEpisode();
        this.controller?.abort();
        this.controller = new AbortController();
        this.setBusy(true);
        try {
            this.report(`Đang tải sub tập ${number}…`);
            const file = await this.fetchFile(candidate);
            this.remember(number, file);
            saveBrowserFile(new Blob([file.bytes]), file.name);
            if (number === this.currentEpisode()) {
                await this.apply(number);
            }
            this.report(
                `Đã tải sub tập ${number}. Có thể chọn bản đã tải ở Bản phụ đề.`,
            );
        } catch (error) {
            this.report(
                error.name === "AbortError" ? "Đã hủy tải sub." : error.message,
            );
        } finally {
            this.setBusy(false);
        }
    }
    async downloadAll() {
        if (this.busy || !this.episodes().length) {
            return;
        }
        let target = null;
        const animeName = safeName(title(this.selection.anime));
        try {
            // Request the directory while the click still carries browser user activation.
            if (window.showDirectoryPicker) {
                target = await window.showDirectoryPicker({
                    mode: "readwrite",
                    id: "ergo-subtitles",
                });
            }
        } catch (error) {
            if (error.name === "AbortError") {
                return;
            }
            this.report("Không mở được thư mục ghi file, sẽ tải ZIP.");
        }
        this.controller?.abort();
        this.controller = new AbortController();
        this.setBusy(true);
        const archive = [];
        const reports = [];
        let completed = 0;
        let totalBytes = 0;
        let failure = "";
        try {
            const settings = this.settings();
            const root = target
                ? target.name === animeName
                    ? target
                    : await target.getDirectoryHandle(animeName, {
                          create: true,
                      })
                : null;
            for (const episode of this.episodes()) {
                if (this.controller.signal.aborted) {
                    throw new DOMException("Đã hủy", "AbortError");
                }
                const number = episode.episode_number;
                this.report(
                    `Đang tìm và tải sub tập ${number} • ${completed}/${this.episodes().length} tập`,
                );
                const data = await this.find(number, settings);
                if (!data.results.length) {
                    reports.push(`EP${number}: Không tìm thấy phụ đề.`);
                    continue;
                }
                const file = await this.fetchFile(data.results[0]);
                totalBytes += file.bytes.length;
                if (totalBytes > MAX_ARCHIVE_BYTES) {
                    throw new Error("Tổng phụ đề vượt giới hạn 100 MB.");
                }
                if (root) {
                    const directory = await root.getDirectoryHandle(
                        `EP${number}`,
                        { create: true },
                    );
                    const handle = await directory.getFileHandle(file.name, {
                        create: true,
                    });
                    const writable = await handle.createWritable();
                    try {
                        await writable.write(file.bytes);
                        await writable.close();
                    } catch (error) {
                        await writable.abort().catch(() => {});
                        throw error;
                    }
                } else {
                    archive.push({
                        path: `${animeName}/EP${number}/${file.name}`,
                        bytes: file.bytes,
                    });
                }
                this.remember(number, file);
                completed++;
                if (number === this.currentEpisode()) {
                    await this.apply(number);
                }
            }
        } catch (error) {
            failure = error.name === "AbortError" ? "Đã hủy." : error.message;
            reports.push(failure + " Các tập chưa xử lý không được tải.");
        } finally {
            if (!target && archive.length) {
                if (reports.length) {
                    archive.push({
                        path: `${animeName}/Bao-cao.txt`,
                        bytes: new TextEncoder().encode(reports.join("\n")),
                    });
                }
                saveBrowserFile(
                    subtitleZip(archive),
                    `${animeName}.subtitles.zip`,
                );
            }
            this.report(
                `Đã tải ${completed}/${this.episodes().length} tập.${reports.length ? " " + reports.join(" ") : ""}${!target && completed ? " Giải nén ZIP rồi chọn thư mục phụ đề để dùng lại." : ""}`,
            );
            this.setBusy(false);
        }
    }
    aliases() {
        const anime = this.selection.anime;
        const names =
            typeof anime.title === "object"
                ? Object.values(anime.title)
                : [anime.title];
        return [title(anime), ...names, anime.anime_id?.replace(/-[^-]+$/, "")]
            .filter((value) => typeof value === "string")
            .map(normalize);
    }
    async scanServer() {
        this.report("Đang quét thư viện phụ đề đã cấu hình…");
        try {
            const entries = await api("/api/v1/playback/subtitles");
            const aliases = this.aliases();
            let count = 0;
            for (const entry of entries) {
                const match = /^EP\s*(\d+)$/i.exec(entry.episode);
                if (!match || !aliases.includes(normalize(entry.series))) {
                    continue;
                }
                const number = Number(match[1]);
                if (
                    !this.episodes().some(
                        (item) => item.episode_number === number,
                    )
                ) {
                    continue;
                }
                this.library.set(number, {
                    subtitles: entry.subtitles,
                    fonts: entry.fonts,
                });
                count++;
            }
            this.onChanged();
            if (this.currentEpisode()) {
                await this.apply(this.currentEpisode());
            }
            this.report(
                count
                    ? `Đã tìm thấy phụ đề của ${count} tập trong thư viện. Tự áp dụng khi đổi tập.`
                    : "Chưa có sub tương ứng trong thư viện đã cấu hình. Có thể chọn thư mục trên thiết bị hoặc tải sub.",
            );
        } catch (error) {
            this.report(error.message);
        }
    }
    chooseFolder() {
        const input = document.createElement("input");
        input.type = "file";
        input.webkitdirectory = true;
        input.multiple = true;
        input.hidden = true;
        input.onchange = async () => {
            try {
                await this.readFolder([...input.files]);
            } catch (error) {
                this.report(error.message);
            } finally {
                input.remove();
            }
        };
        input.oncancel = () => input.remove();
        document.body.append(input);
        input.click();
    }
    async readFolder(files) {
        if (files.length > 10000) {
            throw new Error("Thư mục vượt giới hạn 10.000 file.");
        }
        const aliases = this.aliases();
        const groups = new Map();
        let skipped = 0;
        for (const file of files) {
            const parts = (file.webkitRelativePath || file.name).split("/");
            const episodeIndex = parts.findLastIndex((part) =>
                /^EP\s*\d+$/i.test(part),
            );
            if (episodeIndex < 0) {
                continue;
            }
            const parent = parts[episodeIndex - 1];
            // If selecting a parent library, only use the chosen anime; selecting the anime root explicitly also works.
            if (parent && !aliases.includes(normalize(parent))) {
                skipped++;
                continue;
            }
            const number = Number(parts[episodeIndex].match(/\d+/)[0]);
            if (
                !this.episodes().some((item) => item.episode_number === number)
            ) {
                continue;
            }
            const type = extension(file.name);
            if (!SUBTITLE.has(type) && !FONT.has(type)) {
                continue;
            }
            if (file.size > MAX_BYTES) {
                skipped++;
                continue;
            }
            const directory = parts.slice(0, -1).join("/");
            if (!groups.has(directory)) {
                groups.set(directory, { number, subtitles: [], fonts: [] });
            }
            groups
                .get(directory)
                [SUBTITLE.has(type) ? "subtitles" : "fonts"].push(file);
        }
        if (![...groups.values()].some((group) => group.subtitles.length)) {
            throw new Error(
                "Không tìm thấy sub tương ứng. Chọn thư mục có cấu trúc Tên anime/EP1/file sub, EP2/…",
            );
        }
        const scanned = new Map();
        for (const group of groups.values()) {
            let entry = scanned.get(group.number);
            if (!entry) {
                entry = { subtitles: [], fonts: [] };
                scanned.set(group.number, entry);
            }
            for (const file of group.subtitles.sort((a, b) =>
                a.name.localeCompare(b.name, "vi", { numeric: true }),
            )) {
                const url = URL.createObjectURL(file);
                this.urls.push(url);
                entry.subtitles.push({ name: file.name, url });
            }
            for (const file of group.fonts) {
                const url = URL.createObjectURL(file);
                this.urls.push(url);
                entry.fonts.push({ url });
            }
        }
        for (const [number, entry] of scanned) {
            this.library.set(number, entry);
        }
        this.onChanged();
        await this.apply(this.currentEpisode());
        this.report(
            `Đã đọc phụ đề của ${scanned.size} tập. Tự áp dụng sub đầu tiên khi đổi tập.${skipped ? ` Bỏ qua ${skipped} file không phù hợp.` : ""}`,
        );
    }
    dispose() {
        this.controller?.abort();
        for (const url of this.urls) {
            URL.revokeObjectURL(url);
        }
        this.urls = [];
    }
}
