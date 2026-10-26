$('search').onsubmit = async (event) => {
    event.preventDefault();
    const version = ++searchVersion;
    ++episodeVersion;
    $('message').textContent = 'Đang tìm anime…';
    $('results').replaceChildren();
    try {
        const source = currentSource;
        const data = await api(
            sourceUrl(API_ROUTES.anime + '/search?q=' + encodeURIComponent($('query').value.trim()), source)
        );
        if (version !== searchVersion) return;
        const results = data.results || (data.anime_id ? [data] : []);
        $('message').textContent = results.length
            ? `${results.length} kết quả • Chọn anime để xem tập`
            : 'Không tìm thấy anime.';
        for (const anime of results) {
            const card = element('button', null, 'card');
            card.type = 'button';
            const cover = anime.cover_image?.large || anime.cover_image?.medium;
            if (cover) {
                try {
                    const url = new URL(cover);
                    if (url.protocol === 'https:') {
                        const img = element('img');
                        img.src = url.href;
                        img.alt = title(anime);
                        img.loading = 'lazy';
                        img.referrerPolicy = 'no-referrer';
                        card.append(img);
                    }
                } catch {}
            }
            card.append(
                element('strong', title(anime)),
                element('small', `${anime.format || 'ANIME'} • ${anime.season_year || ''}`)
            );
            card.onclick = () => choose(anime, source);
            $('results').append(card);
        }
    } catch (error) {
        if (version === searchVersion) report(error);
    }
};

function choose(anime, source = currentSource) {
    sessionStorage.setItem('ergo.anime', JSON.stringify({ anime, source }));
    sessionStorage.removeItem('ergo.episodes');
    window.ErgoNavigation.go('download');
}
function updateSourceDescription() {
    const name = $('anime-source').selectedOptions[0]?.textContent || 'ReAnime';
    $('source-description').textContent = `Tìm kiếm và tải phim từ ${name} • Phụ đề từ OpenSubtitles`;
    $('query').placeholder = `Tên anime hoặc link ${name}…`;
}

$('anime-source').onchange = () => {
    currentSource = $('anime-source').value;
    localStorage.setItem('ergo-proxy.anime-source', currentSource);
    ++searchVersion;
    $('results').replaceChildren();
    $('query').value = '';
    updateSourceDescription();
};
window.addEventListener('DOMContentLoaded', () => {
    api(API_ROUTES.anime + '/sources')
        .then((sources) => {
            let saved = currentSource;
            try {
                saved = localStorage.getItem('ergo-proxy.anime-source') || currentSource;
            } catch {}
            if (!sources.length) throw Error('Không có nguồn phim khả dụng.');
            $('anime-source').replaceChildren(
                ...sources.map((source) => {
                    const option = document.createElement('option');
                    option.value = source.id;
                    option.textContent = source.name;
                    return option;
                })
            );
            const next = sources.some((source) => source.id === saved) ? saved : sources[0].id;
            $('anime-source').value = next;
            if (next !== currentSource) $('anime-source').onchange();
            else updateSourceDescription();
        })
        .catch(report);
});

window.addEventListener('DOMContentLoaded', () => {
    $('query').value = sessionStorage.getItem('ergo.query') || '';
    if ($('query').value) {
        $('search').requestSubmit();
    }
});
window.addEventListener('pagehide', () => sessionStorage.setItem('ergo.query', $('query').value));
