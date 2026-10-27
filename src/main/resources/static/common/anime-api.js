import { localStore } from './storage.js';
export { requestJson as api } from './http.js';
export const API_ROUTES = Object.freeze({
    anime: '/api/v1/anime',
    original: '/api/v1/original',
    subtitle: '/api/v1/subtitle',
    bundle: '/api/v1/bundle'
});
export const $ = (id) => document.getElementById(id);
export function sourceUrl(url, source = localStore.get('ergo-proxy.anime-source', 'reanime')) {
    const result = new URL(url, location.origin);
    result.searchParams.set('source', source);
    return result.pathname + result.search;
}
export function title(anime) {
    return typeof anime.title === 'string'
        ? anime.title
        : anime.title?.english || anime.title?.romaji || anime.anime_id;
}
export function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text) {
        node.textContent = text;
    }
    if (className) {
        node.className = className;
    }
    return node;
}
export function report(error) {
    $('message').textContent = error.message;
}
