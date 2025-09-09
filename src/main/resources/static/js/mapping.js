// mapping.js - Modernized Mapping Designer
// Uses fetch API + async/await + FontAwesome icons
// Redesign includes loaders, toast notifications, and confirmation dialogs.

document.addEventListener("DOMContentLoaded", () => {
  const mappingTable = document.getElementById("mappingTableBody");
  const saveBtn = document.getElementById("saveMappingBtn");
  const toastContainer = document.getElementById("toastContainer");

  // ✅ Utility: Show toast notifications
  function showToast(message, type = "success") {
    const icon =
      type === "success"
        ? "fa-check-circle"
        : type === "error"
        ? "fa-times-circle"
        : "fa-info-circle";

    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <i class="fas ${icon}"></i>
      <span>${message}</span>
    `;
    toastContainer.appendChild(toast);

    setTimeout(() => toast.remove(), 4000);
  }

  // ✅ Utility: Show loader while async actions
  function toggleLoader(show) {
    const loader = document.getElementById("loader");
    loader.style.display = show ? "flex" : "none";
  }

  // ✅ Fetch mappings for project
  async function loadMappings(projectId) {
    try {
      toggleLoader(true);
      const res = await fetch(`/api/mappings?projectId=${projectId}`);
      if (!res.ok) throw new Error("Failed to fetch mappings");
      const data = await res.json();

      renderMappings(data);
      showToast("Mappings loaded", "success");
    } catch (err) {
      console.error(err);
      showToast(err.message, "error");
    } finally {
      toggleLoader(false);
    }
  }

  // ✅ Render mappings into table
  function renderMappings(mappings) {
    mappingTable.innerHTML = "";
    if (!mappings || mappings.length === 0) {
      mappingTable.innerHTML =
        '<tr><td colspan="4" class="text-center text-muted">No mappings found</td></tr>';
      return;
    }

    mappings.forEach((m) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${m.filePattern}</td>
        <td>${m.mappingJson ? JSON.stringify(m.mappingJson) : "-"}</td>
        <td>
          <button class="btn btn-sm btn-outline-primary edit-btn" data-id="${
            m.id
          }">
            <i class="fas fa-edit"></i> Edit
          </button>
        </td>
      `;
      mappingTable.appendChild(row);
    });

    // Attach edit handlers
    document.querySelectorAll(".edit-btn").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        const id = e.currentTarget.dataset.id;
        editMapping(id);
      });
    });
  }

  // ✅ Save mapping
  async function saveMapping(projectId, filePattern, mappingJson) {
    try {
      toggleLoader(true);
      const res = await fetch("/api/mappings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projectId, filePattern, mappingJson }),
      });
      if (!res.ok) throw new Error("Failed to save mapping");

      const data = await res.json();
      showToast("Mapping saved successfully!", "success");
      loadMappings(projectId); // refresh list
    } catch (err) {
      console.error(err);
      showToast(err.message, "error");
    } finally {
      toggleLoader(false);
    }
  }

  // ✅ Edit mapping (basic example: load in modal)
  function editMapping(id) {
    // In real flow: fetch mapping details & open modal
    showToast(`Edit mapping ${id}`, "info");
  }

  // ✅ Event bindings
  saveBtn?.addEventListener("click", () => {
    const projectId = document.getElementById("projectId").value;
    const filePattern = document.getElementById("filePattern").value;
    const mappingJson = document.getElementById("mappingJson").value;

    if (!projectId || !filePattern || !mappingJson) {
      showToast("All fields are required", "error");
      return;
    }

    if (!confirm("Are you sure you want to save this mapping?")) return;

    saveMapping(projectId, filePattern, mappingJson);
  });

  // Initial load (example: first projectId from hidden input)
  const projectIdInput = document.getElementById("projectId");
  if (projectIdInput) {
    loadMappings(projectIdInput.value);
  }
});