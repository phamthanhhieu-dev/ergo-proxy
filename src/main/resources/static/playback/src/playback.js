import { watchLayout } from "./watch-layout";
import "../../common/navigation.js";
import { localStore, sessionStore } from "../../common/storage.js";
import "./playback.css";
import { BrowserLibrary } from "../../common/browser-library";
import { request, text } from "./api";
import { VideoPlayer } from "./player";
import { clock, PlayerControls } from "./player-controls";
import { PlaybackProgress } from "./playback-progress";
import { SubtitleEditor } from "./subtitles/editor";

const root = document.getElementById("playback-root");
root.innerHTML = watchLayout();
const el = (id) => document.getElementById(id);
const video = el("local-video");
const report = (message) => text(el("player-state"), message);
const progress = new PlaybackProgress(video, {
    onOffer: (position) => {
        el("resume-continue").textContent = "Tiếp tục từ " + clock(position);
        el("resume-prompt").hidden = false;
    },
    onDismiss: () => {
        el("resume-prompt").hidden = true;
    },
    onError: report,
    onChanged: () => updateEpisodeProgress(),
});
el("resume-continue").onclick = () => progress.choose(true);
el("resume-restart").onclick = () => progress.choose(false);
const player = new VideoPlayer(video, report, progress);
const editor = new SubtitleEditor(video, el("subtitle-editor"));
new PlayerControls(video, {
    onLayout: () => {
        void editor
            .refreshLayout()
            .catch((error) => editor.report(error.message));
    },
    onError: report,
    onRestorePicture: () => player.restorePicture(),
});
const browserLibrary = new BrowserLibrary();
const episodeRows = new Map();
let entries = [];
let selected = null;
let generation = 0;
function showPlayer() {
    el("watch-panel").hidden = false;
    window.ErgoNavigation.go("watch");
}
function elementNode(tag, className) {
    const node = document.createElement(tag);
    node.className = className;
    return node;
}
function updateEpisodeProgress() {
    const history = progress.read();
    for (const row of episodeRows.values()) {
        const state = progress.status(row.entry, history);
        row.button.dataset.watched = String(state.watched);
        row.button.dataset.inProgress = String(state.inProgress);
        if (state.inProgress) {
            row.status.textContent =
                (state.watched ? "✓ Đã xem • Xem lại " : "Đang xem • ") +
                clock(state.position);
        } else {
            row.status.textContent = state.watched ? "✓ Đã xem" : "Chưa xem";
        }
        row.status.title = row.status.textContent;
        row.bar.value = state.inProgress
            ? Math.min(100, (state.position / state.duration) * 100)
            : state.watched
              ? 100
              : 0;
    }
}
function refreshEpisodes() {
    const series = el("library-series").value;
    el("library-episodes").replaceChildren();
    episodeRows.clear();
    const list = entries.filter((entry) => entry.series === series);
    text(el("library-count"), `${list.length} tập / video`);
    for (const entry of list) {
        const button = document.createElement("button");
        button.className = "library-episode secondary";
        button.type = "button";
        if (selected?.video.id === entry.video.id) {
            button.classList.add("active");
            button.setAttribute("aria-current", "true");
        }
        const title = document.createElement("strong");
        title.textContent = entry.episode;
        const name = document.createElement("small");
        name.textContent = entry.video.name;
        const sub = document.createElement("small");
        sub.textContent = `${entry.subtitles.length} phụ đề đi kèm`;
        const status = elementNode("span", "episode-watch-status");
        const bar = document.createElement("progress");
        bar.className = "episode-watch-progress";
        bar.max = 100;
        bar.setAttribute("aria-label", "Tiến độ xem " + entry.episode);
        button.append(title, name, sub, status, bar);
        episodeRows.set(progress.identity(entry), {
            entry,
            button,
            status,
            bar,
        });
        button.onclick = () => openVideo(entry);
        el("library-episodes").append(button);
    }
    updateEpisodeProgress();
}
function setLibrary(data) {
    if (!data.videos.length) {
        text(
            el("playback-status"),
            data.name
                ? "Thư mục chưa có video được hỗ trợ."
                : "Đã hủy chọn video.",
        );
        return;
    }
    player.stop();
    const pageUrl = new URL(location.href);
    pageUrl.searchParams.delete("local");
    history.replaceState(history.state, "", pageUrl.pathname + pageUrl.search);
    sessionStore.set("ergo.playback-mode", data.local ? "local" : "server");
    sessionStore.remove("ergo.library");
    entries = data.videos;
    const series = [...new Set(entries.map((entry) => entry.series))];
    el("library-series").replaceChildren(
        ...series.map((name) => new Option(name, name)),
    );
    text(
        el("playback-status"),
        `${data.name} • ${entries.length} video • Chọn tập để xem`,
    );
    showPlayer();
    refreshEpisodes();
    const initial = progress.initial(entries);
    void openVideo(initial);
}
async function loadLibrary() {
    const buttons = ["choose-video", "choose-folder", "server-library"].map(el);
    for (const button of buttons) {
        button.disabled = true;
    }
    text(el("playback-status"), "Đang đọc thư viện trên máy host…");
    try {
        const library = await request("/library");
        setLibrary(library);
        browserLibrary.dispose();
    } catch (error) {
        text(el("playback-status"), error.message);
    } finally {
        for (const button of buttons) {
            button.disabled = false;
        }
    }
}
async function openVideo(entry) {
    progress.flush();
    const version = ++generation;
    selected = entry;
    sessionStore.set("ergo.video", entry.video.id);
    showPlayer();
    el("library-series").value = entry.series;
    refreshEpisodes();
    text(el("watch-title"), `${entry.series} • ${entry.episode}`);
    text(el("playback-status"), entry.video.name);
    await editor.prepare(entry);
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
    await editor.loadFirst();
    if (version !== generation) {
        return;
    }
    const list = entries.filter((item) => item.series === entry.series);
    const index = list.indexOf(entry);
    el("previous-episode").disabled = index <= 0;
    el("next-episode").disabled = index >= list.length - 1;
    localStore.set("playback.last-video", entry.video.name);
}
el("choose-video").onclick = () =>
    browserLibrary.choose(false, setLibrary, (message) =>
        text(el("playback-status"), message),
    );
el("choose-folder").onclick = () =>
    browserLibrary.choose(true, setLibrary, (message) =>
        text(el("playback-status"), message),
    );
el("server-library").onclick = () => loadLibrary();
el("library-series").onchange = () => {
    const list = entries.filter(
        (entry) => entry.series === el("library-series").value,
    );
    const initial = progress.initial(list);
    if (initial) {
        void openVideo(initial);
    }
};
for (const [id, step] of [
    ["previous-episode", -1],
    ["next-episode", 1],
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
video.addEventListener("error", () => {
    if (selected && video.error?.code !== 1) {
        report(
            "Không phát được file này trên trình duyệt hiện tại. Kiểm tra codec video/âm thanh hoặc dùng trình phát hỗ trợ file gốc. Player không tự chuyển mã.",
        );
    }
});
video.addEventListener("ended", () => {
    const list = entries.filter((item) => item.series === selected?.series);
    const next = list[list.indexOf(selected) + 1];
    if (next) {
        report("Đã hết tập. Bấm Tập tiếp để xem " + next.episode);
    }
});
window.addEventListener("pagehide", (event) => {
    player.stop();
    void editor.clear();
    if (!event.persisted) {
        browserLibrary.dispose();
    }
});

const localChoice = new URLSearchParams(location.search).get("local");
const localMode =
    Boolean(localChoice) || sessionStore.get("ergo.playback-mode") === "local";
if (localMode) {
    showPlayer();
    text(el("watch-title"), "Chọn video trên thiết bị");
    text(
        el("playback-status"),
        "Chọn video và sub cùng lúc, hoặc chọn thư mục để tự nhận video/sub theo từng EP. File không được tải lên server; sau khi tải lại trang cần chọn lại.",
    );
    el(localChoice === "folder" ? "choose-folder" : "choose-video").focus();
} else {
    void loadLibrary();
}

window.addEventListener("pageshow", (event) => {
    if (event.persisted && selected) {
        void openVideo(selected);
    }
});
