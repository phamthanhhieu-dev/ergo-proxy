import { $, API_ROUTES, api, sourceUrl, title, element, report } from '../common/anime-api.js';
import { localStore, sessionStore } from '../common/storage.js';
import '../common/navigation.js';
let currentSource = localStore.get('ergo-proxy.anime-source', 'reanime');
let searchVersion = 0;
let searchController;

function cardFor(anime, source) {
    const card = element('button', null, 'card');
    card.type = 'button';
    const cover = anime.cover_image?.large || anime.cover_image?.medium;
    if (cover) {
        try {
            const url = new URL(cover);
            if (url.protocol === 'https:') {
                const image = element('img');
                Object.assign(image, {
                    src: url.href,
                    alt: title(anime),
                    loading: 'lazy',
                    referrerPolicy: 'no-referrer'
                });
                card.append(image);
            }
        } catch {
            /* Ignore malformed cover URLs. */
        }
    }
    card.append(
        element('strong', title(anime)),
        element('small', `${anime.format || 'ANIME'} • ${anime.season_year || ''}`)
    );
    card.onclick = () => {
        if (!sessionStore.write('ergo.anime', { anime, source })) {
            report(new Error('Trình duyệt không cho lưu anime đã chọn. Hãy cho phép dữ liệu trang.'));
            return;
        }
        sessionStore.remove('ergo.episodes');
        window.ErgoNavigation.go('download');
    };
    return card;
}
async function search() {
    searchController?.abort();
    searchController = new AbortController();
    const version = ++searchVersion;
    const source = currentSource;
    $('message').textContent = 'Đang tìm anime…';
    $('results').replaceChildren();
    try {
        const data = await api(
            sourceUrl(API_ROUTES.anime + '/search?q=' + encodeURIComponent($('query').value.trim()), source),
            { signal: searchController.signal }
        );
        if (version !== searchVersion) {
            return;
        }
        const results = data.results || (data.anime_id ? [data] : []);
        $('message').textContent = results.length
            ? `${results.length} kết quả • Chọn anime để xem tập`
            : 'Không tìm thấy anime.';
        $('results').replaceChildren(...results.map((anime) => cardFor(anime, source)));
    } catch (error) {
        if (version === searchVersion && error.name !== 'AbortError') {
            report(error);
        }
    }
}
function updateSource() {
    const name = $('anime-source').selectedOptions[0]?.textContent || 'ReAnime';
    $('source-description').textContent = `Tìm kiếm và tải phim từ ${name} • Phụ đề từ OpenSubtitles`;
    $('query').placeholder = `Tên anime hoặc link ${name}…`;
}
$('search').onsubmit = (event) => {
    event.preventDefault();
    void search();
};
$('anime-source').onchange = () => {
    searchController?.abort();
    ++searchVersion;
    currentSource = $('anime-source').value;
    localStore.set('ergo-proxy.anime-source', currentSource);
    $('results').replaceChildren();
    $('query').value = '';
    $('message').textContent = '';
    updateSource();
};
async function initialize() {
    try {
        const sources = await api(API_ROUTES.anime + '/sources');
        if (!sources.length) {
            throw new Error('Không có nguồn phim khả dụng.');
        }
        $('anime-source').replaceChildren(...sources.map((source) => new Option(source.name, source.id)));
        currentSource = sources.some((source) => source.id === currentSource) ? currentSource : sources[0].id;
        $('anime-source').value = currentSource;
        localStore.set('ergo-proxy.anime-source', currentSource);
        updateSource();
        $('query').value = sessionStore.get('ergo.query', '');
        if ($('query').value) {
            await search();
        }
    } catch (error) {
        report(error);
    }
}
window.addEventListener('pagehide', () => {
    searchController?.abort();
    sessionStore.set('ergo.query', $('query').value);
});
window.addEventListener('pageshow', (event) => {
    if (event.persisted && $('query').value && !$('results').childElementCount) {
        void search();
    }
});
void initialize();
