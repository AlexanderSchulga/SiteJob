// Фоновая карусель
document.addEventListener('DOMContentLoaded', function() {
    const slides = document.querySelectorAll('.carousel-slide');
    let currentIndex = 0;

    // Функция переключения слайда
    function nextSlide() {
        // Убираем активный класс у текущего слайда
        slides[currentIndex].classList.remove('active');

        // Переходим к следующему слайду
        currentIndex = (currentIndex + 1) % slides.length;

        // Добавляем активный класс новому слайду
        slides[currentIndex].classList.add('active');
    }

    // Запускаем интервал, если есть слайды
    if (slides.length > 1) {
        setInterval(nextSlide, 3000);
    }
});