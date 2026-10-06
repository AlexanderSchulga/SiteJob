package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkMessagesAsReadService {

    private final CommentRepoPersonal commentRepoPersonal;
    private final CommentRepoJobPersonal commentRepoJobPersonal;
    private final CommentRepoVacPersonal commentRepoVacPersonal;
    private final CommentRepoResPersonal commentRepoResPersonal;

    /**
     * Отмечает сообщения в переписке как прочитанные для текущего пользователя.
     * Используется для уведомлений о непрочитанных сообщениях.
     * Ошибки пробрасываются наверх — обрабатываются GlobalExceptionHandler.
     *
     * @param postId         ID поста (не hashId!)
     * @param authentication данные аутентификации пользователя
     */
    @Transactional
    public ResponseEntity<?> markMessagesAsRead(Long postId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        commentRepoPersonal.markMessagesAsRead(postId, currentUser.getId());
        log.debug("Отмечены сообщения как прочитанные, postId: {}", postId);
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> markMessagesAsReadJob(Long postId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        commentRepoJobPersonal.markMessagesAsRead(postId, currentUser.getId());
        log.debug("Отмечены сообщения как прочитанные, postId: {}", postId);
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> markMessagesAsReadVac(Long postId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        commentRepoVacPersonal.markMessagesAsRead(postId, currentUser.getId());
        log.debug("Отмечены сообщения как прочитанные, postId: {}", postId);
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> markMessagesAsReadRes(Long resId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        commentRepoResPersonal.markMessagesAsRead(resId, currentUser.getId());
        log.debug("Отмечены сообщения как прочитанные, resId: {}", resId);
        return ResponseEntity.ok().build();
    }
}