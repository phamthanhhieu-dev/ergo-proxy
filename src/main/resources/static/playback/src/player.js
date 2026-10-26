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
        this.video.src = fileUrl(entry.video.id);
        this.video.load();
        this.report('Đang đọc file gốc từ ổ đĩa • Hỗ trợ tua • Không chuyển mã video.');
    }
}
