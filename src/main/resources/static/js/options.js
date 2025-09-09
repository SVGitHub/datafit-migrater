// ==============================
// options.js - Options Page
// ==============================

import { apiFetch, showToast } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    const optionsForm = document.querySelector("#optionsForm");

    async function loadOptions() {
        try {
            const options = await apiFetch("/api/options");
            Object.entries(options).forEach(([key, value]) => {
                const field = optionsForm.querySelector(`[name="${key}"]`);
                if (field) field.value = value;
            });
        } catch {
            showToast("Failed to load options", "error");
        }
    }

    optionsForm?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const data = Object.fromEntries(new FormData(optionsForm));

        try {
            await apiFetch("/api/options", {
                method: "POST",
                body: JSON.stringify(data)
            });
            showToast("Options saved", "success");
        } catch {
            showToast("Failed to save options", "error");
        }
    });

    loadOptions();
});