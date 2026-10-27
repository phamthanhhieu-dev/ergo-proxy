export const downloadState = { anime: null, source: 'reanime', episodes: [] };
export function selectedEpisodes() {
    const selected = document.querySelectorAll('#episodes input:checked');
    return [...selected].map((input) => Number(input.value));
}
export function validateOriginalPage(pageUrl, episodes) {
    if (pageUrl && (episodes.length !== 1 || !/^https:\/\/flixcloud\.cc\/d\/[\w-]+\/?$/.test(pageUrl))) {
        throw new Error('Link /d/ chỉ dùng cho một tập đã chọn.');
    }
}
