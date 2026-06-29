const SupportPermissions = {
    // Право на просмотр тикета пользователем
    canSeeTicket(comment, currentUser) {
        if (!comment || !currentUser) return false;

        // Важно: проверяем hasSuperAccess для SUPER и ADMIN
        const isSuperOrAdmin = currentUser.role === 'ROLE_SUPER' || currentUser.role === 'ROLE_ADMIN';
        const isOwnTicket = comment.user.id === currentUser.id;

        // SUPER/ADMIN видят все тикеты
        if (isSuperOrAdmin) return true;

        // Обычный пользователь видит только свои тикеты
        return isOwnTicket;
    },

    // Право на отправку ответа в тикете
    canReplyToComment(comment, currentUser) {
        if (!comment || !currentUser) return false;

        const isSuperOrAdmin = currentUser.role === 'ROLE_SUPER' || currentUser.role === 'ROLE_ADMIN';
        const isAssigned = comment.assignedSuper != null;
        const isAssignedToCurrent = isAssigned &&
            comment.assignedSuper.id === currentUser.id;
        const isOwnTicket = comment.user.id === currentUser.id;
        const isRootComment = !comment.parentComment;

        // Нельзя отвечать на ответы (только на корневые комментарии)
        if (!isRootComment) return false;

        // Для SUPER/ADMIN: можно отвечать только если тикет назначен на них
        if (isSuperOrAdmin) {
            return isAssignedToCurrent;
        }

        // Для пользователей: можно отвечать только в свои тикеты
        // и только если у них еще нет ответов
        const hasReplies = comment.replies && comment.replies.length > 0;
        return isOwnTicket && !hasReplies;
    },

    // Право на удаление комментария
    canDelete(comment, currentUser) {
        if (!comment || !currentUser) return false;

        const isSuperOrAdmin = currentUser.role === 'ROLE_SUPER' || currentUser.role === 'ROLE_ADMIN';
        const isRootComment = !comment.parentComment;

        // Только SUPER/ADMIN могут удалять корневые комментарии
        if (isSuperOrAdmin && isRootComment) {
            const isAssigned = comment.assignedSuper != null;
            const isAssignedToCurrent = isAssigned &&
                comment.assignedSuper.id === currentUser.id;

            // SUPER/ADMIN могут удалять только те тикеты, которые назначены на них
            return !isAssigned || isAssignedToCurrent;
        }

        // SUPER/ADMIN могут удалять любые ответы
        if (isSuperOrAdmin && !isRootComment) {
            return comment.user.id === currentUser.id;
        }

        return false;
    },

    // Право на то, чтобы SUPER/ADMIN взял/назначил тикет
    canManageTicket(comment, currentUser) {
        if (!comment || !currentUser) return false;

        const isSuperOrAdmin = currentUser.role === 'ROLE_SUPER' || currentUser.role === 'ROLE_ADMIN';
        const isRootComment = !comment.parentComment;
        const isAssigned = comment.assignedSuper != null;
        const isAssignedToCurrent = isAssigned &&
            comment.assignedSuper.id === currentUser.id;

        // Только SUPER/ADMIN могут управлять назначением тикетов
        return isSuperOrAdmin && isRootComment;
    }
};

// Экспорт
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SupportPermissions;
} else {
    window.SupportPermissions = SupportPermissions;
}