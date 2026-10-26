(() => {
    const get = (id) => document.getElementById(id);
    async function openLibrary(type) {
        const buttons = ['choose-video', 'choose-folder', 'server-library'].map(get);
        for (const button of buttons) {
            button.disabled = true;
        }
        get('playback-status').textContent = type ? 'Chọn video hoặc thư mục trên máy tính…' : 'Đang đọc thư viện…';
        try {
            const url = '/api/v1/playback' + (type ? '/selection?type=' + type : '/library');
            const response = await fetch(url, type ? { method: 'POST' } : {});
            const body = await response.json();
            if (!response.ok || body.status === 1) {
                throw Error(body.message || 'Không mở được thư viện.');
            }
            const library = body.data || body;
            if (!library.videos.length) {
                get('playback-status').textContent = 'Chưa chọn video hoặc thư mục chưa có video.';
                return;
            }
            sessionStorage.setItem('ergo.library', JSON.stringify(library));
            window.ErgoNavigation.go('watch');
        } catch (error) {
            get('playback-status').textContent = error.message;
        } finally {
            for (const button of buttons) {
                button.disabled = false;
            }
        }
    }
    get('choose-video').onclick = () => openLibrary('file');
    get('choose-folder').onclick = () => openLibrary('folder');
    get('server-library').onclick = () => openLibrary();
})();
