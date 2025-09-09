// ==============================
// jobs.js - Jobs Page
// ==============================

import { apiFetch, showToast, confirmDialog } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    const jobsTable = document.querySelector("#jobsTable tbody");

    async function loadJobs() {
        try {
            const jobs = await apiFetch("/api/jobs");
            renderJobs(jobs);
        } catch {
            showToast("Failed to load jobs", "error");
        }
    }

    function renderJobs(jobs) {
        jobsTable.innerHTML = "";
        jobs.forEach(job => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${job.id}</td>
                <td>${job.name}</td>
                <td>${job.status}</td>
                <td>
                    <button class="btn-run" data-id="${job.id}">
                        <i class="fas fa-play"></i>
                    </button>
                    <button class="btn-delete" data-id="${job.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            jobsTable.appendChild(row);
        });
    }

    async function runJob(id) {
        try {
            await apiFetch(`/api/jobs/${id}/run`, { method: "POST" });
            showToast("Job started", "success");
            loadJobs();
        } catch {
            showToast("Failed to start job", "error");
        }
    }

    async function deleteJob(id) {
        confirmDialog("Delete this job?", async () => {
            try {
                await apiFetch(`/api/jobs/${id}`, { method: "DELETE" });
                showToast("Job deleted", "success");
                loadJobs();
            } catch {
                showToast("Failed to delete job", "error");
            }
        });
    }

    jobsTable?.addEventListener("click", (e) => {
        if (e.target.closest(".btn-run")) {
            runJob(e.target.closest(".btn-run").dataset.id);
        } else if (e.target.closest(".btn-delete")) {
            deleteJob(e.target.closest(".btn-delete").dataset.id);
        }
    });

    loadJobs();
});