import "../common/navigation.js";
import {
    $,
    api,
    API_ROUTES,
    element,
    report,
    sourceUrl,
    title,
} from "../common/anime-api.js";
import { localStore, sessionStore } from "../common/storage.js";
import { downloadState } from "./state.js";
import { resetSubtitles } from "./subtitles.js";
import "./original.js";

let episodeVersion = 0;

async function choose(
    anime,
    source = localStore.get("ergo-proxy.anime-source", "reanime"),
) {
    sessionStore.write("ergo.anime", { anime, source });
    const version = ++episodeVersion;
    $("selection").hidden = true;
    $("message").textContent = "Đang lấy danh sách tập…";
    $("subtitle-panel").hidden = true;
    try {
        const episodes = await api(
            sourceUrl(
                API_ROUTES.anime +
                    "/anime/" +
                    encodeURIComponent(anime.anime_id) +
                    "/episodes",
                source,
            ),
        );
        if (version !== episodeVersion) {
            return;
        }
        downloadState.anime = anime;
        downloadState.source = source;
        downloadState.episodes = episodes.filter((ep) => ep.playable !== false);
        $("all").checked = false;
        $("original-page").value = "";
        $("subtitle-panel").hidden = false;
        resetSubtitles();
        $("anime-title").textContent = title(anime);
        $("episodes").replaceChildren();
        for (const ep of downloadState.episodes) {
            const label = element("label", null, "episode"),
                box = element("input");
            box.type = "checkbox";
            box.value = ep.episode_number;
            box.onchange = () => {
                $("all").checked = [
                    ...$("episodes").querySelectorAll("input"),
                ].every((item) => item.checked);
            };
            label.append(
                box,
                document.createTextNode(" Tập " + ep.episode_number),
                element("small", ep.title || ""),
            );
            $("episodes").append(label);
        }
        const checked = sessionStore.read("ergo.episodes", []);
        for (const box of $("episodes").querySelectorAll("input")) {
            box.checked = Array.isArray(checked) && checked.includes(box.value);
        }
        $("all").checked =
            downloadState.episodes.length > 0 &&
            [...$("episodes").querySelectorAll("input")].every(
                (box) => box.checked,
            );
        $("selection").hidden = false;
        $("download").disabled = !downloadState.episodes.length;
        window.ErgoNavigation.go("download");

        window.scrollTo({ top: 0, behavior: "instant" });
        $("anime-title").focus({ preventScroll: true });
        $("message").textContent = downloadState.episodes.length
            ? `${downloadState.episodes.length} tập có thể tải`
            : "Anime chưa có tập có thể tải.";
    } catch (error) {
        if (version === episodeVersion) {
            report(error);
        }
    }
}

$("all").onchange = () =>
    $("episodes")
        .querySelectorAll("input")
        .forEach((box) => (box.checked = $("all").checked));

window.addEventListener("DOMContentLoaded", () => {
    const saved = sessionStore.read("ergo.anime");
    if (saved?.anime?.anime_id) {
        void choose(saved.anime, saved.source);
    } else {
        window.ErgoNavigation.reset();
    }
});
window.addEventListener("pagehide", () => {
    const checked = [...$("episodes").querySelectorAll("input:checked")].map(
        (input) => input.value,
    );
    sessionStore.write("ergo.episodes", checked);
});
