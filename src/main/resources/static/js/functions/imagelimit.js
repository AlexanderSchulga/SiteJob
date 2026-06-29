function showFileSizeToast(message, isError = true) {
    const existingToast = document.querySelector('.file-size-toast');
    if (existingToast) {
        existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.className = `file-size-toast ${isError ? 'toast-error' : 'toast-success'}`;
    toast.innerHTML = `
        <div class="toast-content">
            <div class="toast-icon">${isError ? '⚠️' : '✅'}</div>
            <div class="toast-message">${message}</div>
            <button class="toast-close">&times;</button>
        </div>
        <div class="toast-progress"></div>
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    });

    setTimeout(() => {
        if (toast.parentElement) {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }
    }, 4000);
}

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('productForm');
    const fileInput = document.getElementById('files');

    if (form && fileInput) {
        form.addEventListener('submit', function(e) {
            let totalSize = 0;
            const files = fileInput.files;

            if (files.length === 0) {
                e.preventDefault();
                showFileSizeToast('📸 Пожалуйста, выберите хотя бы одно изображение');
                return false;
            }

            for (let i = 0; i < files.length; i++) {
                totalSize += files[i].size;
            }

            const totalSizeMB = totalSize / (1024 * 1024);

            if (totalSizeMB > 20) {
                e.preventDefault();
                showFileSizeToast(`❗ Общий размер: ${totalSizeMB.toFixed(2)} MB (макс. 20 MB)`);
                return false;
            }

            for (let i = 0; i < files.length; i++) {
                const fileSizeMB = files[i].size / (1024 * 1024);
                if (fileSizeMB > 20) {
                    e.preventDefault();
                    showFileSizeToast(`❗ "${files[i].name}" — ${fileSizeMB.toFixed(2)} MB (макс. 20 MB)`);
                    return false;
                }
            }

            return true;
        });
    }
});