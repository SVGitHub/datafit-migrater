// Extended Options UI
document.addEventListener("DOMContentLoaded", () => {
  const optionsDiv = document.getElementById("options");
  if (optionsDiv) {
    optionsDiv.innerHTML = "<h2>System Options</h2><p>Configure keys, chunk size, parallelism, retention, etc.</p>";
  }
});