// ==============================
// help.js - Help Page Logic
// ==============================

import { showToast } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    const faqItems = document.querySelectorAll(".faq-item");

    faqItems.forEach(item => {
        item.addEventListener("click", () => {
            const answer = item.querySelector(".faq-answer");
            answer.classList.toggle("open");
        });
    });

    // Feedback form
    const feedbackForm = document.querySelector("#feedbackForm");
    if (feedbackForm) {
        feedbackForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const formData = new FormData(feedbackForm);

            try {
                const res = await fetch("/api/feedback", {
                    method: "POST",
                    body: JSON.stringify(Object.fromEntries(formData)),
                    headers: { "Content-Type": "application/json" }
                });

                if (res.ok) {
                    showToast("Thank you for your feedback!", "success");
                    feedbackForm.reset();
                } else {
                    showToast("Failed to send feedback", "error");
                }
            } catch (err) {
                showToast("Error: " + err.message, "error");
            }
        });
    }
});