import { LiveSearch } from "../common/live-search.js";
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
import "../common/navigation.js";

let currentSource = localStore.get("ergo-proxy.anime-source", "reanime");
let searchVersion = 0;
let searchController;
let sourceReady = false;
let queryEdited = false;
const liveSearch = new LiveSearch({
    input: $("query"),
    delay: 0,
    onSearch: (query) => search(query),
    onCancel: () => {
        searchController?.abort();
        ++searchVersion;
        $("results").setAttribute("aria-busy", "false");
    },
    onClear: () => {
        queryEdited = true;
        $("results").replaceChildren();
        $("message").textContent = "";
    }
});

function cardFor(anime, source) {
    const card = element("button", null, "card");
    card.type = "button";
    const cover = anime.cover_image?.large || anime.cover_image?.medium;
    if (cover) {
        try {
            const url = new URL(cover);
            if (url.protocol === "https:") {
                const image = element("img");
                Object.assign(image, {
                    src: url.href,
                    alt: title(anime),
                    loading: "lazy",
                    referrerPolicy: "no-referrer",
                });
                card.append(image);
            }
        } catch {
            /* Ignore malformed cover URLs. */
        }
    }
    card.append(
        element("strong", title(anime)),
        element(
            "small",
            `${anime.format || "ANIME"} • ${anime.season_year || ""}`,
        ),
    );
    card.onclick = () => {
        if (!sessionStore.write("ergo.anime", { anime, source })) {
            report(
                new Error(
                    "Trình duyệt không cho lưu anime đã chọn. Hãy cho phép dữ liệu trang.",
                ),
            );
            return;
        }
        sessionStore.remove("ergo.episodes");
        window.ErgoNavigation.go("online");
    };
    return card;
}
async function search(query = $("query").value.trim()) {
    if (!sourceReady || !query) {
        return;
    }
    searchController?.abort();
    searchController = new AbortController();
    const version = ++searchVersion;
    const source = currentSource;
    $("results").setAttribute("aria-busy", "true");
    $("message").textContent = "Đang tìm anime…";
    $("results").replaceChildren();
    try {
        const data = await api(
            sourceUrl(
                API_ROUTES.anime +
                    "/search?q=" +
                    encodeURIComponent(query),
                source,
            ),
            { signal: searchController.signal },
        );
        if (version !== searchVersion) {
            return;
        }
        const results = data.results || (data.anime_id ? [data] : []);
        $("message").textContent = results.length
            ? `${results.length} kết quả • Chọn anime để xem tập`
            : "Không tìm thấy anime.";
        $("results").replaceChildren(
            ...results.map((anime) => cardFor(anime, source)),
        );
    } catch (error) {
        if (version === searchVersion && error.name !== "AbortError") {
            report(error);
        }
    } finally {
        if (version === searchVersion) {
            $("results").setAttribute("aria-busy", "false");
        }
    }
}
function updateSource() {
    const name = $("anime-source").selectedOptions[0]?.textContent || "ReAnime";
    $("source-description").textContent =
        `Tìm kiếm và tải phim từ ${name} • Phụ đề từ OpenSubtitles`;
    $("query").placeholder = `Tên anime hoặc link ${name}…`;
}
$("search").onsubmit = (event) => {
    event.preventDefault();
    liveSearch.submit();
};
$("anime-source").onchange = () => {
    liveSearch.cancel();
    currentSource = $("anime-source").value;
    localStore.set("ergo-proxy.anime-source", currentSource);
    $("results").replaceChildren();
    $("query").value = "";
    $("message").textContent = "";
    updateSource();
};
async function initialize() {
    try {
        const sources = await api(API_ROUTES.anime + "/sources");
        if (!sources.length) {
            throw new Error("Không có nguồn phim khả dụng.");
        }
        $("anime-source").replaceChildren(
            ...sources.map((source) => new Option(source.name, source.id)),
        );
        currentSource = sources.some((source) => source.id === currentSource)
            ? currentSource
            : sources[0].id;
        $("anime-source").value = currentSource;
        localStore.set("ergo-proxy.anime-source", currentSource);
        updateSource();
        sourceReady = true;
        if (!queryEdited) {
            $("query").value = sessionStore.get("ergo.query", "");
        }
        if ($("query").value) {
            liveSearch.cancel();
            await search();
        }
    } catch (error) {
        report(error);
    }
}
window.addEventListener("pagehide", () => {
    liveSearch.cancel();
    sessionStore.set("ergo.query", $("query").value);
});
window.addEventListener("pageshow", (event) => {
    if (
        event.persisted &&
        $("query").value &&
        !$("results").childElementCount
    ) {
        void search();
    }
});
void initialize();
