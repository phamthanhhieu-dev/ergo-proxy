import { TimelinePreview } from './timeline-preview';

const icons = {
    play: '<svg viewBox="0 0 24 24"><path d="m8 5 11 7-11 7Z" fill="currentColor" stroke="none"/></svg>',
    pause: '<svg viewBox="0 0 24 24"><path d="M8 5v14M16 5v14"/></svg>',
    sound: '<svg viewBox="0 0 24 24"><path d="M10 5 5 9H2v6h3l5 4ZM14 8c3 2 3 6 0 8M17 5c6 4 6 10 0 14"/></svg>',
    muted: '<svg viewBox="0 0 24 24"><path d="M10 5 5 9H2v6h3l5 4ZM15 9l6 6m0-6-6 6"/></svg>',
    expand: '<svg viewBox="0 0 24 24"><path d="M9 3H3v6m12-6h6v6M3 15v6h6m6 0h6v-6"/></svg>',
    shrink: '<svg viewBox="0 0 24 24"><path d="M3 9h6V3m6 0v6h6M9 21v-6H3m18 0h-6v6"/></svg>'
};
function clock(seconds) {
    if (!Number.isFinite(seconds)) {
        return '0:00';
    }
    const total = Math.floor(Math.max(0, seconds));
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor(total / 60) % 60;
    return (
        (hours ? hours + ':' + String(minutes).padStart(2, '0') : String(minutes)) +
        ':' +
        String(total % 60).padStart(2, '0')
    );
}
export class PlayerControls {
    constructor(video, { onLayout, onError, onRestorePicture }) {
        this.video = video;
        this.stage = video.parentElement;
        this.onLayout = onLayout;
        this.onError = onError;
        this.onRestorePicture = onRestorePicture;
        this.expanded = false;
        this.scrubbing = false;
        this.hideTimer = null;
        this.layoutFrame = null;
        this.savedViewport = null;
        this.viewportVersion = 0;
        this.scrollFrame = null;
        this.savedOverflow = '';
        this.orientationLocked = false;
        const controls = document.createElement('div');
        controls.className = 'media-controls';
        controls.innerHTML = `
          <div class="media-timeline"><input class="media-seek" type="range" min="0" max="0" step="0.1" value="0" aria-label="Vị trí phát" disabled /></div>
          <div class="media-buttons">
            <button type="button" class="media-play" aria-label="Phát" title="Phát / Tạm dừng (Space)">${icons.play}</button>
            <button type="button" class="media-mute" aria-label="Tắt tiếng" title="Bật / Tắt tiếng (M)">${icons.sound}</button>
            <input class="media-volume" type="range" min="0" max="1" step="0.05" value="1" aria-label="Âm lượng" />
            <span class="media-time" aria-live="off">0:00 / 0:00</span>
            <span class="media-spacer"></span>
            <div class="media-jumps" aria-label="Tua nhanh">
              <button type="button" data-seek="-10" aria-label="Lùi 10 giây" title="Lùi 10 giây">−10s</button>
              <button type="button" data-seek="-5" aria-label="Lùi 5 giây" title="Lùi 5 giây">−5s</button>
              <button type="button" data-seek="5" aria-label="Tiến 5 giây" title="Tiến 5 giây">+5s</button>
              <button type="button" data-seek="10" aria-label="Tiến 10 giây" title="Tiến 10 giây">+10s</button>
            </div>
            <select class="media-speed" aria-label="Tốc độ phát" title="Tốc độ phát"><option value="0.5">0.5×</option><option value="0.75">0.75×</option><option value="1" selected>1×</option><option value="1.25">1.25×</option><option value="1.5">1.5×</option><option value="2">2×</option></select>
            <button type="button" class="media-fullscreen" aria-label="Toàn màn hình" title="Toàn màn hình (F)">${icons.expand}</button>
          </div>`;
        this.controls = controls;
        this.stage.append(controls);
        this.stage.tabIndex = 0;
        this.stage.setAttribute('role', 'group');
        this.stage.setAttribute('aria-label', 'Trình phát video');
        this.play = controls.querySelector('.media-play');
        this.mute = controls.querySelector('.media-mute');
        this.seek = controls.querySelector('.media-seek');
        this.preview = new TimelinePreview(video, this.seek, clock);
        this.volume = controls.querySelector('.media-volume');
        this.time = controls.querySelector('.media-time');
        this.fullscreen = controls.querySelector('.media-fullscreen');
        this.play.onclick = () => this.togglePlay();
        this.mute.onclick = () => {
            video.muted = !video.muted;
        };
        this.volume.oninput = () => {
            video.volume = Number(this.volume.value);
            video.muted = video.volume === 0;
        };
        controls.querySelector('.media-speed').onchange = (event) => {
            video.playbackRate = Number(event.target.value);
            video.defaultPlaybackRate = video.playbackRate;
        };
        this.fullscreen.onclick = () => this.toggleFullscreen();
        for (const button of controls.querySelectorAll('[data-seek]')) {
            button.onclick = () => this.jump(video.currentTime + Number(button.dataset.seek));
        }
        this.seek.oninput = () => {
            this.scrubbing = true;
            this.updateTime(Number(this.seek.value));
        };
        this.seek.onchange = () => {
            this.jump(Number(this.seek.value));
            this.scrubbing = false;
        };
        this.seek.addEventListener('pointercancel', () => {
            this.scrubbing = false;
            this.update();
        });
        for (const event of ['loadedmetadata', 'durationchange', 'timeupdate', 'emptied', 'seeked']) {
            video.addEventListener(event, () => this.update());
        }
        for (const event of ['play', 'pause', 'ended', 'waiting', 'playing']) {
            video.addEventListener(event, () => {
                this.updatePlay();
                this.show();
            });
        }
        video.addEventListener('volumechange', () => this.updateVolume());
        video.addEventListener('error', () => this.show());
        this.stage.addEventListener('pointermove', () => this.show());
        this.stage.addEventListener('pointerdown', () => this.show());
        this.stage.addEventListener('focusin', () => this.show());
        this.stage.addEventListener('keydown', (event) => this.keyboard(event));
        video.addEventListener('click', (event) => {
            if (event.pointerType === 'touch' && !video.paused) {
                this.show();
                return;
            }
            void this.togglePlay();
        });
        video.addEventListener('dblclick', () => this.toggleFullscreen());
        document.addEventListener('fullscreenchange', () => this.fullscreenChanged());
        document.addEventListener('webkitfullscreenchange', () => this.fullscreenChanged());
        document.addEventListener('keydown', (event) => {
            if (event.key !== 'Escape') {
                return;
            }
            if (this.stage.classList.contains('is-expanded')) {
                this.exitExpanded();
            } else if ((document.fullscreenElement || document.webkitFullscreenElement) === this.stage) {
                void this.toggleFullscreen();
            }
        });
        window.addEventListener('pagehide', () => this.unlockOrientation());
        window.addEventListener('resize', () => this.refreshLayout());
        this.resizeObserver = new ResizeObserver(() => this.refreshLayout());
        this.resizeObserver.observe(this.stage);
        video.controls = false;
        this.update();
        this.updateVolume();
        this.updatePlay();
    }
    async togglePlay() {
        try {
            if (this.video.paused || this.video.ended) {
                await this.video.play();
            } else {
                this.video.pause();
            }
        } catch (error) {
            this.onError(error.message);
        }
    }
    jump(value) {
        if (!Number.isFinite(this.video.duration)) {
            return;
        }
        this.video.currentTime = Math.max(0, Math.min(value, this.video.duration));
        this.show();
    }
    updateTime(value = this.video.currentTime) {
        this.time.textContent = clock(value) + ' / ' + clock(this.video.duration);
    }
    update() {
        const duration = this.video.duration;
        this.seek.disabled = !Number.isFinite(duration) || duration <= 0;
        this.seek.max = Number.isFinite(duration) ? String(duration) : '0';
        if (!this.scrubbing) {
            this.seek.value = String(this.video.currentTime || 0);
            this.updateTime();
        }
    }
    updatePlay() {
        const paused = this.video.paused;
        this.play.innerHTML = paused ? icons.play : icons.pause;
        this.play.setAttribute('aria-label', paused ? 'Phát' : 'Tạm dừng');
    }
    updateVolume() {
        const muted = this.video.muted || this.video.volume === 0;
        this.mute.innerHTML = muted ? icons.muted : icons.sound;
        this.mute.setAttribute('aria-label', muted ? 'Bật tiếng' : 'Tắt tiếng');
        this.volume.value = String(muted ? 0 : this.video.volume);
    }
    show() {
        this.stage.dataset.controlsHidden = 'false';
        clearTimeout(this.hideTimer);
        if (!this.video.paused) {
            this.hideTimer = setTimeout(() => {
                if (!this.scrubbing && !this.preview.active && !this.controls.querySelector(':focus-visible')) {
                    this.stage.dataset.controlsHidden = 'true';
                }
            }, 3000);
        }
    }
    keyboard(event) {
        if (event.target.closest('input, select, button')) {
            return;
        }
        const key = event.key.toLowerCase();
        if (![' ', 'k', 'f', 'm', 'arrowleft', 'arrowright', 'arrowup', 'arrowdown'].includes(key)) {
            return;
        }
        event.preventDefault();
        this.show();
        switch (key) {
            case ' ':
            case 'k':
                void this.togglePlay();
                break;
            case 'f':
                void this.toggleFullscreen();
                break;
            case 'm':
                this.video.muted = !this.video.muted;
                break;
            case 'arrowleft':
                this.jump(this.video.currentTime - 5);
                break;
            case 'arrowright':
                this.jump(this.video.currentTime + 5);
                break;
            case 'arrowup':
                this.video.volume = Math.min(1, this.video.volume + 0.05);
                break;
            case 'arrowdown':
                this.video.volume = Math.max(0, this.video.volume - 0.05);
                break;
        }
    }
    async toggleFullscreen() {
        try {
            if (this.stage.classList.contains('is-expanded')) {
                this.exitExpanded();
                return;
            }
            const active = document.fullscreenElement || document.webkitFullscreenElement;
            if (active) {
                if (document.exitFullscreen) {
                    await document.exitFullscreen();
                } else if (document.webkitExitFullscreen) {
                    document.webkitExitFullscreen();
                }
                return;
            }
            this.saveViewport();
            const enter = this.stage.requestFullscreen || this.stage.webkitRequestFullscreen;
            if (enter) {
                try {
                    await enter.call(this.stage);
                    await this.lockLandscape();
                } catch {
                    this.enterExpanded();
                }
            } else {
                this.enterExpanded();
            }
        } catch (error) {
            this.onError(error.message);
        }
    }
    enterExpanded() {
        this.saveViewport();
        this.savedOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        this.stage.classList.add('is-expanded');
        void this.lockLandscape();
        this.fullscreenChanged();
    }
    exitExpanded() {
        if (!this.stage.classList.contains('is-expanded')) {
            return;
        }
        this.stage.classList.remove('is-expanded');
        document.body.style.overflow = this.savedOverflow;
        this.fullscreenChanged();
    }
    saveViewport() {
        if (this.savedViewport) {
            return;
        }
        this.viewportVersion++;
        cancelAnimationFrame(this.scrollFrame);
        this.savedViewport = { x: window.scrollX, y: window.scrollY, focus: document.activeElement };
    }
    restoreViewport() {
        const saved = this.savedViewport;
        if (!saved || this.expanded) {
            return;
        }
        this.savedViewport = null;
        const version = this.viewportVersion;
        const restore = () => {
            if (version !== this.viewportVersion || this.expanded) {
                return;
            }
            if (saved.focus?.isConnected) {
                saved.focus.focus({ preventScroll: true });
            }
            window.scrollTo({ left: saved.x, top: saved.y, behavior: 'instant' });
        };
        restore();
        // Fullscreen focus restoration can run after the layout/compositor repaint.
        this.scrollFrame = requestAnimationFrame(restore);
    }
    fullscreenChanged() {
        const wasExpanded = this.expanded;
        const expanded =
            (document.fullscreenElement || document.webkitFullscreenElement) === this.stage ||
            this.stage.classList.contains('is-expanded');
        this.expanded = expanded;
        this.preview.hide();
        if (!expanded) {
            this.unlockOrientation();
            if (wasExpanded) {
                this.refreshLayout(true);
            }
        }
        this.fullscreen.innerHTML = expanded ? icons.shrink : icons.expand;
        this.fullscreen.setAttribute('aria-label', expanded ? 'Thoát toàn màn hình' : 'Toàn màn hình');
        this.fullscreen.title = expanded ? 'Thoát toàn màn hình (Esc / F)' : 'Toàn màn hình (F)';
        this.show();
        if (expanded || !wasExpanded) {
            this.refreshLayout();
        }
    }
    async lockLandscape() {
        const touchDevice = navigator.maxTouchPoints > 0 || window.matchMedia('(pointer: coarse)').matches;
        if (!touchDevice || !screen.orientation?.lock) {
            return;
        }
        try {
            await screen.orientation.lock('landscape');
            this.orientationLocked = true;
            const expanded =
                (document.fullscreenElement || document.webkitFullscreenElement) === this.stage ||
                this.stage.classList.contains('is-expanded');
            if (!expanded) {
                this.unlockOrientation();
            }
        } catch {
            /* iOS/trình duyệt hoặc thiết lập hệ điều hành có thể không cho khóa hướng. */
        }
    }
    unlockOrientation() {
        if (!this.orientationLocked) {
            return;
        }
        try {
            screen.orientation?.unlock();
        } catch {
            /* Không đổi hướng nếu thiết bị không hỗ trợ. */
        }
        this.orientationLocked = false;
    }
    refreshLayout(restorePicture = false) {
        this.restorePending = this.restorePending || restorePicture;
        cancelAnimationFrame(this.layoutFrame);
        this.layoutFrame = requestAnimationFrame(() => {
            this.layoutFrame = requestAnimationFrame(async () => {
                const restore = this.restorePending;
                this.restorePending = false;
                if (restore && !this.expanded) {
                    await this.onRestorePicture();
                }
                this.onLayout();
                if (restore && !this.expanded) {
                    this.restoreViewport();
                }
            });
        });
    }
    reset() {
        this.preview.reset();
        this.unlockOrientation();
        this.exitExpanded();
        this.scrubbing = false;
        this.update();
        this.show();
    }
}
