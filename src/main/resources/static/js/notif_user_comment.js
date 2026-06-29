// Проверяем, нужно ли обновить страницу после редиректа
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем, есть ли флаг обновления в sessionStorage
    if (sessionStorage.getItem('needsRefresh') === 'true') {
        sessionStorage.removeItem('needsRefresh');

        // Очищаем контейнер и сбрасываем пагинацию
        document.getElementById('ticketsContainer').innerHTML = '';
        currentPage = 0;
        hasNextPage = true;

        // Небольшая задержка для завершения операций на сервере
        setTimeout(function() {
            loadMoreTickets();
        }, 300);
    } else {
        // Обычная загрузка
        loadMoreTickets();
    }
});

// Отмечаем, что нужно обновить данные при уходе со страницы
window.addEventListener('beforeunload', function() {
    // Проверяем, что мы не обновляем страницу (F5)
    if (!sessionStorage.getItem('isRefreshing')) {
        sessionStorage.setItem('needsRefresh', 'true');
    }
});

// Сбрасываем флаг при обновлении страницы
window.addEventListener('load', function() {
    sessionStorage.removeItem('isRefreshing');
});

// Отслеживаем видимость страницы (возврат из редиректа)
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        // Страница стала видимой - возможно, вернулись с редиректа
        if (sessionStorage.getItem('needsRefresh') === 'true') {
            sessionStorage.removeItem('needsRefresh');

            // Перезагружаем данные
            document.getElementById('ticketsContainer').innerHTML = '';
            currentPage = 0;
            hasNextPage = true;
            loadMoreTickets();
        }
    }
});

let currentPage = 0;
let loading = false;
let hasNextPage = true;
const pageSize = 4;

function loadMoreTickets() {
    if (loading || !hasNextPage) return;

    loading = true;
    document.getElementById('loadingSpinner').classList.add('active');
    document.getElementById('loadMoreBtn').disabled = true;

    fetch(`/user/notifications_message/load?page=${currentPage}&size=${pageSize}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                console.log('Error');
                return;
            }

            displayTickets(data.tickets);
            hasNextPage = data.hasNext;
            currentPage++;

            // Обновляем видимость кнопки и сообщений
            updateUIState(data.tickets);
        })
        .catch(error => {
            console.log('Error loading tickets');
        })
        .finally(() => {
            loading = false;
            document.getElementById('loadingSpinner').classList.remove('active');
            document.getElementById('loadMoreBtn').disabled = false;
        });
}

function displayTickets(tickets) {
    const container = document.getElementById('ticketsContainer');

    if (tickets.length === 0 && currentPage === 0) {
        document.getElementById('noMessages').style.display = 'block';
        document.getElementById('loadMoreBtn').style.display = 'none';
        return;
    }

    tickets.forEach(ticket => {
        const ticketHtml = createTicketHtml(ticket);
        container.insertAdjacentHTML('beforeend', ticketHtml);
    });
}

function updateUIState(tickets) {
    // Если это первая страница и сообщений нет
    if (currentPage === 1 && tickets.length === 0) {
        document.getElementById('noMessages').style.display = 'block';
        document.getElementById('loadMoreBtn').style.display = 'none';
        document.getElementById('noMoreMessages').classList.remove('active');
    }
    // Если есть сообщения, но следующей страницы нет
    else if (!hasNextPage) {
        document.getElementById('loadMoreBtn').style.display = 'none';
        if (tickets.length > 0) {
            document.getElementById('noMoreMessages').classList.add('active');
        }
    }
    // Если есть следующая страница, показываем кнопку
    else {
        document.getElementById('loadMoreBtn').style.display = 'flex';
        document.getElementById('noMoreMessages').classList.remove('active');
    }
}

function createTicketHtml(ticket) {
    const postTypeText = ticket.postType === 'product' ? 'Товар' : ticket.postType === 'resume' ? 'Резюме' :
        (ticket.postType === 'service' ? 'Услуга' : 'Вакансия');

    const unreadClass = !ticket.read ? 'unread' : '';

    // Формируем URL в зависимости от типа поста с использованием нового контроллера
    let postUrl;
    if (ticket.postType === 'product') {
        postUrl = `/message/post/${ticket.postHashId}`;

    } else if (ticket.postType === 'service') {
        postUrl = `/message/service/${ticket.postHashId}`;

    } else if (ticket.postType === 'vacancy') {
        postUrl = `/message/vacancy/${ticket.postHashId}`;
    } else {
        postUrl = `/message/resume/${ticket.postHashId}`;
    }

    // Добавляем messageId для отметки конкретного сообщения как прочитанного
    if (ticket.messageId) {
        postUrl += `?messageId=${ticket.messageId}`;
    }

    return `
        <div class="message-ticket ${unreadClass}" onclick="window.location.href='${postUrl}'">
            <div class="message-info">
                <div class="message-from">
                    <i class="fas fa-user"></i>
                    <span>${escapeHtml(ticket.fromUserName)}</span>
                    <span class="post-type-badge type-${ticket.postType}">${postTypeText}</span>
                </div>
                <div class="message-post">
                    <i class="fas fa-tag"></i>
                    <span>${escapeHtml(ticket.postTitle)}</span>
                </div>
                <div class="message-preview">
                    <i class="fas fa-comment"></i>
                    <span>${escapeHtml(ticket.messagePreview)}</span>
                </div>
                <div class="message-time">
                    <i class="far fa-clock"></i>
                    <span>${formatDate(ticket.messageTime)}</span>
                </div>
            </div>
            <div class="btn-go">
                <i class="fas fa-arrow-right"></i> Перейти
            </div>
        </div>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateTimeStr) {
    const date = new Date(dateTimeStr);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}.${month}.${year} ${hours}:${minutes}`;
}

// ПРИНУДИТЕЛЬНОЕ ПРИЖАТИЕ ФУТЕРА
function forceFooterToBottom() {
    const footer = document.querySelector('footer');
    const wrapper = document.querySelector('.container-wrapper');

    if (!footer || !wrapper) return;

    // Вычисляем высоту контента без футера
    const wrapperRect = wrapper.getBoundingClientRect();
    const windowHeight = window.innerHeight;
    const footerHeight = footer.offsetHeight;

    // Если контент не доходит до низа, прижимаем футер
    if (wrapperRect.bottom + footerHeight < windowHeight) {
        wrapper.style.minHeight = (windowHeight - footerHeight - 50) + 'px';
    } else {
        wrapper.style.minHeight = '';
    }
}

// Вызываем после загрузки страницы и после каждой подгрузки тикетов
window.addEventListener('load', forceFooterToBottom);
window.addEventListener('resize', forceFooterToBottom);

// Вызываем после каждого обновления тикетов
const originalLoadMore = loadMoreTickets;
window.loadMoreTickets = function() {
    originalLoadMore();
    setTimeout(forceFooterToBottom, 100);
};

// Также добавляем в конец существующей функции loadMoreTickets
// или просто вызываем везде
setTimeout(forceFooterToBottom, 500);