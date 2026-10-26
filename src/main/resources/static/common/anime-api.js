const API_ROUTES = Object.freeze({
    anime: '/api/v1/anime',
    original: '/api/v1/original',
    subtitle: '/api/v1/subtitle',
    bundle: '/api/v1/bundle'
});
const $ = (id) => document.getElementById(id);
let currentSource = localStorage.getItem('ergo-proxy.anime-source') || 'reanime',
    selectedAnimeSource;

function sourceUrl(url, source = currentSource) {
    return url + (url.includes('?') ? '&' : '?') + 'source=' + encodeURIComponent(source);
}

let selectedAnime,
    available = [],
    searchVersion = 0,
    episodeVersion = 0;
const title = (anime) =>
    typeof anime.title === 'string' ? anime.title : anime.title?.english || anime.title?.romaji || anime.anime_id;

function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text) node.textContent = text;
    if (className) node.className = className;
    return node;
}

async function api(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw Error(error.message || `Lỗi HTTP ${response.status}`);
    }
    const result = await response.json();
    return result && typeof result.status === 'number' && Object.prototype.hasOwnProperty.call(result, 'data')
        ? result.data
        : result;
}

function report(error) {
    $('message').textContent = error.message;
}
