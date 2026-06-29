function showFileSizeToast(message, isError = true) {
    // Удаляем существующий тост, если есть
    const existingToast = document.querySelector('.file-size-toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Создаем элемент тоста
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

    // Анимация появления
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Закрытие по кнопке
    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    });

    // Автоматическое закрытие через 4 секунды
    setTimeout(() => {
        if (toast.parentElement) {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }
    }, 4000);
}

document.addEventListener('DOMContentLoaded', function() {
    const resumeForm = document.getElementById('resumeForm');
    const resumeFiles = document.getElementById('resume_files');

    if (resumeForm && resumeFiles) {
        resumeForm.addEventListener('submit', function(e) {
            let totalSize = 0;
            const files = resumeFiles.files;

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
});