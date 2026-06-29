package jobforsite.sitejob.module.shared.controller.supportandmessage;

import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.dto.SupportCommentDTO;
import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.repository.SupportCommentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Добавлен SLF4J для логирования
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер для загрузки комментариев/тикетов в системе поддержки.
 * Обрабатывает запросы на получение списка комментариев с учётом ролей пользователей.
 */
@Slf4j // Включаем логгер
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/support")
public class LoadSupportPagination extends BaseController {

    private final SupportCommentRepo supportCommentRepo;

    /**
     * Получает список комментариев (тикетов) для заданной категории с пагинацией.
     * Доступ к данным зависит от роли пользователя:
     * - SUPER видит все тикеты в категории.
     * - ADMIN видит свободные и свои назначенные тикеты.
     * - Обычный пользователь видит только свои тикеты.
     *
     * @param category Категория тикетов.
     * @param page Номер страницы (начиная с 0).
     * @param size Количество элементов на странице.
     * @param authentication Объект аутентификации Spring Security.
     * @return ResponseEntity с картой, содержащей список комментариев и метаинформацию.
     */
    @GetMapping("/comments")
    public ResponseEntity<Map<String, Object>> getSupportComments(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        // Логируем начало обработки запроса (без личных данных)
        log.debug("Запрос на получение комментариев: category={}, page={}, size={}", category, page, size);

        try {
            User currentUser = (User) authentication.getPrincipal();
            // Для отладки используем только роль и косвенные признаки
            log.debug("Обработка запроса от пользователя с ролью: {}", currentUser.getRole());

            boolean isSuperOrAdmin = hasSuperAccess(authentication);
            boolean isSuperOnly = hasOnlySuperAccess(authentication);
            boolean isAdminOnly = hasOnlyAdminAccess(authentication);
            String currentUserNameuse = currentUser.getNameuse(); // Используется только для бизнес-логики, не для логов

            Slice<SupportComment> commentsSlice;

            // --- Выбор стратегии загрузки в зависимости от роли ---
            if (isSuperOrAdmin) {
                if (isSuperOnly) {
                    // SUPER видит ВСЕ тикеты в категории
                    log.debug("Пользователь с ролью SUPER запрашивает все тикеты категории");
                    commentsSlice = supportCommentRepo.findAllRootCommentsForSuperAll(
                            category,
                            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                    );
                } else if (isAdminOnly) {
                    // ADMIN видит свободные + свои взятые тикеты
                    log.debug("Пользователь с ролью ADMIN запрашивает тикеты (свободные и свои)");
                    commentsSlice = supportCommentRepo.findAllRootCommentsForSuper(
                            category,
                            currentUser.getId(),
                            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                    );
                } else {
                    // Случай, когда есть и SUPER, и ADMIN (гипотетически) — падаем в стандартную логику
                    log.warn("Нестандартная комбинация ролей (SUPER+ADMIN), используется стандартная логика");
                    commentsSlice = supportCommentRepo.findUserTicketsIncludingAssigned(
                            category,
                            currentUser.getId(),
                            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                    );
                }
            } else {
                // Обычный пользователь видит ТОЛЬКО СВОИ ТИКЕТЫ
                log.debug("Обычный пользователь запрашивает только свои тикеты");
                commentsSlice = supportCommentRepo.findUserTicketsIncludingAssigned(
                        category,
                        currentUser.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
            }

            // --- Преобразование сущностей в DTO с фильтрацией прав ---
            List<SupportCommentDTO> commentsDTO = commentsSlice.getContent().stream()
                    .map(comment -> {
                        // Логируем только идентификаторы и метрики, без личных данных
                        log.trace("Обработка комментария ID: {}, количество ответов: {}",
                                comment.getId(),
                                (comment.getReplies() != null ? comment.getReplies().size() : 0));
                        return convertToDTO(comment, currentUser, isSuperOnly, isSuperOrAdmin);
                    })
                    .collect(Collectors.toList());

            // --- Формирование ответа ---
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("comments", commentsDTO);
            response.put("hasMore", commentsSlice.hasNext()); // Slice имеет только hasNext()
            response.put("currentPage", page);
            response.put("category", category);
            response.put("isSuperOrAdmin", isSuperOrAdmin);
            response.put("isSuperOnly", isSuperOnly);
            response.put("isAdminOnly", isAdminOnly);
            response.put("currentUserRole", currentUser.getRole());
            response.put("currentUserNameuse", currentUserNameuse);
            response.put("currentUserId", isSuperOnly ? currentUser.getId() : null); // SUPER видит свой ID, остальным null

            log.info("Успешно загружено {} комментариев для категории '{}', есть ли дальше: {}",
                    commentsDTO.size(), category, commentsSlice.hasNext());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Логируем ошибку без личных данных
            log.error("Ошибка при загрузке комментариев для категории '{}'", category, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Конвертирует сущность SupportComment в DTO с учётом прав доступа.
     * Для SUPER отдаётся максимум информации (id, username, email),
     * для остальных ролей — только nameuse и role.
     *
     * @param comment Комментарий для конвертации.
     * @param currentUser Текущий аутентифицированный пользователь.
     * @param isSuperOnly Флаг, указывающий на роль SUPER.
     * @param isSuperOrAdmin Флаг, указывающий на роль SUPER или ADMIN.
     * @return SupportCommentDTO, готовый к отправке клиенту.
     */
    private SupportCommentDTO convertToDTO(SupportComment comment, User currentUser,
                                           boolean isSuperOnly, boolean isSuperOrAdmin) {
        SupportCommentDTO dto = new SupportCommentDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setCategory(comment.getCategory());
        dto.setRead(comment.getIsRead());

        // --- Обработка назначенного супервизора (assignedSuper) ---
        if (comment.getAssignedSuper() != null) {
            SupportCommentDTO.UserDTO assignedSuperDTO = new SupportCommentDTO.UserDTO();

            // ТОЛЬКО для ROLE_SUPER показываем полную информацию о назначенном супере
            if (isSuperOnly) {
                assignedSuperDTO.setId(comment.getAssignedSuper().getId());
                assignedSuperDTO.setUsername(comment.getAssignedSuper().getUsername());
                assignedSuperDTO.setEmail(comment.getAssignedSuper().getEmail());
            } else {
                // Для всех остальных скрываем конфиденциальную информацию
                assignedSuperDTO.setId(null);
                assignedSuperDTO.setUsername(null);
                assignedSuperDTO.setEmail(null);
            }

            // Nameuse и Role показываем всем
            assignedSuperDTO.setNameuse(comment.getAssignedSuper().getNameuse());
            assignedSuperDTO.setRole(comment.getAssignedSuper().getRole());

            // Статус онлайн и время последнего визита — показываем всем (для UX)
            if (comment.getAssignedSuper().getOnline() != null) {
                assignedSuperDTO.setOnline(comment.getAssignedSuper().getOnline());
            }
            if (comment.getAssignedSuper().getLastSeen() != null) {
                assignedSuperDTO.setLastSeen(comment.getAssignedSuper().getLastSeen());
            }

            dto.setAssignedSuper(assignedSuperDTO);
            dto.setAssignedAt(comment.getAssignedAt());
        }

        // --- Обработка автора комментария (user) ---
        User user = comment.getUser();
        SupportCommentDTO.UserDTO userDTO = new SupportCommentDTO.UserDTO();

        // ТОЛЬКО для ROLE_SUPER показываем полную информацию об авторе
        if (isSuperOnly) {
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
        } else {
            // Для всех остальных скрываем конфиденциальную информацию
            userDTO.setId(null);
            userDTO.setUsername(null);
            userDTO.setEmail(null);
        }

        // Nameuse и Role показываем всем
        userDTO.setNameuse(user.getNameuse());
        userDTO.setRole(user.getRole());

        // Статус онлайн и время последнего визита — показываем всем
        if (user.getOnline() != null) {
            userDTO.setOnline(user.getOnline());
        }
        if (user.getLastSeen() != null) {
            userDTO.setLastSeen(user.getLastSeen());
        }

        dto.setUser(userDTO);

        // Флаг: может ли SUPER отвечать на этот комментарий
        if (isSuperOnly) {
            dto.setCanSuperReply(true);
        }

        // Флаг для отображения статуса "ожидайте" (показывается обычному пользователю,
        // если это его собственный тикет и на него ещё не назначен супервизор)
        boolean shouldShowWaitingStatus =
                !isSuperOrAdmin &&
                        user.getId().equals(currentUser.getId()) &&
                        comment.getParentComment() == null &&
                        comment.getAssignedSuper() == null;
        dto.setShouldShowWaitingStatus(shouldShowWaitingStatus);

        // Рекурсивная фильтрация ответов (replies)
        List<SupportCommentDTO> repliesDTO = filterRepliesByPermissions(comment.getReplies(), comment, currentUser, isSuperOnly, isSuperOrAdmin);
        dto.setReplies(repliesDTO);

        return dto;
    }

    /**
     * Фильтрует список ответов (replies) в зависимости от прав пользователя.
     * - SUPER и ADMIN видят все ответы.
     * - Обычный пользователь видит только свои ответы и ответы SUPER/ADMIN в своих тикетах.
     *
     * @param replies Список ответов-сущностей.
     * @param parentComment Родительский комментарий (тикет).
     * @param currentUser Текущий пользователь.
     * @param isSuperOnly Флаг SUPER.
     * @param isSuperOrAdmin Флаг SUPER или ADMIN.
     * @return Отфильтрованный список DTO ответов.
     */
    private List<SupportCommentDTO> filterRepliesByPermissions(List<SupportComment> replies,
                                                               SupportComment parentComment,
                                                               User currentUser,
                                                               boolean isSuperOnly,
                                                               boolean isSuperOrAdmin) {
        if (replies == null || replies.isEmpty()) {
            return new ArrayList<>();
        }

        // Получаем nameuse пользователей для сравнения (безопасно, без id/username)
        String currentUserNameuse = currentUser.getNameuse();
        String parentUserNameuse = parentComment.getUser().getNameuse();

        return replies.stream()
                .filter(reply -> {
                    if (isSuperOrAdmin) {
                        // SUPER и ADMIN видят все ответы
                        return true;
                    } else {
                        // Обычный пользователь видит:
                        // 1. Свои ответы (сравниваем по nameuse)
                        // 2. Ответы SUPER/ADMIN в его диалогах
                        String replyUserNameuse = reply.getUser().getNameuse();
                        boolean isOwnReply = replyUserNameuse != null &&
                                replyUserNameuse.equals(currentUserNameuse);
                        boolean isSuperOrAdminReply = reply.getUser().getRole().equals("ROLE_SUPER") ||
                                reply.getUser().getRole().equals("ROLE_ADMIN");
                        boolean isOwnTicket = parentUserNameuse != null &&
                                parentUserNameuse.equals(currentUserNameuse);

                        return isOwnReply || (isSuperOrAdminReply && isOwnTicket);
                    }
                })
                .map(reply -> {
                    SupportCommentDTO replyDTO = new SupportCommentDTO();
                    replyDTO.setId(reply.getId());
                    replyDTO.setText(reply.getText());
                    replyDTO.setCreatedAt(reply.getCreatedAt());
                    replyDTO.setCategory(reply.getCategory());
                    replyDTO.setRead(reply.getIsRead());

                    SupportCommentDTO.UserDTO replyUserDTO = new SupportCommentDTO.UserDTO();

                    // ТОЛЬКО для ROLE_SUPER показываем полную информацию об авторе ответа
                    if (isSuperOnly) {
                        replyUserDTO.setId(reply.getUser().getId());
                        replyUserDTO.setUsername(reply.getUser().getUsername());
                        replyUserDTO.setEmail(reply.getUser().getEmail());
                    } else {
                        replyUserDTO.setId(null);
                        replyUserDTO.setUsername(null);
                        replyUserDTO.setEmail(null);
                    }

                    // Nameuse и Role показываем всем
                    replyUserDTO.setNameuse(reply.getUser().getNameuse());
                    replyUserDTO.setRole(reply.getUser().getRole());

                    // Статус онлайн и lastSeen
                    if (reply.getUser().getOnline() != null) {
                        replyUserDTO.setOnline(reply.getUser().getOnline());
                    }
                    if (reply.getUser().getLastSeen() != null) {
                        replyUserDTO.setLastSeen(reply.getUser().getLastSeen());
                    }

                    replyDTO.setUser(replyUserDTO);
                    return replyDTO;
                })
                .sorted((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .collect(Collectors.toList());
    }
}