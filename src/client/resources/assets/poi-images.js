"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const field = document.getElementById("poi-image-input");
    if (!field) return; // Only active on the POI form.

    const panel = document.getElementById("poi-image-preview");
    const preview = document.getElementById("poi-image-preview-img");
    const filename = document.getElementById("poi-image-filename");
    const error = document.getElementById("poi-image-error");
    const clear = document.getElementById("poi-image-clear");
    const allowed = new Set(["image/jpeg", "image/png", "image/webp"]);
    const maxBytes = 5 * 1024 * 1024;
    let previewUrl = null;

    function reset() {
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = null;
        preview.removeAttribute("src");
        field.value = "";
        panel.hidden = true;
    }

    field.addEventListener("change", () => {
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = null;
        const file = field.files && field.files[0];
        error.hidden = true;
        if (!file) { panel.hidden = true; return; }
        if (!allowed.has(file.type) || file.size === 0 || file.size > maxBytes) {
            error.textContent = "Chỉ chọn ảnh JPG, PNG hoặc WebP, dung lượng tối đa 5 MB.";
            error.hidden = false;
            reset();
            return;
        }
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        filename.textContent = file.name + " (" + (file.size / 1024 / 1024).toFixed(2) + " MB)";
        panel.hidden = false;
    });

    clear.addEventListener("click", () => { reset(); error.hidden = true; });
    document.querySelectorAll(".poi-photo-delete-form").forEach(form => {
        form.addEventListener("submit", event => {
            if (!window.confirm("Bạn muốn xóa ảnh địa điểm này?")) event.preventDefault();
        });
    });
    window.addEventListener("pagehide", () => { if (previewUrl) URL.revokeObjectURL(previewUrl); });
});
