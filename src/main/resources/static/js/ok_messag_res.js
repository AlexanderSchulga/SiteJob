// для автоматической отметки сообщений как прочитанных

document.addEventListener('DOMContentLoaded', function() {
    // При клике на колокольчик сразу отмечаем сообщения как прочитанные
    const notificationButtons = document.querySelectorAll('.notification-btn');
    notificationButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const url = this.getAttribute('href');
            const postId = url.split('/').pop();

            // Отправляем запрос на сервер для отметки как прочитанных
            fetch(`/api/messages/res/${postId}/markAsRead`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]').value
                }
            }).catch(err => console.log('Error marking as read'));

            // Продолжаем переход по ссылке
            // (не предотвращаем стандартное поведение)
        });
    });

    // Остальной код для валидации файлов...
    const productFiles = document.getElementById('product_files');
    const serviceFiles = document.getElementById('service_files');

    if (productFiles) {
        productFiles.addEventListener('change', function(e) {
            const files = e.target.files;
            if (files.length > 10) {
                alert('Можно загрузить не более 10 изображений');
                e.target.value = '';
            }
        });
    }

    if (serviceFiles) {
        serviceFiles.addEventListener('change', function(e) {
            const files = e.target.files;
            if (files.length > 10) {
                alert('Можно загрузить не более 10 изображений');
                e.target.value = '';
            }
        });
    }
});