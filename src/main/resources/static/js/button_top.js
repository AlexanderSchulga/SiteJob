
document.addEventListener('DOMContentLoaded', function() {
    let mybutton = document.getElementById("myBtn");

// Когда пользователь прокручивает документ вниз на 20px, показываем кнопку
    window.addEventListener('scroll', function () {
        if (document.body.scrollTop > 20 || document.documentElement.scrollTop > 20) {
            mybutton.style.display = "block";
        } else {
            mybutton.style.display = "none";
        }
    });

// Делаем функцию глобально доступной
    window.topFunction = function () {
        document.body.scrollTop = 0; // Для Safari
        document.documentElement.scrollTop = 0; // Для Chrome, Firefox, IE и Opera
    }
});