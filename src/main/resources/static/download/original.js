// Attachment navigation lets Chrome own the download, Save As and progress.
function nativeDownload(url, fields) {
    const frame = document.createElement('iframe');
    frame.hidden = true;
    frame.name = 'download-' + crypto.randomUUID();
    frame.title = 'Tải xuống';
    frame.onload = () => {
        try {
            const text = frame.contentDocument?.body?.textContent;
            if (text?.trim().startsWith('{')) {
                const error = JSON.parse(text);
                if (error.message) report(Error(error.message));
            }
        } catch {
        }
    };
    document.body.append(frame);
    if (fields) {
        const form = document.createElement('form');
        form.hidden = true;
        form.method = 'POST';
        form.action = url;
        form.target = frame.name;
        for (const [key, value] of Object.entries(fields)) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = key;
            input.value = value;
            form.append(input);
        }
        document.body.append(form);
        form.submit();
        form.remove();
    } else frame.src = url;
}

let originalBusy = false;
$('download').onclick = async () => {
    if (originalBusy) return;
    const episodes = [...$('episodes').querySelectorAll('input:checked')].map((box) => Number(box.value));
    const anime = selectedAnime,
        server = $('original-server').value,
        audio = $('audio').value,
        pageUrl = $('original-page').value.trim();
    if (!episodes.length) {
        report(Error('Chọn ít nhất một tập.'));
        return;
    }
    if (pageUrl && (episodes.length !== 1 || !/^https:\/\/flixcloud\.cc\/d\/[\w-]+\/?$/.test(pageUrl))) {
        report(Error('Link /d/ chỉ dùng cho một tập đã chọn.'));
        return;
    }
    const source = selectedAnimeSource;
    originalBusy = true;
    $('anime-source').disabled = true;
    $('download').disabled = true;
    try {
        for (let index = 0; index < episodes.length; index++) {
            const episode = episodes[index];
            $('message').textContent =
                'Đang chuẩn bị nguồn tập ' + episode + ' (' + (index + 1) + '/' + episodes.length + ')…';
            let ticket = await api(sourceUrl(API_ROUTES.original, source), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ slug: anime.anime_id, episode, server, audio, pageUrl })
            });
            while (ticket.state !== 'READY') {
                if (ticket.state === 'FAILED') throw Error(ticket.message);
                await new Promise((resolve) => setTimeout(resolve, 1000));
                ticket = await api(sourceUrl(API_ROUTES.original + '/' + ticket.id, source));
            }
            nativeDownload(sourceUrl(API_ROUTES.original + '/' + ticket.id + '/content', source));
        }
        $('message').textContent =
            'Đã chuyển ' +
            episodes.length +
            ' video sang Downloads của trình duyệt (Ctrl+J). Theo dõi tiến trình và hủy tải tại đó.';
    } catch (error) {
        report(error);
    } finally {
        originalBusy = false;
        $('anime-source').disabled = false;
        $('download').disabled = false;
    }
};
