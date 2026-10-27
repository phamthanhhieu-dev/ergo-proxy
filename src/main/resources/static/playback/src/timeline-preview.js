// One muted decoder, active only while hovering. At most 24 small frames stay in memory.
export class TimelinePreview {
    constructor(video, seek, formatTime) {
        this.video = video;
        this.seek = seek;
        this.formatTime = formatTime;
        this.source = "";
        this.frames = new Map();
        this.target = 0;
        this.active = false;
        this.timer = null;
        this.decoder = document.createElement("video");
        this.decoder.muted = true;
        this.decoder.playsInline = true;
        this.decoder.preload = "metadata";
        this.element = document.createElement("div");
        this.element.className = "timeline-preview";
        this.element.hidden = true;
        this.element.setAttribute("role", "tooltip");
        this.canvas = document.createElement("canvas");
        this.canvas.width = 192;
        this.canvas.height = 108;
        this.context = this.canvas.getContext("2d");
        this.label = document.createElement("span");
        this.element.append(this.canvas, this.label);
        seek.parentElement.append(this.element);
        this.decoder.addEventListener("loadedmetadata", () => this.render());
        this.decoder.addEventListener("loadeddata", () => this.capture());
        this.decoder.addEventListener("seeked", () => this.capture());
        this.decoder.addEventListener("error", () => {
            this.canvas.hidden = true;
        });
        seek.addEventListener("pointermove", (event) => this.move(event));
        seek.addEventListener("pointerenter", (event) => this.move(event));
        seek.addEventListener("pointerleave", () => this.hide());
        seek.addEventListener("pointercancel", () => this.hide());
        video.addEventListener("emptied", () => this.reset());
        window.addEventListener("pagehide", () => this.reset());
    }
    move(event) {
        if (
            event.pointerType === "touch" ||
            this.seek.disabled ||
            !Number.isFinite(this.video.duration)
        ) {
            return;
        }
        const source = this.video.currentSrc || this.video.src;
        if (source !== this.source) {
            this.reset();
            this.source = source;
        }
        const rect = this.seek.getBoundingClientRect();
        const fraction = Math.max(
            0,
            Math.min(
                1,
                (event.clientX - rect.left - 8) / Math.max(1, rect.width - 16),
            ),
        );
        this.target = Math.min(
            fraction * this.video.duration,
            Math.max(0, this.video.duration - 0.05),
        );
        this.active = true;
        this.element.hidden = false;
        this.label.textContent = this.formatTime(this.target);
        const half = this.element.offsetWidth / 2;
        const left = Math.max(
            half,
            Math.min(rect.width - half, event.clientX - rect.left),
        );
        this.element.style.left = left + "px";
        this.canvas.hidden = true;
        const cached = this.frames.get(Math.floor(this.target));
        if (cached) {
            this.context.putImageData(cached, 0, 0);
            this.canvas.hidden = false;
        }
        clearTimeout(this.timer);
        this.timer = setTimeout(() => this.render(), 140);
    }
    render() {
        if (
            !this.active ||
            !this.source ||
            this.frames.has(Math.floor(this.target))
        ) {
            return;
        }
        if (this.decoder.getAttribute("src") !== this.source) {
            this.decoder.src = this.source;
            this.decoder.load();
            return;
        }
        if (!this.decoder.readyState || this.decoder.seeking) {
            return;
        }
        if (Math.abs(this.decoder.currentTime - this.target) > 0.05) {
            this.decoder.currentTime = this.target;
        } else {
            this.capture();
        }
    }
    capture() {
        if (
            !this.active ||
            this.decoder.readyState < 2 ||
            this.decoder.seeking
        ) {
            return;
        }
        if (Math.abs(this.decoder.currentTime - this.target) > 0.1) {
            this.render();
            return;
        }
        try {
            const width = this.decoder.videoWidth;
            const height = this.decoder.videoHeight;
            if (!width || !height) {
                return;
            }
            const scale = Math.min(
                this.canvas.width / width,
                this.canvas.height / height,
            );
            this.context.fillStyle = "#000";
            this.context.fillRect(0, 0, this.canvas.width, this.canvas.height);
            this.context.drawImage(
                this.decoder,
                (192 - width * scale) / 2,
                (108 - height * scale) / 2,
                width * scale,
                height * scale,
            );
            this.frames.set(
                Math.floor(this.target),
                this.context.getImageData(0, 0, 192, 108),
            );
            if (this.frames.size > 24) {
                this.frames.delete(this.frames.keys().next().value);
            }
            this.canvas.hidden = false;
        } catch {
            this.canvas.hidden = true;
        }
    }
    hide() {
        this.active = false;
        clearTimeout(this.timer);
        this.element.hidden = true;
        if (this.decoder.hasAttribute("src")) {
            this.decoder.removeAttribute("src");
            this.decoder.load();
        }
    }
    reset() {
        this.hide();
        this.source = "";
        this.frames.clear();
    }
}
