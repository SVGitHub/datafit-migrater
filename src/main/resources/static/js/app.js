// ==============================
// app.js - global minimal logic
// ==============================

import { showToast } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    // Highlight active nav item
    const path = window.location.pathname;
    document.querySelectorAll("nav a").forEach(link => {
        if (link.getAttribute("href") === path) {
            link.classList.add("active");
        }
    });

    // Logout button handler
    const logoutBtn = document.querySelector("#logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async (e) => {
            e.preventDefault();
            try {
                await fetch("/logout", { method: "POST", credentials: "include" });
                window.location.href = "/login?logout";
            } catch (err) {
                showToast("Logout failed", "error");
            }
        });
    }
});