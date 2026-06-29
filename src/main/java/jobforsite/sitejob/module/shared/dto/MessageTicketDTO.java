package jobforsite.sitejob.module.shared.dto;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для тикетов сообщений.
 * Используется для передачи информации о диалогах/сообщениях,
 * связанных с объявлениями, вакансиями и другими постами.
 */
public class MessageTicketDTO {

    // ================ ИНФОРМАЦИЯ О ПОСТЕ (ОБЪЯВЛЕНИИ) ================

    /**
     * Идентификатор поста (объявления, вакансии, резюме и т.д.)
     */
    private Long postId;

    /**
     * Хэш-идентификатор поста для публичных ссылок
     */
    private String postHashId;

    /**
     * Заголовок поста
     */
    private String postTitle;

    /**
     * Тип поста: например "product", "service", "vacancy", "resume"
     */
    private String postType;

    // ================ ИНФОРМАЦИЯ О СООБЩЕНИИ ================

    /**
     * Имя пользователя, отправившего сообщение
     */
    private String fromUserName;

    /**
     * Время отправки сообщения
     */
    private LocalDateTime messageTime;

    /**
     * Превью сообщения (первая часть текста)
     */
    private String messagePreview;

    /**
     * Флаг прочтения сообщения получателем
     */
    private boolean read;

    /**
     * Идентификатор сообщения
     */
    private Long messageId;

    // ================ КОНСТРУКТОРЫ ================

    /**
     * Конструктор по умолчанию
     */
    public MessageTicketDTO() {
    }

    /**
     * Конструктор с основными полями (без messageId)
     *
     * @param postId        идентификатор поста
     * @param postHashId    хэш-идентификатор поста
     * @param postTitle     заголовок поста
     * @param postType      тип поста
     * @param fromUserName  имя отправителя
     * @param messageTime   время сообщения
     * @param messagePreview превью сообщения
     * @param read          прочитано ли сообщение
     */
    public MessageTicketDTO(Long postId, String postHashId, String postTitle,
                            String postType, String fromUserName,
                            LocalDateTime messageTime, String messagePreview,
                            boolean read) {
        this.postId = postId;
        this.postHashId = postHashId;
        this.postTitle = postTitle;
        this.postType = postType;
        this.fromUserName = fromUserName;
        this.messageTime = messageTime;
        this.messagePreview = messagePreview;
        this.read = read;
    }

    // ================ ГЕТТЕРЫ И СЕТТЕРЫ ================

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getPostHashId() {
        return postHashId;
    }

    public void setPostHashId(String postHashId) {
        this.postHashId = postHashId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public LocalDateTime getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(LocalDateTime messageTime) {
        this.messageTime = messageTime;
    }

    public String getMessagePreview() {
        return messagePreview;
    }

    public void setMessagePreview(String messagePreview) {
        this.messagePreview = messagePreview;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
}