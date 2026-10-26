import { fileUrl } from './api';

/** Truyền nguyên file từ ổ đĩa; giải mã video thuộc về trình duyệt. */
export class VideoPlayer {
    constructor(video, report) {
        this.video = video;
        this.report = report;
    }
    stop() {
        this.video.pause();
        this.video.removeAttribute('src');
        this.video.load();
    }
    async open(entry) {
        this.stop();
        this.video.src = entry.video.url || fileUrl(entry.video.id);
        this.video.load();
        this.report(
            entry.video.url
                ? 'Đang đọc video trên thiết bị • Không tải video lên server.'
                : 'Đang đọc thư viện trên máy host • Hỗ trợ tua • Không chuyển mã video.'
        );
    }
}
