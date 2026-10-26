import './playback.css';
import { BrowserLibrary } from '../../common/browser-library';
import { request, fileUrl, text } from './api';
import { VideoPlayer } from './player';
import { PlayerControls } from './player-controls';
import { SubtitleRenderer } from './subtitles/renderer';
const root = document.getElementById('playback-root');
root.innerHTML = `
<section id="watch-panel" hidden>
  <div class="section-head"><div><div class="eyebrow">ĐANG XEM / THƯ VIỆN CỤC BỘ</div><h2 id="watch-title">Chọn video để bắt đầu</h2></div></div>
  <div class="watch-layout"><div class="watch-main">
    <div id="video-stage"><video id="local-video" controls playsinline preload="metadata" disablepictureinpicture></video></div>
    <div class="player-toolbar"><button id="previous-episode" class="secondary">← Tập trước</button><button id="next-episode" class="secondary">Tập tiếp →</button></div>
    <p id="player-state" role="status" aria-live="polite"></p>
    <div class="subtitle-editor"><div class="section-head"><h2>Phụ đề</h2><label class="file-button">Thêm file sub<input id="add-subtitle" type="file" accept=".ass,.ssa,.srt,.vtt" hidden /></label></div>
      <label>Bản phụ đề <select id="local-subtitles"><option value="">Tắt phụ đề</option></select></label>
      <div class="subtitle-settings">
        <label><input id="keep-ass-style" type="checkbox" checked /> Giữ kiểu ASS/SSA gốc</label>
        <label>Font chữ<input id="subtitle-font" value="Liberation Sans" maxlength="80" list="font-list" /><datalist id="font-list"><option value="Liberation Sans"></option><option value="Arial"></option><option value="Roboto"></option><option value="Noto Sans"></option></datalist></label>
        <label>Cỡ chữ <output id="font-scale-value">100%</output><input id="font-scale" type="range" min="50" max="250" value="100" step="5" /></label>
        <label>Độ trễ phụ đề (giây)<input id="subtitle-delay" type="number" min="-3600" max="3600" value="0" step="0.1" /></label>
      </div>
      <div class="playback-actions"><button id="sub-earlier" class="secondary">Sớm hơn 0,25s</button><button id="sub-later" class="secondary">Muộn hơn 0,25s</button><label class="file-button secondary">Thêm font TTF/OTF<input id="add-font" type="file" accept=".ttf,.otf,.woff,.woff2" multiple hidden /></label><button id="reset-subtitles" class="text">Khôi phục mặc định</button><button id="export-subtitles" class="secondary">Xuất bản ASS đã chỉnh</button></div>
      <p class="hint">Độ trễ dương = sub xuất hiện muộn hơn; âm = sớm hơn. Thay đổi font/cỡ chữ sẽ bỏ chế độ giữ kiểu gốc. Font chưa có sẽ dùng font dự phòng; thêm file font để hiển thị đúng. Các chỉnh sửa chỉ áp dụng trên player; file sub gốc được giữ nguyên.</p>
      <p id="subtitle-editor-status" role="status" aria-live="polite"></p>
    </div>
  </div><aside class="episode-browser"><label>Bộ anime <select id="library-series"></select></label><div id="library-count" class="eyebrow"></div><div id="library-episodes"></div></aside></div>
  <p class="hint">Video được truyền nguyên file từ ổ đĩa qua mạng LAN. Phụ đề, font và độ trễ xử lý trên thiết bị đang xem. Khả năng phát video/âm thanh phụ thuộc codec trình duyệt hỗ trợ.</p>
</section>`;
const el = (id) => document.getElementById(id);
const video = el('local-video');
const report = (message) => text(el('player-state'), message);
const subtitleReport = (message) => text(el('subtitle-editor-status'), message);
const player = new VideoPlayer(video, report);
const renderer = new SubtitleRenderer(video);
const controls = new PlayerControls(video, {
    onLayout: () => {
        void renderer.refreshLayout().catch((error) => subtitleReport(error.message));
    },
    onError: report,
    onRestorePicture: () => player.restorePicture()
});
const browserLibrary = new BrowserLibrary();
let entries = [];
let selected = null;
let subtitleFiles = [];
let generation = 0;
let subGeneration = 0;
let updateTimer;
function showPlayer() {
    el('watch-panel').hidden = false;
    window.ErgoNavigation.go('watch');
}
function refreshEpisodes() {
    const series = el('library-series').value;
    el('library-episodes').replaceChildren();
    const list = entries.filter((entry) => entry.series === series);
    text(el('library-count'), `${list.length} tập / video`);
    for (const entry of list) {
        const button = document.createElement('button');
        button.className = 'library-episode secondary';
        button.type = 'button';
        if (selected?.video.id === entry.video.id) {
            button.classList.add('active');
            button.setAttribute('aria-current', 'true');
        }
        const title = document.createElement('strong');
        title.textContent = entry.episode;
        const name = document.createElement('small');
        name.textContent = entry.video.name;
        const sub = document.createElement('small');
        sub.textContent = `${entry.subtitles.length} phụ đề đi kèm`;
        button.append(title, name, sub);
        button.onclick = () => openVideo(entry);
        el('library-episodes').append(button);
    }
}
function setLibrary(data) {
    if (!data.videos.length) {
        text(el('playback-status'), data.name ? 'Thư mục chưa có video được hỗ trợ.' : 'Đã hủy chọn video.');
        return;
    }
    player.stop();
    const pageUrl = new URL(location.href);
    pageUrl.searchParams.delete('local');
    history.replaceState(history.state, '', pageUrl.pathname + pageUrl.search);
    sessionStorage.setItem('ergo.playback-mode', data.local ? 'local' : 'server');
    sessionStorage.removeItem('ergo.library');
    entries = data.videos;
    const series = [...new Set(entries.map((entry) => entry.series))];
    el('library-series').replaceChildren(...series.map((name) => new Option(name, name)));
    text(el('playback-status'), `${data.name} • ${entries.length} video • Chọn tập để xem`);
    showPlayer();
    refreshEpisodes();
    const savedId = sessionStorage.getItem('ergo.video');
    const savedName = localStorage.getItem('playback.last-video');
    const initial =
        entries.find((entry) => entry.video.id === savedId) ||
        entries.find((entry) => entry.video.name === savedName) ||
        entries[0];
    void openVideo(initial);
}
async function loadLibrary() {
    const buttons = ['choose-video', 'choose-folder', 'server-library'].map(el);
    for (const button of buttons) {
        button.disabled = true;
    }
    text(el('playback-status'), 'Đang đọc thư viện trên máy host…');
    try {
        const library = await request('/library');
        setLibrary(library);
        browserLibrary.dispose();
    } catch (error) {
        text(el('playback-status'), error.message);
    } finally {
        for (const button of buttons) {
            button.disabled = false;
        }
    }
}
async function openVideo(entry) {
    const version = ++generation;
    subGeneration++;
    selected = entry;
    sessionStorage.setItem('ergo.video', entry.video.id);
    showPlayer();
    el('library-series').value = entry.series;
    refreshEpisodes();
    text(el('watch-title'), `${entry.series} • ${entry.episode}`);
    text(el('playback-status'), entry.video.name);
    subtitleFiles = entry.subtitles.map((file) => ({
        ...file,
        url: file.url || fileUrl(file.id)
    }));
    el('local-subtitles').replaceChildren(
        new Option('Tắt phụ đề', ''),
        ...subtitleFiles.map((file, index) => new Option(file.name, String(index)))
    );
    await renderer.clear();
    subtitleReport('');
    if (version !== generation) {
        return;
    }
    try {
        await player.open(entry);
    } catch (error) {
        if (version === generation) {
            player.stop();
            report(error.message);
        }
    }
    if (version !== generation) {
        return;
    }
    if (subtitleFiles.length) {
        el('local-subtitles').value = '0';
        await chooseSubtitle();
    }
    const list = entries.filter((item) => item.series === entry.series);
    const index = list.indexOf(entry);
    el('previous-episode').disabled = index <= 0;
    el('next-episode').disabled = index >= list.length - 1;
    try {
        localStorage.setItem('playback.last-video', entry.video.name);
    } catch {}
}
async function chooseSubtitle() {
    const version = ++subGeneration;
    const value = el('local-subtitles').value;
    if (value === '') {
        await renderer.clear();
        subtitleReport('Đã tắt phụ đề.');
        return;
    }
    const file = subtitleFiles[Number(value)];
    subtitleReport('Đang tải phụ đề…');
    try {
        let bytes = file.bytes;
        if (!bytes) {
            const response = await fetch(file.url);
            if (!response.ok) {
                throw new Error('Không đọc được file phụ đề. Hãy chọn lại thư mục.');
            }
            const length = Number(response.headers.get('Content-Length') || 0);
            if (length > 8 * 1024 * 1024) {
                throw new Error('Phụ đề vượt giới hạn 8 MB.');
            }
            bytes = new Uint8Array(await response.arrayBuffer());
        }
        if (bytes.length > 8 * 1024 * 1024) {
            throw new Error('Phụ đề vượt giới hạn 8 MB.');
        }
        if (version !== subGeneration) {
            return;
        }
        await renderer.load(bytes, file.name, selected?.fonts.map((font) => font.url || fileUrl(font.id)) || []);
        if (version === subGeneration) {
            subtitleReport('Đang dùng: ' + file.name);
        }
    } catch (error) {
        if (version === subGeneration) {
            subtitleReport('Không hiển thị được phụ đề: ' + error.message);
        }
    }
}
function settings() {
    return {
        original: el('keep-ass-style').checked,
        font: el('subtitle-font').value.trim() || 'Liberation Sans',
        scale: Number(el('font-scale').value),
        delay: Math.max(-3600, Math.min(3600, Number(el('subtitle-delay').value) || 0))
    };
}
async function applySettings() {
    const value = settings();
    text(el('font-scale-value'), value.scale + '%');
    try {
        await renderer.apply(value);
    } catch (error) {
        subtitleReport(error.message);
    }
}
function stepDelay(step) {
    el('subtitle-delay').value = String(Math.round((settings().delay + step) * 100) / 100);
    void applySettings();
}
el('choose-video').onclick = () =>
    browserLibrary.choose(false, setLibrary, (message) => text(el('playback-status'), message));
el('choose-folder').onclick = () =>
    browserLibrary.choose(true, setLibrary, (message) => text(el('playback-status'), message));
el('server-library').onclick = () => loadLibrary();
el('library-series').onchange = refreshEpisodes;
el('local-subtitles').onchange = chooseSubtitle;
window.addEventListener('ergo:viewchange', (event) => {
    if (event.detail.previous !== 'watch' || event.detail.view === 'watch') {
        return;
    }
    generation++;
    subGeneration++;
    player.stop();
    controls.reset();
    void renderer.clear();
    text(el('playback-status'), '');
});
for (const [id, step] of [
    ['previous-episode', -1],
    ['next-episode', 1]
]) {
    el(id).onclick = () => {
        if (!selected) {
            return;
        }
        const list = entries.filter((item) => item.series === selected.series);
        const next = list[list.indexOf(selected) + step];
        if (next) {
            void openVideo(next);
        }
    };
}
for (const id of ['keep-ass-style', 'subtitle-font', 'font-scale', 'subtitle-delay']) {
    el(id).oninput = () => {
        if (id === 'subtitle-font' || id === 'font-scale') {
            el('keep-ass-style').checked = false;
        }
        clearTimeout(updateTimer);
        updateTimer = setTimeout(applySettings, 120);
    };
}
el('sub-earlier').onclick = () => stepDelay(-0.25);
el('sub-later').onclick = () => stepDelay(0.25);
el('reset-subtitles').onclick = () => {
    el('keep-ass-style').checked = true;
    el('subtitle-font').value = 'Liberation Sans';
    el('font-scale').value = '100';
    el('subtitle-delay').value = '0';
    void applySettings();
};
el('export-subtitles').onclick = () => {
    try {
        renderer.export();
        subtitleReport('Đã xuất bản ASS đã chỉnh, file nguồn giữ nguyên.');
    } catch (error) {
        subtitleReport(error.message);
    }
};
el('add-font').onchange = async (event) => {
    try {
        await renderer.addFonts(event.target.files);
        subtitleReport('Đã nạp font. Nhập đúng tên font trong ô Font chữ.');
    } catch (error) {
        subtitleReport(error.message);
    } finally {
        event.target.value = '';
    }
};
el('add-subtitle').onchange = async (event) => {
    const file = event.target.files[0];
    if (!file) {
        return;
    }
    try {
        if (file.size > 8 * 1024 * 1024) {
            throw new Error('Phụ đề vượt giới hạn 8 MB.');
        }
        subtitleFiles.push({
            name: file.name,
            bytes: new Uint8Array(await file.arrayBuffer())
        });
        el('local-subtitles').append(new Option(file.name, String(subtitleFiles.length - 1)));
        el('local-subtitles').value = String(subtitleFiles.length - 1);
        await chooseSubtitle();
    } catch (error) {
        subtitleReport(error.message);
    } finally {
        event.target.value = '';
    }
};
video.addEventListener('error', () => {
    if (selected && video.error?.code !== 1) {
        report(
            'Không phát được file này trên trình duyệt hiện tại. Kiểm tra codec video/âm thanh hoặc dùng trình phát hỗ trợ file gốc. Player không tự chuyển mã.'
        );
    }
});
video.addEventListener('ended', () => {
    const list = entries.filter((item) => item.series === selected?.series);
    const next = list[list.indexOf(selected) + 1];
    if (next) {
        report('Đã hết tập. Bấm Tập tiếp để xem ' + next.episode);
    }
});
window.addEventListener('pagehide', (event) => {
    player.stop();
    if (!event.persisted) {
        browserLibrary.dispose();
    }
});

const localChoice = new URLSearchParams(location.search).get('local');
const localMode = Boolean(localChoice) || sessionStorage.getItem('ergo.playback-mode') === 'local';
if (localMode) {
    showPlayer();
    text(el('watch-title'), 'Chọn video trên thiết bị');
    text(
        el('playback-status'),
        'Chọn video và sub cùng lúc, hoặc chọn thư mục để tự nhận video/sub theo từng EP. File không được tải lên server; sau khi tải lại trang cần chọn lại.'
    );
    el(localChoice === 'folder' ? 'choose-folder' : 'choose-video').focus();
} else {
    void loadLibrary();
}

window.addEventListener('pageshow', (event) => {
    if (event.persisted && selected) {
        void openVideo(selected);
    }
});
