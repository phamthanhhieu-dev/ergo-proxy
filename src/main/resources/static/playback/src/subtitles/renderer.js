import JASSUB from 'jassub';
import workerUrl from 'jassub/dist/worker/worker.js?worker&url';
import wasmUrl from 'jassub/dist/wasm/jassub-worker.wasm?url';
import modernWasmUrl from 'jassub/dist/wasm/jassub-worker-modern.wasm?url';
import defaultFontUrl from 'jassub/dist/default.woff2?url';
import { decodeSubtitle, toAss, customizeAss } from './ass';
export class SubtitleRenderer {
    constructor(video) {
        this.video = video;
        this.instance = null;
        this.original = '';
        this.name = '';
        this.version = 0;
        this.settings = { original: true, font: 'Liberation Sans', scale: 100, delay: 0 };
        this.fonts = [];
        this.extraFonts = [];
    }
    async clear() {
        this.version++;
        const previous = this.instance;
        this.instance = null;
        this.original = '';
        if (previous) {
            await previous.destroy();
        }
    }
    async load(bytes, name, fonts = []) {
        fonts = [...fonts, ...this.extraFonts];
        await this.clear();
        const version = ++this.version;
        const extension = name.split('.').pop().toLowerCase();
        this.original = toAss(decodeSubtitle(bytes), extension);
        this.name = name;
        this.fonts = fonts;
        const instance = new JASSUB({
            video: this.video,
            subContent: customizeAss(this.original, this.settings),
            timeOffset: -this.settings.delay,
            workerUrl,
            wasmUrl,
            modernWasmUrl,
            queryFonts: false,
            maxRenderHeight: window.matchMedia('(pointer: coarse)').matches ? 720 : 1080,
            fonts: [defaultFontUrl, ...fonts],
            availableFonts: { 'liberation sans': defaultFontUrl },
            defaultFont: 'Liberation Sans'
        });
        this.instance = instance;
        await instance.ready;
        if (version !== this.version) {
            await instance.destroy();
            return;
        }
        await instance.resize(true);
    }
    async apply(settings) {
        const previous = this.settings;
        const styleChanged =
            previous.original !== settings.original ||
            previous.font !== settings.font ||
            previous.scale !== settings.scale;
        this.settings = settings;
        const instance = this.instance;
        if (!instance) {
            return;
        }
        await instance.ready;
        if (this.instance !== instance) {
            return;
        }
        instance.timeOffset = -settings.delay;
        if (styleChanged) {
            await instance.renderer.setTrack(customizeAss(this.original, settings));
        }
        if (this.video.paused) {
            await instance.resize(true);
        }
    }
    async refreshLayout() {
        const instance = this.instance;
        if (!instance || !this.video.videoWidth || !this.video.clientWidth || !this.video.clientHeight) {
            return;
        }
        await instance.ready;
        if (this.instance === instance) {
            await instance.resize(this.video.paused);
        }
    }
    async addFonts(files) {
        const fonts = await Promise.all([...files].map(async (file) => new Uint8Array(await file.arrayBuffer())));
        this.fonts.push(...fonts);
        this.extraFonts.push(...fonts);
        if (this.instance) {
            await this.instance.ready;
            await this.instance.renderer.addFonts(fonts);
        }
    }
    export() {
        if (!this.original) {
            throw new Error('Chọn phụ đề trước khi xuất.');
        }
        const content = customizeAss(this.original, this.settings, this.settings.delay);
        const url = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = this.name.replace(/\.[^.]+$/, '') + '.edited.ass';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 10000);
    }
}
