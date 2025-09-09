// ==============================
// admin.js - Admin Page
// ==============================

import { apiFetch, showToast, confirmAction } from "./utils.js";

document.addEventListener("DOMContentLoaded", () => {
    const userTable = document.querySelector("#userTable tbody");
    const refreshBtn = document.querySelector("#refreshUsersBtn");

    // ---- Load Users ----
    async function loadUsers() {
        try {
            const users = await apiFetch("/api/admin/users");
            renderUsers(users);
        } catch {
            showToast("Failed to load users", "error");
        }
    }

    // ---- Render User Rows ----
    function renderUsers(users) {
        if (!userTable) return;
        userTable.innerHTML = "";

        if (!users || users.length === 0) {
            userTable.innerHTML = `<tr><td colspan="4" class="text-center text-gray-500">No users found</td></tr>`;
            return;
        }

        users.forEach(user => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="px-4 py-2">${user.id}</td>
                <td class="px-4 py-2">${user.username}</td>
                <td class="px-4 py-2">${user.role}</td>
                <td class="px-4 py-2 text-right">
                    <button class="editUserBtn text-blue-600 hover:underline" data-id="${user.id}">Edit</button>
                    <button class="deleteUserBtn text-red-600 hover:underline ml-2" data-id="${user.id}">Delete</button>
                </td>
            `;
            userTable.appendChild(tr);
        });

        attachRowEvents();
    }

    // ---- Attach Row Events ----
    function attachRowEvents() {
        document.querySelectorAll(".editUserBtn").forEach(btn =>
            btn.addEventListener("click", () => editUser(btn.dataset.id))
        );
        document.querySelectorAll(".deleteUserBtn").forEach(btn =>
            btn.addEventListener("click", () => deleteUser(btn.dataset.id))
        );
    }

    // ---- Edit User ----
    function editUser(userId) {
        // Could open a modal form (future improvement)
        showToast(`Edit user ${userId}`, "info");
    }

    // ---- Delete User ----
    async function deleteUser(userId) {
        const confirmed = await confirmAction("Are you sure you want to delete this user?");
        if (!confirmed) return;

        try {
            await apiFetch(`/api/admin/users/${userId}`, { method: "DELETE" });
            showToast("User deleted", "success");
            loadUsers();
        } catch {
            showToast("Failed to delete user", "error");
        }
    }

    // ---- Refresh Button ----
    refreshBtn?.addEventListener("click", loadUsers);

    // ---- Initial Load ----
    loadUsers();
});