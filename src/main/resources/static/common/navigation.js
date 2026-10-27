import { sessionStore } from "./storage.js";

(() => {
    const pages = {
        main: "/search/index.html",
        download: "/download/index.html",
        watch: "/playback/index.html",
        online: "/online/index.html",
    };
    const current = document.body.dataset.page || "main";
    document.body.dataset.view = current;
    document.body.classList.toggle("watching", current === "watch");
    const button = document.getElementById("view-back");
    button.hidden = current === "main";
    button.onclick = () => {
        if (history.state?.ergoPrevious) {
            history.back();
        } else {
            location.assign("/search/index.html");
        }
    };
    window.ErgoNavigation = {
        current,
        go(view, query = "") {
            if (view === current) {
                return;
            }
            sessionStore.set("ergo.previous", location.pathname);
            location.assign((pages[view] || "/") + query);
        },
        back() {
            button.click();
        },
        reset() {
            if (current !== "main") {
                location.assign("/search/index.html");
            }
        },
    };
    if (!history.state?.ergoPage) {
        const previous = sessionStore.get("ergo.previous");
        history.replaceState(
            {
                ergoPage: current,
                ergoPrevious: previous && previous !== location.pathname,
            },
            "",
        );
        sessionStore.remove("ergo.previous");
    }
})();
