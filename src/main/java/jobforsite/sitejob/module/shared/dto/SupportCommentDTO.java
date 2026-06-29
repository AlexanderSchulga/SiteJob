package jobforsite.sitejob.module.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для комментариев в системе поддержки.
 * Поддерживает иерархическую структуру ответов (replies),
 * информацию о назначенном супервайзере и статусах прочтения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportCommentDTO {
    private Long id;                        // Уникальный идентификатор комментария
    private String text;                    // Текст комментария

    @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;        // Дата и время создания комментария

    private UserDTO user;                   // Автор комментария
    private List<SupportCommentDTO> replies = new ArrayList<>();   // Вложенные ответы на комментарий
    private boolean isRead;                 // Отметка о прочтении комментария
    private String category;                // Категория обращения/комментария

    private UserDTO assignedSuper;          // Супервайзер, назначенный на обращение
    private LocalDateTime assignedAt;       // Дата и время назначения супервайзера
    private boolean shouldShowWaitingStatus;    // Флаг отображения статуса ожидания в UI

    /**
     * Возвращает отформатированную дату создания.
     * Используется для отображения в UI.
     *
     * @return строка с датой в формате "dd.MM.yyyy HH:mm" или пустая строка, если дата не задана
     */
    public String getFormattedDate() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return createdAt.format(formatter);
    }

    /**
     * Устанавливает флаг возможности ответа для супервайзера.
     * (Метод-заглушка, используется для совместимости)
     *
     * @param b флаг возможности ответа
     */
    public void setCanSuperReply(boolean b) {
        // Метод-заглушка для обратной совместимости
    }

    /**
     * Внутренний DTO для пользователя в контексте поддержки.
     * Содержит минимальный набор полей, необходимых для отображения информации об авторе/супервайзере.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private Long id;                    // Идентификатор пользователя
        private String username;            // Логин пользователя
        private String nameuse;             // Отображаемое имя пользователя
        private String email;               // Email пользователя
        private String role;                // Роль пользователя (например, "USER", "SUPER", "ADMIN")
        private Boolean online;             // Статус онлайн/оффлайн
        private LocalDateTime lastSeen;     // Дата и время последнего визита
    }

    /**
     * Возвращает назначенного супервайзера.
     *
     * @return DTO супервайзера
     */
    public UserDTO getAssignedSuper() {
        return assignedSuper;
    }

    /**
     * Устанавливает назначенного супервайзера.
     *
     * @param assignedSuper DTO супервайзера
     */
    public void setAssignedSuper(UserDTO assignedSuper) {
        this.assignedSuper = assignedSuper;
    }

    /**
     * Возвращает дату и время назначения супервайзера.
     *
     * @return дата и время назначения
     */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    /**
     * Устанавливает дату и время назначения супервайзера.
     *
     * @param assignedAt дата и время назначения
     */
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    /**
     * Проверяет, нужно ли показывать статус ожидания.
     *
     * @return true, если нужно показывать статус ожидания
     */
    public boolean isShouldShowWaitingStatus() {
        return shouldShowWaitingStatus;
    }

    /**
     * Устанавливает флаг отображения статуса ожидания.
     *
     * @param shouldShowWaitingStatus флаг отображения статуса ожидания
     */
    public void setShouldShowWaitingStatus(boolean shouldShowWaitingStatus) {
        this.shouldShowWaitingStatus = shouldShowWaitingStatus;
    }
}