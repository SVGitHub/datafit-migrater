// main.js
function showToast(message, type = "info") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");

    let bg = "bg-primary";
    if (type === "success") bg = "bg-success";
    if (type === "error") bg = "bg-danger";
    if (type === "warning") bg = "bg-warning";

    toast.className = `toast align-items-center text-white ${bg} border-0 show`;
    toast.role = "alert";
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}