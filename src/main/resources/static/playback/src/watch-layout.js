export function watchLayout(online = false) {
    let markup = `
<section id="watch-panel" hidden>
  <div class="section-head"><div><div class="eyebrow">ĐANG XEM / THƯ VIỆN CỤC BỘ</div><h2 id="watch-title">Chọn video để bắt đầu</h2></div></div>
  <div class="watch-layout"><div class="watch-main">
    <div class="video-frame"><div id="video-stage"><video id="local-video" controls playsinline preload="metadata" disablepictureinpicture></video>
      <div id="resume-prompt" class="resume-prompt" role="group" aria-label="Tiếp tục xem" hidden>
        <span>Bạn đang xem dở tập này</span>
        <div><button id="resume-continue" type="button">Tiếp tục xem</button><button id="resume-restart" class="secondary" type="button">Xem từ đầu</button></div>
      </div></div></div>
    <div class="player-toolbar"><button id="previous-episode" class="secondary">← Tập trước</button><button id="next-episode" class="secondary">Tập tiếp →</button></div>
    <p id="player-state" role="status" aria-live="polite"></p>
    <div id="subtitle-editor" class="subtitle-editor"></div>
  </div><aside class="episode-browser"><label>Bộ anime <select id="library-series"></select></label><div id="library-count" class="eyebrow"></div><div id="library-episodes"></div></aside></div>
  <p class="hint">Video được truyền nguyên file từ ổ đĩa qua mạng LAN. Phụ đề, font và độ trễ xử lý trên thiết bị đang xem. Khả năng phát video/âm thanh phụ thuộc codec trình duyệt hỗ trợ.</p>
</section>`;
    if (online) {
        markup = markup.replace('id="watch-panel" hidden', 'id="watch-panel"');
        markup = markup.replace(
            "ĐANG XEM / THƯ VIỆN CỤC BỘ",
            "ĐANG XEM / ONLINE",
        );
        markup = markup.replace('id="watch-title"', 'id="online-title"');
        const start = markup.indexOf('<div class="video-frame">');
        const end = markup.indexOf('<div class="player-toolbar">', start);
        markup =
            markup.slice(0, start) +
            `<div class="video-frame"><div id="online-stage"><iframe id="online-frame" title="Player FlixCloud" sandbox="allow-scripts allow-same-origin allow-presentation" allow="autoplay; fullscreen; picture-in-picture" allowfullscreen></iframe><canvas id="online-subtitles"></canvas></div></div>` +
            markup.slice(end);
        markup = markup.replace('id="subtitle-editor"', 'id="online-editor"');
        markup = markup.replace(
            '<div id="online-editor"',
            '<div id="online-downloads"></div><div id="online-editor"',
        );
        markup = markup.replace(
            "Video được truyền nguyên file từ ổ đĩa qua mạng LAN. Phụ đề, font và độ trễ xử lý trên thiết bị đang xem. Khả năng phát video/âm thanh phụ thuộc codec trình duyệt hỗ trợ.",
            "Nếu nguồn đang bật phụ đề, chọn Subtitles → Off trong player. Phụ đề riêng, font và độ trễ xử lý trên thiết bị đang xem.",
        );
    }
    return markup;
}
