async function choose(anime, source = currentSource) {
    sessionStorage.setItem('ergo.anime', JSON.stringify({ anime, source }));
    if (document.body.dataset.page !== 'download') {
        window.ErgoNavigation.go('download');
        return;
    }
    const version = ++episodeVersion;
    $('selection').hidden = true;
    $('message').textContent = 'Đang lấy danh sách tập…';
    $('subtitle-panel').hidden = true;
    subtitleVersion++;
    try {
        const episodes = await api(
            sourceUrl(API_ROUTES.anime + '/anime/' + encodeURIComponent(anime.anime_id) + '/episodes', source)
        );
        if (version !== episodeVersion) return;
        selectedAnime = anime;
        selectedAnimeSource = source;
        available = episodes.filter((ep) => ep.playable !== false);
        $('all').checked = false;
        $('original-page').value = '';
        $('subtitle-panel').hidden = false;
        $('subtitle-results').replaceChildren();
        $('subtitle-status').textContent = '';
        $('subtitle-save-all').hidden = true;
        $('subtitle-imdb').value = '';
        $('subtitle-season').value = 1;
        subtitleVersion++;
        subtitleSelections.clear();
        $('anime-title').textContent = title(anime);
        $('episodes').replaceChildren();
        for (const ep of available) {
            const label = element('label', null, 'episode'),
                box = element('input');
            box.type = 'checkbox';
            box.value = ep.episode_number;
            box.onchange = () => {
                $('all').checked = [...$('episodes').querySelectorAll('input')].every((item) => item.checked);
            };
            label.append(box, document.createTextNode(' Tập ' + ep.episode_number), element('small', ep.title || ''));
            $('episodes').append(label);
        }
        const checked = JSON.parse(sessionStorage.getItem('ergo.episodes') || '[]');
        for (const box of $('episodes').querySelectorAll('input')) {
            box.checked = checked.includes(box.value);
        }
        $('selection').hidden = false;
        $('download').disabled = !available.length;
        window.ErgoNavigation.go('download');

        window.scrollTo({ top: 0, behavior: 'instant' });
        $('anime-title').focus({ preventScroll: true });
        $('message').textContent = available.length
            ? `${available.length} tập có thể tải`
            : 'Anime chưa có tập có thể tải.';
    } catch (error) {
        if (version === episodeVersion) report(error);
    }
}

$('all').onchange = () =>
    $('episodes')
        .querySelectorAll('input')
        .forEach((box) => (box.checked = $('all').checked));

window.addEventListener('DOMContentLoaded', () => {
    const saved = JSON.parse(sessionStorage.getItem('ergo.anime') || 'null');
    if (saved) {
        void choose(saved.anime, saved.source);
    } else {
        window.ErgoNavigation.reset();
    }
});
window.addEventListener('pagehide', () => {
    const checked = [...$('episodes').querySelectorAll('input:checked')].map((input) => input.value);
    sessionStorage.setItem('ergo.episodes', JSON.stringify(checked));
});
