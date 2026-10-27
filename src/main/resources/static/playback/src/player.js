import { fileUrl } from "./api";

/** Truyền nguyên file từ ổ đĩa; giải mã video thuộc về trình duyệt. */
export class VideoPlayer {
    constructor(video, report, progress) {
        this.video = video;
        this.report = report;
        this.progress = progress;
        this.pictureVersion = 0;
    }
    stop() {
        this.pictureVersion++;
        this.progress?.close();
        this.video.pause();
        this.video.removeAttribute("src");
        this.video.load();
    }
    async restorePicture() {
        const video = this.video;
        if (!video.currentSrc || video.readyState < 2) {
            return;
        }
        const version = ++this.pictureVersion;
        const source = video.currentSrc;
        const playing = !video.paused;
        const time = video.currentTime;
        const display = video.style.display;
        // Tear down the stale fullscreen compositor layer without replacing/reloading the source.
        video.pause();
        video.style.display = "none";
        void video.offsetHeight;
        await new Promise((resolve) => requestAnimationFrame(resolve));
        video.style.display = display;
        void video.offsetHeight;
        if (version !== this.pictureVersion || source !== video.currentSrc) {
            return;
        }
        try {
            // A same-position seek also redraws the decoded frame while paused.
            video.currentTime = time;
            if (playing) {
                await video.play();
            }
        } catch (error) {
            if (
                version === this.pictureVersion &&
                error.name !== "AbortError"
            ) {
                this.report(error.message);
            }
        }
    }
    async open(entry) {
        this.stop();
        const source = entry.video.url || fileUrl(entry.video.id);
        this.progress?.open(entry, source);
        this.video.src = source;
        this.video.load();
        this.report(
            entry.video.url
                ? "Đang đọc video trên thiết bị • Không tải video lên server."
                : "Đang đọc thư viện trên máy host • Hỗ trợ tua • Không chuyển mã video.",
        );
    }
}
