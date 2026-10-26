import { $, API_ROUTES, api, sourceUrl, report } from '../common/anime-api.js';
import { nativeDownload } from '../common/native-download.js';
import { downloadState, selectedEpisodes, validateOriginalPage } from './state.js';

let originalBusy = false;
$('download').onclick = async () => {
    if (originalBusy) {
        return;
    }
    const episodes = selectedEpisodes();
    const anime = downloadState.anime,
        server = $('original-server').value,
        audio = $('audio').value,
        pageUrl = $('original-page').value.trim();
    if (!episodes.length) {
        report(Error('Chọn ít nhất một tập.'));
        return;
    }
    try {
        validateOriginalPage(pageUrl, episodes);
    } catch (error) {
        report(error);
        return;
    }
    const source = downloadState.source;
    originalBusy = true;
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
            const deadline = Date.now() + 10 * 60 * 1000;
            while (ticket.state !== 'READY') {
                if (Date.now() > deadline) {
                    throw new Error('Nguồn tải chưa sẵn sàng sau 10 phút. Hãy thử lại.');
                }
                if (ticket.state === 'FAILED') {
                    throw Error(ticket.message);
                }
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
        $('download').disabled = false;
    }
};
