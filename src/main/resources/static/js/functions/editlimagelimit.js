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
    // Для товаров
    const productForm = document.getElementById('productForm');
    const productFiles = document.getElementById('product_files');
    if (productForm && productFiles) {
        productForm.addEventListener('submit', function(e) {
            let totalSize = 0;
            const files = productFiles.files;

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
                if (files[i].size / (1024 * 1024) > 20) {
                    e.preventDefault();
                    showFileSizeToast(`❗ "${files[i].name}" — ${(files[i].size / (1024 * 1024)).toFixed(2)} MB (макс. 20 MB)`);
                    return false;
                }
            }
            return true;
        });
    }

    // Для услуг
    const serviceForm = document.getElementById('serviceForm');
    const serviceFiles = document.getElementById('service_files');
    if (serviceForm && serviceFiles) {
        serviceForm.addEventListener('submit', function(e) {
            let totalSize = 0;
            const files = serviceFiles.files;

            for (let i = 0; i < files.length; i++) {
                totalSize += files[i].size;
            }

            const totalSizeMB = totalSize / (1024 * 1024);
            if (totalSizeMB > 20) {
                e.preventDefault();
                showFileSizeToast(`❗ Общий размер: ${totalSizeMB.toFixed(2)} MB (макс. 20 MB)`);
                return false;
            }
            return true;
        });
    }

    // Для вакансий
    const vacancyForm = document.getElementById('vacancyForm');
    const vacancyFiles = document.getElementById('vacancy_files');
    if (vacancyForm && vacancyFiles) {
        vacancyForm.addEventListener('submit', function(e) {
            let totalSize = 0;
            const files = vacancyFiles.files;

            for (let i = 0; i < files.length; i++) {
                totalSize += files[i].size;
            }

            const totalSizeMB = totalSize / (1024 * 1024);
            if (totalSizeMB > 20) {
                e.preventDefault();
                showFileSizeToast(`❗ Общий размер: ${totalSizeMB.toFixed(2)} MB (макс. 20 MB)`);
                return false;
            }
            return true;
        });
    }
});