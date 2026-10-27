// Storage can be blocked, full, or contain invalid JSON. Callers decide how to report failures.
export function createStorage(scope = "localStorage", onError = () => {}) {
    return {
        get(key, fallback = null) {
            try {
                return window[scope].getItem(key) ?? fallback;
            } catch (error) {
                onError(error);
                return fallback;
            }
        },
        set(key, value) {
            try {
                window[scope].setItem(key, value);
                return true;
            } catch (error) {
                onError(error);
                return false;
            }
        },
        remove(key) {
            try {
                window[scope].removeItem(key);
            } catch (error) {
                onError(error);
            }
        },
        read(key, fallback = null) {
            const raw = this.get(key);
            if (raw === null) {
                return fallback;
            }
            try {
                return JSON.parse(raw);
            } catch (error) {
                onError(error);
                return fallback;
            }
        },
        write(key, value) {
            try {
                return this.set(key, JSON.stringify(value));
            } catch (error) {
                onError(error);
                return false;
            }
        },
    };
}
export const localStore = createStorage();
export const sessionStore = createStorage("sessionStorage");
