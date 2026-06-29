/**
 * Валидация размера файлов для админского редактирования
 * Максимальный общий размер: 10 MB
 * Максимальный размер одного файла: 10 MB
 */

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

    // Универсальная функция проверки размера файлов
    function validateFileSize(fileInput, maxSizeMB = 20) {
        if (!fileInput || fileInput.files.length === 0) return true;

        let totalSize = 0;
        const files = fileInput.files;

        for (let i = 0; i < files.length; i++) {
            totalSize += files[i].size;
        }

        const totalSizeMB = totalSize / (1024 * 1024);

        if (totalSizeMB > maxSizeMB) {
            showFileSizeToast(`❗ Общий размер: ${totalSizeMB.toFixed(2)} MB (макс. ${maxSizeMB} MB)`);
            return false;
        }

        for (let i = 0; i < files.length; i++) {
            const fileSizeMB = files[i].size / (1024 * 1024);
            if (fileSizeMB > maxSizeMB) {
                showFileSizeToast(`❗ "${files[i].name}" — ${fileSizeMB.toFixed(2)} MB (макс. ${maxSizeMB} MB)`);
                return false;
            }
        }
        return true;
    }

    // Для товаров
    const productForm = document.getElementById('productForm');
    const productFiles = document.getElementById('product_files');
    if (productForm && productFiles) {
        productForm.addEventListener('submit', function(e) {
            if (!validateFileSize(productFiles, 20)) {
                e.preventDefault();
            }
        });
    }

    // Для услуг
    const serviceForm = document.getElementById('serviceForm');
    const serviceFiles = document.getElementById('service_files');
    if (serviceForm && serviceFiles) {
        serviceForm.addEventListener('submit', function(e) {
            if (!validateFileSize(serviceFiles, 20)) {
                e.preventDefault();
            }
        });
    }

    // Для вакансий
    const vacancyForm = document.getElementById('vacancyForm');
    const vacancyFiles = document.getElementById('vacancy_files');
    if (vacancyForm && vacancyFiles) {
        vacancyForm.addEventListener('submit', function(e) {
            if (!validateFileSize(vacancyFiles, 20)) {
                e.preventDefault();
            }
        });
    }

    // Для резюме
    const resumeForm = document.getElementById('resumeForm');
    const resumeFiles = document.getElementById('resume_files');
    if (resumeForm && resumeFiles) {
        resumeForm.addEventListener('submit', function(e) {
            if (!validateFileSize(resumeFiles, 20)) {
                e.preventDefault();
            }
        });
    }
});