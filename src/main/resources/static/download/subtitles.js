let subtitleVersion = 0,
    subtitleBusy = false,
    subtitleConfigured = false;
const subtitleSelections = new Map();
const safeSubtitleName = (text) => {
    const clean = text.replace(/[\\/:*?"<>|\x00-\x1f]/g, '_'),
        match = clean.match(/\.[a-z0-9]{1,4}$/i);
    return match ? clean.slice(0, -match[0].length).slice(0, 160 - match[0].length) + match[0] : clean.slice(0, 160);
};

function subtitleControls(busy) {
    subtitleBusy = busy;
    for (const id of ['subtitle-search', 'subtitle-full', 'subtitle-save-all', 'download-bundle'])
        $(id).disabled = busy || !subtitleConfigured;
}

api(API_ROUTES.subtitle + '/configuration')
    .then((config) => {
        subtitleConfigured = config.configured;
        $('subtitle-config').textContent = config.configured
            ? `Đã cấu hình OpenSubtitles${config.authenticated ? ' • Có tài khoản/token' : ' • Quota ẩn danh'}.`
            : 'Chưa có API key. Cấu hình OPENSUBTITLES_API_KEY ở server theo README.';
        subtitleControls(subtitleBusy);
    })
    .catch((error) => {
        $('subtitle-config').textContent = error.message;
    });
subtitleControls(false);

function subtitleSettings() {
    const season = Number($('subtitle-season').value),
        imdb = $('subtitle-imdb').value.trim(),
        language = $('subtitle-language').value;
    if (
        !selectedAnime ||
        !Number.isInteger(season) ||
        season < 1 ||
        season > 1000 ||
        (imdb && !/^(tt)?\d{1,12}$/.test(imdb))
    )
        throw Error('Kiểm tra anime, mùa và IMDb ID.');
    return { q: title(selectedAnime), season, imdb, language, movie: selectedAnime.format === 'MOVIE' };
}

function subtitleName(settings, episode, candidate) {
    return candidate?.fileName
        ? safeSubtitleName(candidate.fileName)
        : safeSubtitleName(settings.q) +
        (settings.movie ? '' : ` - E${String(episode).padStart(2, '0')}`) +
        `.${settings.language}.txt`;
}

async function saveSubtitle(selection, button) {
    if (subtitleBusy) return;
    nativeDownload(API_ROUTES.subtitle + '/native', { fileId: selection.candidate.fileId });
    $('subtitle-status').textContent = 'Đã chuyển phụ đề sang Downloads của trình duyệt (Ctrl+J).';
}

async function findSubtitles(episodes, settings, version) {
    subtitleSelections.clear();
    $('subtitle-results').replaceChildren();
    $('subtitle-save-all').hidden = true;
    const missing = [];
    for (const episode of settings.movie ? episodes.slice(0, 1) : episodes) {
        if (version !== subtitleVersion) throw Error('Đã hủy tìm phụ đề vì chuyển anime.');
        $('subtitle-status').textContent = `Đang tìm sub tập ${episode}…`;
        const params = new URLSearchParams({ ...settings, episode });
        const data = await api(API_ROUTES.subtitle + '/search?' + params);
        if (version !== subtitleVersion) throw Error('Đã hủy tìm phụ đề vì chuyển anime.');
        const row = element('div', null, 'subtitle-row');
        row.append(element('strong', settings.movie ? settings.q : `Tập ${episode}`));
        if (!data.results.length) {
            missing.push(episode);
            row.append(element('p', 'Không có phụ đề phù hợp.'));
        } else {
            const select = element('select');
            select.setAttribute('aria-label', 'Bản phụ đề tập ' + episode);
            const option = (candidate) =>
                `${candidate.title} • ${candidate.fileName || candidate.release} • ${candidate.downloads} lượt tải`;
            data.results.forEach((candidate, index) => select.append(new Option(option(candidate), String(index))));
            const selection = {
                candidate: data.results[0],
                name: subtitleName(settings, episode, data.results[0]),
                episode
            };
            subtitleSelections.set(episode, selection);
            select.onchange = () => {
                selection.candidate = data.results[Number(select.value)];
                selection.name = subtitleName(settings, episode, selection.candidate);
            };
            const save = element('button', 'Tải sub');
            save.onclick = () => saveSubtitle(selection, save);
            row.append(select, save);
            if (data.totalPages > 1) {
                let page = 1;
                const more = element('button', 'Xem thêm');
                more.onclick = async () => {
                    if (subtitleBusy) return;
                    more.disabled = true;
                    try {
                        params.set('page', page + 1);
                        const next = await api(API_ROUTES.subtitle + '/search?' + params);
                        if (version !== subtitleVersion) return;
                        for (const candidate of next.results)
                            if (!data.results.some((item) => item.fileId === candidate.fileId)) {
                                select.append(new Option(option(candidate), String(data.results.length)));
                                data.results.push(candidate);
                            }
                        page++;
                        more.hidden = page >= next.totalPages;
                    } catch (error) {
                        $('subtitle-status').textContent = error.message;
                    } finally {
                        more.disabled = false;
                    }
                };
                row.append(more);
            }
        }
        $('subtitle-results').append(row);
    }
    $('subtitle-save-all').hidden = !subtitleSelections.size;
    return missing;
}

async function downloadSubtitleZip(selections, name, missing = []) {
    if (!selections.length) throw Error('Không tìm được phụ đề để tải.');
    nativeDownload(API_ROUTES.subtitle + '/native-archive', {
        payload: JSON.stringify({
            name,
            files: selections.map((selection) => ({ fileId: selection.candidate.fileId, episode: selection.episode })),
            missing
        })
    });
    $('subtitle-status').textContent = 'Đã chuyển ZIP sang Downloads của trình duyệt (Ctrl+J).';
}

$('subtitle-search').onclick = async () => {
    if (subtitleBusy) return;
    const episodes = [...$('episodes').querySelectorAll('input:checked')].map((box) => Number(box.value));
    if (!episodes.length) {
        $('subtitle-status').textContent = 'Chọn ít nhất một tập hoặc bấm Tải full sub.';
        return;
    }
    subtitleControls(true);
    const version = ++subtitleVersion;
    try {
        const missing = await findSubtitles(episodes, subtitleSettings(), version);
        $('subtitle-status').textContent =
            `Đã tự chọn bản sub đầu tiên cho ${subtitleSelections.size} tập. ${missing.length ? missing.length + ' tập không có sub.' : ''}`;
    } catch (error) {
        if (version === subtitleVersion && error.name !== 'AbortError')
            $('subtitle-status').textContent = error.message;
    } finally {
        subtitleControls(false);
    }
};
$('subtitle-full').onclick = async () => {
    if (subtitleBusy) return;
    subtitleControls(true);
    const version = ++subtitleVersion;
    try {
        const settings = subtitleSettings(),
            episodes = available.map((ep) => Number(ep.episode_number));
        if (!episodes.length) throw Error('Anime chưa có danh sách tập.');
        const name = safeSubtitleName(settings.q) + `.${settings.language}.subtitles.zip`;
        const missing = await findSubtitles(episodes, settings, version);
        if (version !== subtitleVersion) return;
        await downloadSubtitleZip([...subtitleSelections.values()], name, missing);
    } catch (error) {
        if (version === subtitleVersion && error.name !== 'AbortError')
            $('subtitle-status').textContent = error.message;
    } finally {
        subtitleControls(false);
    }
};
$('subtitle-save-all').onclick = async () => {
    if (subtitleBusy) return;
    const selections = [...subtitleSelections.values()];
    if (!selections.length) return;
    subtitleControls(true);
    try {
        const settings = subtitleSettings(),
            name = safeSubtitleName(settings.q) + `.${settings.language}.subtitles.zip`;
        await downloadSubtitleZip(selections, name);
    } catch (error) {
        if (error.name !== 'AbortError') $('subtitle-status').textContent = error.message;
    } finally {
        subtitleControls(false);
    }
};
$('download-bundle').onclick = () => {
    if (subtitleBusy) return;
    try {
        const settings = subtitleSettings();
        const episodes = [...$('episodes').querySelectorAll('input:checked')].map((box) => Number(box.value));
        if (!episodes.length) throw Error('Chọn ít nhất một tập để tải video kèm phụ đề.');
        const pageUrl = $('original-page').value.trim();
        if (pageUrl && (episodes.length !== 1 || !/^https:\/\/flixcloud\.cc\/d\/[\w-]+\/?$/.test(pageUrl)))
            throw Error('Link Download nhập tay chỉ áp dụng khi chọn một tập.');
        const subtitles = episodes.flatMap((episode) => {
            const candidate = subtitleSelections.get(episode)?.candidate;
            return candidate &&
            candidate.language === settings.language &&
            (settings.movie || candidate.season === settings.season)
                ? [{ episode, fileId: candidate.fileId }]
                : [];
        });
        const payload = {
            slug: selectedAnime.anime_id,
            title: settings.q,
            episodes,
            server: $('original-server').value,
            audio: $('audio').value,
            language: settings.language,
            season: settings.season,
            imdb: settings.imdb,
            movie: settings.movie,
            pageUrl,
            subtitles
        };
        nativeDownload(API_ROUTES.bundle + '/native', {
            payload: JSON.stringify(payload),
            source: selectedAnimeSource
        });
        $('subtitle-status').textContent =
            'Đã chuyển gói video + sub sang Downloads của trình duyệt (Ctrl+J). Giải nén ZIP vào Videos để có thư mục ' +
            settings.q +
            '/EP…';
    } catch (error) {
        $('subtitle-status').textContent = error.message;
    }
};
