const VIDEO = new Set(["mkv", "mp4", "m4v", "webm", "mov", "avi"]);
const SUBTITLE = new Set(["ass", "ssa", "srt", "vtt"]);
const FONT = new Set(["ttf", "otf", "woff", "woff2"]);
const extension = (name) => name.split(".").pop().toLowerCase();

// Blob URLs reference the selected files; videos are never uploaded or copied into a cache.
export class BrowserLibrary {
    constructor() {
        this.urls = [];
        this.input = null;
    }
    dispose() {
        for (const url of this.urls) {
            URL.revokeObjectURL(url);
        }
        this.urls = [];
    }
    describe(file) {
        const url = URL.createObjectURL(file);
        this.urls.push(url);
        return { id: url, name: file.name, size: file.size, url };
    }
    read(files) {
        const folders = new Map();
        for (const file of files) {
            const path = file.webkitRelativePath || file.name;
            const directory = path.includes("/")
                ? path.slice(0, path.lastIndexOf("/"))
                : "";
            if (!folders.has(directory)) {
                folders.set(directory, []);
            }
            folders.get(directory).push(file);
        }
        const videos = [];
        for (const [directory, siblings] of folders) {
            const path = directory.split("/").filter(Boolean);
            const folder = path.at(-1) || "Video trên thiết bị";
            const episodeFolder = /^EP\s*\d+$/i.test(folder);
            for (const file of siblings) {
                if (!VIDEO.has(extension(file.name))) {
                    continue;
                }
                videos.push({
                    series: episodeFolder
                        ? path.at(-2) || "Video trên thiết bị"
                        : folder,
                    episode: episodeFolder
                        ? folder
                        : file.name.replace(/\.[^.]+$/, ""),
                    video: this.describe(file),
                    subtitles: siblings
                        .filter((item) => SUBTITLE.has(extension(item.name)))
                        .map((item) => this.describe(item)),
                    fonts: siblings
                        .filter((item) => FONT.has(extension(item.name)))
                        .map((item) => this.describe(item)),
                });
            }
        }
        videos.sort((a, b) =>
            (a.series + "/" + a.episode + "/" + a.video.name).localeCompare(
                b.series + "/" + b.episode + "/" + b.video.name,
                "vi",
                { numeric: true },
            ),
        );
        return {
            local: true,
            name:
                files[0]?.webkitRelativePath?.split("/")[0] ||
                "Video trên thiết bị",
            videos,
        };
    }
    choose(folder, onLibrary, onMessage) {
        this.input?.remove();
        const input = document.createElement("input");
        this.input = input;
        input.type = "file";
        input.multiple = true;
        input.hidden = true;
        if (folder) {
            input.webkitdirectory = true;
        } else {
            input.accept = [...VIDEO, ...SUBTITLE, ...FONT]
                .map((value) => "." + value)
                .join(",");
        }
        input.onchange = () => {
            try {
                const files = [...input.files];
                if (!files.some((file) => VIDEO.has(extension(file.name)))) {
                    onMessage(
                        "Chưa chọn video được hỗ trợ. Có thể chọn video và sub cùng lúc.",
                    );
                    return;
                }
                // Release old URLs only after the player switches to the new library.
                const previous = this.urls;
                this.urls = [];
                try {
                    const library = this.read(files);
                    onLibrary(library);
                    for (const url of previous) {
                        URL.revokeObjectURL(url);
                    }
                } catch (error) {
                    this.dispose();
                    this.urls = previous;
                    throw error;
                }
            } catch (error) {
                onMessage(error.message);
            } finally {
                input.remove();
                this.input = null;
            }
        };
        input.oncancel = () => {
            input.remove();
            this.input = null;
        };
        document.body.append(input);
        input.click();
    }
}
