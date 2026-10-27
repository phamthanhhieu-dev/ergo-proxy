// Reusable input-driven search. Cancellation happens as soon as the query changes,
// before the debounce expires, so an older response cannot replace current suggestions.
export class LiveSearch {
    constructor({ input, onSearch, onCancel, onClear, delay = 350 }) {
        Object.assign(this, { input, onSearch, onCancel, onClear, delay });
        this.timer = null;
        this.composing = false;
        input.addEventListener("compositionstart", () => {
            this.composing = true;
            this.cancel();
        });
        input.addEventListener("compositionend", () => {
            this.composing = false;
            this.schedule();
        });
        input.addEventListener("input", (event) => {
            if (!this.composing && !event.isComposing) {
                this.schedule();
            }
        });
    }
    cancel() {
        clearTimeout(this.timer);
        this.timer = null;
        this.onCancel();
    }
    schedule() {
        this.cancel();
        const query = this.input.value.trim();
        this.onClear();
        if (!query) {
            return;
        }
        if (this.delay <= 0) {
            this.submit();
        } else {
            this.timer = setTimeout(() => this.submit(), this.delay);
        }
    }
    submit() {
        this.cancel();
        const query = this.input.value.trim();
        if (!query) {
            this.onClear();
            return;
        }
        if (!this.composing) {
            void this.onSearch(query);
        }
    }
}
