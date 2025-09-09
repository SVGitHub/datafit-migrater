// ==============================
// auth.js - Authentication Page
// ==============================

import { apiFetch, showToast } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.querySelector("#loginForm");
    const ssoBtn = document.querySelector("#ssoLoginBtn");

    // ---- Local Login ----
    loginForm?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const formData = Object.fromEntries(new FormData(loginForm));

        try {
            const response = await apiFetch("/perform_login", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams(formData).toString()
            });

            if (response?.authenticated) {
                showToast("Login successful", "success");
                window.location.href = "/home";
            } else {
                showToast("Invalid username or password", "error");
            }
        } catch {
            showToast("Login failed", "error");
        }
    });

    // ---- SSO Login ----
    ssoBtn?.addEventListener("click", () => {
        // Redirect to OAuth2 login
        window.location.href = "/oauth2/authorization/google";
    });
});