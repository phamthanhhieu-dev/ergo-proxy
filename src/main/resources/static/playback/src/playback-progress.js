const STORAGE_KEY = 'ergo.playback.progress.v1';
const SAVE_INTERVAL_MS = 5000;
const MAX_ENTRIES = 500;

// Store metadata only. File name + byte size survives UUID changes and new blob URLs.
export class PlaybackProgress {
    constructor(video, { onOffer, onDismiss, onError }) {
        this.video = video;
        this.onOffer = onOffer;
        this.onDismiss = onDismiss;
        this.onError = onError;
        this.entry = null;
        this.pending = null;
        this.ready = false;
        this.started = false;
        this.lastSaved = 0;
        this.storageWarning = false;
        for (const event of ['loadedmetadata', 'durationchange']) {
            video.addEventListener(event, () => this.metadata());
        }
        video.addEventListener('timeupdate', () => {
            if (performance.now() - this.lastSaved >= SAVE_INTERVAL_MS) {
                this.flush();
            }
        });
        for (const event of ['pause', 'ended']) {
            video.addEventListener(event, () => this.flush());
        }
        video.addEventListener('seeked', () => {
            if (this.ready && !this.pending) {
                this.started = true;
                this.flush();
            }
        });
        video.addEventListener('play', () => {
            if (this.pending) {
                video.pause();
                return;
            }
            this.started = true;
        });
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'hidden') {
                this.flush();
            }
        });
        window.addEventListener('pagehide', () => this.flush());
    }
    read() {
        try {
            const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
            if (
                value &&
                value.version === 1 &&
                value.items &&
                typeof value.items === 'object' &&
                !Array.isArray(value.items)
            ) {
                return value;
            }
        } catch {
            this.warn();
        }
        return { version: 1, items: {} };
    }
    warn() {
        if (this.storageWarning) {
            return;
        }
        this.storageWarning = true;
        this.onError('Không lưu được tiến độ xem trong trình duyệt. Video vẫn phát bình thường.');
    }
    open(entry, source) {
        this.entry = entry;
        this.source = new URL(source, document.baseURI).href;
        this.key = JSON.stringify([entry.video.name, entry.video.size || 0]);
        this.ready = false;
        this.started = false;
        this.pending = null;
        this.lastSaved = 0;
        this.onDismiss();
    }
    metadata() {
        if (!this.entry || this.ready || this.video.currentSrc !== this.source) {
            return;
        }
        const duration = this.video.duration;
        if (!Number.isFinite(duration) || duration <= 0) {
            return;
        }
        this.ready = true;
        const saved = this.read().items[this.key];
        if (!saved || !Number.isFinite(saved.position) || !Number.isFinite(saved.duration)) {
            return;
        }
        if (Math.abs(saved.duration - duration) > Math.max(2, duration * 0.005)) {
            return;
        }
        if (saved.completed || saved.position < 1 || saved.position >= duration - this.endWindow()) {
            return;
        }
        this.pending = saved;
        this.video.pause();
        this.onOffer(saved.position);
    }
    endWindow() {
        return Math.min(10, this.video.duration * 0.02);
    }
    flush() {
        if (!this.entry || !this.ready || !this.started || this.pending || this.video.currentSrc !== this.source) {
            return;
        }
        const position = this.video.currentTime;
        const duration = this.video.duration;
        if (!Number.isFinite(position) || !Number.isFinite(duration) || duration <= 0) {
            return;
        }
        const value = this.read();
        value.items[this.key] = {
            name: this.entry.video.name,
            series: this.entry.series,
            episode: this.entry.episode,
            position,
            duration,
            completed: this.video.ended || position >= duration - this.endWindow(),
            updatedAt: Date.now()
        };
        const keys = Object.keys(value.items);
        if (keys.length > MAX_ENTRIES) {
            keys.sort((a, b) => (Number(value.items[a]?.updatedAt) || 0) - (Number(value.items[b]?.updatedAt) || 0));
            for (const key of keys.slice(0, keys.length - MAX_ENTRIES)) {
                delete value.items[key];
            }
        }
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
            this.lastSaved = performance.now();
        } catch {
            this.warn();
        }
    }
    async choose(resume) {
        if (!this.pending || !this.ready) {
            return;
        }
        const position = resume ? this.pending.position : 0;
        this.pending = null;
        this.started = true;
        this.onDismiss();
        try {
            this.video.currentTime = Math.max(0, Math.min(position, this.video.duration));
            this.flush();
            await this.video.play();
        } catch (error) {
            if (error.name !== 'AbortError') {
                this.onError(error.message);
            }
        }
    }
    close() {
        this.flush();
        this.entry = null;
        this.pending = null;
        this.ready = false;
        this.started = false;
        this.onDismiss();
    }
}
