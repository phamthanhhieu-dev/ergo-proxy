import './navigation.js';
import { sessionStore } from './storage.js';

(() => {
    const get = (id) => document.getElementById(id);
    get('choose-video').textContent = '▶ Mở video trên thiết bị';
    get('choose-video').onclick = () => window.ErgoNavigation.go('watch', '?local=file');
    get('choose-folder').textContent = 'Mở thư mục trên thiết bị';
    get('choose-folder').onclick = () => window.ErgoNavigation.go('watch', '?local=folder');
    get('server-library').onclick = () => {
        sessionStore.set('ergo.playback-mode', 'server');
        window.ErgoNavigation.go('watch');
    };
})();
