import { report } from "./anime-api.js";

// Attachment navigation lets Chrome own the download, Save As and progress.
export function nativeDownload(url, fields) {
    const frame = document.createElement("iframe");
    frame.hidden = true;
    frame.name = "download-" + crypto.randomUUID();
    frame.title = "Tải xuống";
    frame.onload = () => {
        try {
            const text = frame.contentDocument?.body?.textContent;
            if (text?.trim().startsWith("{")) {
                const error = JSON.parse(text);
                if (error.message) {
                    report(Error(error.message));
                }
            }
        } catch {}
    };
    document.body.append(frame);
    if (fields) {
        const form = document.createElement("form");
        form.hidden = true;
        form.method = "POST";
        form.action = url;
        form.target = frame.name;
        for (const [key, value] of Object.entries(fields)) {
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = key;
            input.value = value;
            form.append(input);
        }
        document.body.append(form);
        form.submit();
        form.remove();
    } else {
        frame.src = url;
    }
}
