import { watchLayout } from "../playback/src/watch-layout.js";
import { OnlineSubtitles, subtitleDownloadMarkup } from "./subtitles.js";
import "../common/navigation.js";
import "../playback/src/playback.css";
import "./online.css";
import {
    $,
    api,
    API_ROUTES,
    element,
    sourceUrl,
    title,
} from "../common/anime-api.js";
import { localStore, sessionStore } from "../common/storage.js";
import { SubtitleRenderer } from "../playback/src/subtitles/renderer.js";
import { SubtitleEditor } from "../playback/src/subtitles/editor.js";

const selection = sessionStore.read("ergo.anime");
$("playback-root").innerHTML = watchLayout(true);
$("online-downloads").innerHTML = subtitleDownloadMarkup;
$("library-series").replaceChildren(
    new Option(
        selection?.anime ? title(selection.anime) : "Anime đã chọn",
        "selected",
    ),
);
const frame = $("online-frame");
const stage = $("online-stage");
const renderer = new SubtitleRenderer(null, $("online-subtitles"));
const editor = new SubtitleEditor(null, $("online-editor"), renderer);
const subtitles = new OnlineSubtitles({
    editor,
    selection,
    currentEpisode: () => episode,
    episodes: () => episodes,
    onChanged: () => showEpisodes(),
});
let episodes = [];
let episode = 0;
let generation = 0;
let origin = "";
let time = 0;
let duration = 0;
let resumeAt = 0;
let lastSaved = 0;
let fullScroll = 0;
let pendingRender = false;
const progressKey = "ergo.online.progress.v1";
const seriesKey = `${selection?.source}:${selection?.anime?.anime_id}`;
function report(error) {
    $("player-state").textContent = error.message || error;
}
function command(command, value) {
    if (origin) {
        frame.contentWindow?.postMessage({ command, value }, origin);
    }
}
function records() {
    return localStore.read(progressKey, {});
}
function saveProgress() {
    if (!episode || duration <= 0 || resumeAt > 0) {
        return;
    }
    const history = records();
    history[seriesKey] ||= {};
    const previous = history[seriesKey][episode];
    history[seriesKey][episode] = {
        position: time,
        duration,
        updatedAt: Date.now(),
        watched: previous?.watched || time >= duration - 10,
    };
    const series = Object.keys(history);
    if (series.length > 100) {
        delete history[series[0]];
    }
    localStore.write(progressKey, history);
    lastSaved = performance.now();
    showEpisodes();
}
const episodeRows = new Map();
function showEpisodes() {
    const history = records()[seriesKey] || {};
    $("library-count").textContent = `${episodes.length} tập / video`;
    for (const item of episodes) {
        const number = item.episode_number;
        let row = episodeRows.get(number);
        if (!row) {
            const button = element("button", null, "library-episode secondary");
            const heading = element("strong", `EP${number}`);
            const name = element("small", item.title || `Tập ${number}`);
            const sub = element("small");
            const status = element("span", null, "episode-watch-status");
            const bar = element("progress", null, "episode-watch-progress");
            bar.max = 100;
            bar.setAttribute("aria-label", `Tiến độ xem EP${number}`);
            button.append(heading, name, sub, status, bar);
            button.onclick = () => void openEpisode(number);
            row = { button, sub, status, bar };
            episodeRows.set(number, row);
            $("library-episodes").append(button);
        }
        const state = history[number];
        row.button.classList.toggle("active", number === episode);
        if (number === episode) {
            row.button.setAttribute("aria-current", "true");
        } else {
            row.button.removeAttribute("aria-current");
        }
        row.sub.textContent = `${subtitles.count(number)} phụ đề đi kèm`;
        row.status.textContent = state?.watched
            ? "✓ Đã xem"
            : state?.position > 1
              ? "Đang xem"
              : "Chưa xem";
        row.button.dataset.watched = String(!!state?.watched);
        row.button.dataset.inProgress = String(
            !!state && !state.watched && state.position > 1,
        );
        row.bar.value = state?.watched
            ? 100
            : state?.duration
              ? (100 * state.position) / state.duration
              : 0;
    }
}
async function openEpisode(number) {
    saveProgress();
    const version = ++generation;
    subtitles.cancelSearch();
    episode = number;
    origin = "";
    frame.removeAttribute("src");
    time = duration = 0;
    $("online-title").textContent = `${title(selection.anime)} • Tập ${number}`;
    showEpisodes();
    const index = episodes.findIndex((item) => item.episode_number === number);
    $("previous-episode").disabled = index <= 0;
    $("next-episode").disabled = index >= episodes.length - 1;
    await subtitles.apply(number);
    if (version !== generation) {
        return;
    }
    const record = records()[seriesKey]?.[number];
    resumeAt = record && !record.watched ? record.position : 0;
    report("Đang mở nguồn phim…");
    try {
        const data = await api(
            sourceUrl(API_ROUTES.anime + "/embed", selection.source),
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    slug: selection.anime.anime_id,
                    episode: number,
                    server: $("online-server").value,
                    audio: $("online-audio").value,
                }),
            },
        );
        if (version !== generation) {
            return;
        }
        const url = new URL(data.embedUrl);
        if (
            url.protocol !== "https:" ||
            url.hostname !== "flixcloud.cc" ||
            !/^\/e\/[a-zA-Z0-9]+\/?$/.test(url.pathname)
        ) {
            throw new Error("Nguồn trả về liên kết player không hợp lệ.");
        }
        origin = url.origin;
        frame.src = url.href;
        report("");
    } catch (error) {
        if (version === generation) {
            report(error);
        }
    }
}
async function fullscreen() {
    if (document.fullscreenElement) {
        await document.exitFullscreen();
    } else {
        fullScroll = window.scrollY;
        await stage.requestFullscreen();
        try {
            await screen.orientation?.lock?.("landscape");
        } catch {
            /* Device may not support orientation lock. */
        }
    }
}
window.addEventListener("message", (event) => {
    if (
        !origin ||
        event.origin !== origin ||
        event.source !== frame.contentWindow ||
        !event.data ||
        typeof event.data !== "object"
    ) {
        return;
    }
    const data = event.data;
    if (data.zenCommand === "getFullscreenState") {
        frame.contentWindow.postMessage(
            { zenFullscreenState: !!document.fullscreenElement },
            origin,
        );
    }
    if (data.zenCommand === "toggleFullscreen") {
        void fullscreen().catch(report);
    }
    if (
        Number.isFinite(data.currentTime) &&
        Number.isFinite(data.duration) &&
        data.duration > 0
    ) {
        time = Math.max(0, data.currentTime);
        duration = data.duration;
        if (resumeAt > 0) {
            command("seek", Math.min(resumeAt, duration - 1));
            resumeAt = 0;
        }
        if (performance.now() - lastSaved > 5000) {
            saveProgress();
        }
    }
});
const polling = setInterval(() => {
    command("getTime");
}, 100);
const rendering = setInterval(async () => {
    const instance = renderer.instance;
    if (!instance || pendingRender) {
        return;
    }
    pendingRender = true;
    try {
        await instance.ready;
        if (renderer.instance === instance) {
            await instance.manualRender({
                mediaTime: time,
                width: 1920,
                height: 1080,
            });
        }
    } catch (error) {
        editor.report(error.message);
    } finally {
        pendingRender = false;
    }
}, 100);
document.addEventListener("fullscreenchange", () => {
    if (origin) {
        frame.contentWindow.postMessage(
            { zenFullscreenState: !!document.fullscreenElement },
            origin,
        );
    }
    if (!document.fullscreenElement) {
        screen.orientation?.unlock?.();
        requestAnimationFrame(() => window.scrollTo(0, fullScroll));
    }
    void editor.refreshLayout();
});
for (const button of document.querySelectorAll("[data-online-seek]")) {
    button.onclick = () =>
        command(
            "seek",
            Math.max(
                0,
                Math.min(duration, time + Number(button.dataset.onlineSeek)),
            ),
        );
}
$("download-page").onclick = () => window.ErgoNavigation.go("download");
$("server-subs").onclick = () => void subtitles.scanServer();
$("choose-subs").onclick = () => subtitles.chooseFolder();
$("previous-episode").onclick = () => adjacentEpisode(-1);
$("next-episode").onclick = () => adjacentEpisode(1);
function adjacentEpisode(step) {
    const index = episodes.findIndex((item) => item.episode_number === episode);
    const next = episodes[index + step];
    if (next) {
        void openEpisode(next.episode_number);
    }
}
$("online-server").onchange = $("online-audio").onchange = () =>
    void openEpisode(episode);
$("online-fullscreen").onclick = () => void fullscreen().catch(report);
window.addEventListener("pagehide", () => {
    saveProgress();
    clearInterval(polling);
    clearInterval(rendering);
    subtitles.dispose();
    void editor.clear();
});
window.addEventListener("pageshow", (event) => {
    if (event.persisted) {
        location.reload();
    }
});
async function initialize() {
    if (!selection?.anime?.anime_id) {
        location.replace("/search/index.html");
        return;
    }
    try {
        episodes = await api(
            sourceUrl(
                API_ROUTES.anime +
                    "/anime/" +
                    encodeURIComponent(selection.anime.anime_id) +
                    "/episodes",
                selection.source,
            ),
        );
        episodes = episodes.filter((item) => item.playable !== false);
        if (!episodes.length) {
            throw new Error("Anime chưa có tập để xem.");
        }
        await subtitles.scanServer();
        const history = records()[seriesKey] || {};
        const recent = episodes
            .filter(
                (item) =>
                    history[item.episode_number] &&
                    !history[item.episode_number].watched,
            )
            .sort(
                (a, b) =>
                    history[b.episode_number].updatedAt -
                    history[a.episode_number].updatedAt,
            )[0];
        await openEpisode((recent || episodes[0]).episode_number);
    } catch (error) {
        report(error);
    }
}
void initialize();
