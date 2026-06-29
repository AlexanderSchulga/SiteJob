document.addEventListener('DOMContentLoaded', function() {
    var siteheader = document.getElementById("site-header");
    if (!siteheader) return;

    var sticky = siteheader.offsetTop;

    // Функция обновления отступа для body
    function updateBodyPadding() {
        if (siteheader.classList.contains("sticky")) {
            // Когда хедер фиксированный - добавляем отступ равный его высоте
            var headerHeight = siteheader.offsetHeight;
            document.body.style.paddingTop = headerHeight + "px";
            document.body.classList.add('has-sticky-header');
        } else {
            // Когда хедер не фиксированный - убираем отступ
            document.body.style.paddingTop = "0px";
            document.body.classList.remove('has-sticky-header');
        }
    }

    function myFunction() {
        if (window.pageYOffset >= sticky) {
            siteheader.classList.add("sticky");
        } else {
            siteheader.classList.remove("sticky");
        }
        updateBodyPadding();
    }

    // Обработчик скролла
    window.onscroll = function() {
        if (window.innerWidth > 576) { // Применяем sticky только на широких экранах
            myFunction();
        } else {
            // На мобильных убираем sticky и отступ
            siteheader.classList.remove("sticky");
            document.body.style.paddingTop = "0px";
            document.body.classList.remove('has-sticky-header');
        }
    };

    // Обновляем отступ при изменении размера окна (высота хедера может меняться)
    window.addEventListener('resize', function() {
        updateBodyPadding();
        // Пересчитываем sticky offset при изменении размера
        sticky = siteheader.offsetTop;
        if (window.innerWidth <= 576) {
            siteheader.classList.remove("sticky");
            document.body.style.paddingTop = "0px";
            document.body.classList.remove('has-sticky-header');
        } else {
            myFunction();
        }
    });

    // Вызываем при загрузке
    updateBodyPadding();

    // Также обновляем при любых изменениях DOM (например, если хедер меняет высоту)
    var observer = new MutationObserver(function() {
        updateBodyPadding();
    });
    observer.observe(siteheader, { attributes: true, childList: true, subtree: true });
});