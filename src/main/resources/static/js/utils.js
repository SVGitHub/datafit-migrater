// ==============================
// utils.js - shared helpers
// ==============================

// Toast notification
export function showToast(message, type = "info") {
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span>${message}</span>
    `;
    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add("visible"), 50);
    setTimeout(() => {
        toast.classList.remove("visible");
        toast.addEventListener("transitionend", () => toast.remove());
    }, 3000);
}

// Loader control
export function showLoader() {
    let loader = document.querySelector("#global-loader");
    if (!loader) {
        loader = document.createElement("div");
        loader.id = "global-loader";
        loader.innerHTML = `<div class="spinner"></div>`;
        document.body.appendChild(loader);
    }
    loader.style.display = "flex";
}

export function hideLoader() {
    const loader = document.querySelector("#global-loader");
    if (loader) loader.style.display = "none";
}

// Fetch wrapper
export async function apiFetch(url, options = {}) {
    try {
        showLoader();
        const response = await fetch(url, {
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            },
            credentials: "include",
            ...options
        });
        hideLoader();

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP ${response.status}`);
        }

        return response.json();
    } catch (err) {
        hideLoader();
        showToast(err.message, "error");
        throw err;
    }
}

// Confirmation modal
export function confirmDialog(message, onConfirm) {
    if (confirm(message)) {
        onConfirm();
    }
}