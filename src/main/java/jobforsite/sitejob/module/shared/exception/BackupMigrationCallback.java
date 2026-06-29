package jobforsite.sitejob.module.shared.exception;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Flyway-колбэк для восстановления бэкапа после миграции.
 *
 * <p>Основное назначение: выполняет SQL-файл бэкапа после того, как Flyway
 * применит все миграции. Путь к бэкапу настраивается через application.properties.
 *
 * <p>⚠️ В данный момент отключен: {@code @Component} закомментирован, чтобы
 * предотвратить автоматическое выполнение в продакшен-миграциях.
 *
 * <p>📌 Смотри: ключ {@code backup.file.path} в application.properties
 *
 * @author YourTeam
 * @see org.flywaydb.core.api.callback.Callback
 */
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "backup.restore.enabled", havingValue = "true")
public class BackupMigrationCallback implements Callback {

    private static final Logger log = LoggerFactory.getLogger(BackupMigrationCallback.class);

    /**
     * Путь к SQL-файлу бэкапа.
     * По умолчанию: E:/migrat/sitejob.sql (для локальной разработки на Windows)
     * В продакшене: переопределить через {@code backup.file.path} во внешнем конфиге
     */
    @Value("${backup.file.path:E:/migrat/sitejob.sql}")
    private String backupFilePath;

    private final DataSource dataSource;

    /**
     * Определяет, какие события Flyway поддерживаются этим колбэком.
     *
     * <p>Сейчас поддерживается только {@code AFTER_MIGRATE} — бэкап восстанавливается
     * после завершения основной миграции.
     *
     * @param event   событие жизненного цикла Flyway
     * @param context контекст выполнения (не используется)
     * @return true если событие AFTER_MIGRATE, иначе false
     */
    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    /**
     * Указывает, может ли колбэк выполняться внутри транзакции.
     *
     * <p>Возврат {@code false} гарантирует, что восстановление бэкапа выполняется
     * в своей собственной отдельной транзакции, снижая риск проблем с блокировками
     * или откатов, влияющих на основную миграцию.
     *
     * @param event   событие жизненного цикла Flyway
     * @param context контекст выполнения (не используется)
     * @return false (выполняется вне основной транзакции)
     */
    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    /**
     * Выполняет логику восстановления бэкапа.
     *
     * <p>Алгоритм работы:
     * <ol>
     *   <li>Проверяет, существует ли файл бэкапа по пути {@code backupFilePath}</li>
     *   <li>Читает всё содержимое SQL-файла</li>
     *   <li>Разделяет команды по точке с запятой ({@code ;})</li>
     *   <li>Выполняет каждую непустую команду через JDBC Statement</li>
     * </ol>
     *
     * <p>⚠️ Важно:
     * <ul>
     *   <li>Данные пользователя (id, username) не логируются</li>
     *   <li>Если любая SQL-команда завершится ошибкой, весь процесс остановится с записью в лог</li>
     *   <li>Файл бэкапа должен быть тщательно подготовлен (валидный SQL, безопасный порядок операций)</li>
     * </ul>
     *
     * @param event   событие Flyway (ожидается AFTER_MIGRATE)
     * @param context контекст выполнения (не используется)
     */
    @Override
    public void handle(Event event, Context context) {
        try {
            if (Files.exists(Paths.get(backupFilePath))) {
                log.info("Файл бэкапа найден, начинаем восстановление: {}", backupFilePath);
                String sqlContent = new String(Files.readAllBytes(Paths.get(backupFilePath)));

                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {

                    String[] sqlCommands = sqlContent.split(";");
                    int executedCount = 0;
                    for (String sql : sqlCommands) {
                        if (!sql.trim().isEmpty()) {
                            stmt.execute(sql);
                            executedCount++;
                        }
                    }
                    log.info("Восстановление бэкапа успешно завершено. Выполнено команд: {}", executedCount);
                }
            } else {
                log.warn("Файл бэкапа не найден, пропускаем восстановление: {}", backupFilePath);
            }
        } catch (Exception e) {
            // Ошибка логируется без раскрытия данных пользователя
            log.error("Ошибка при восстановлении бэкапа из файла: {}. Сообщение: {}", backupFilePath, e.getClass().getSimpleName(), e);
        }
    }

    /**
     * Возвращает имя колбэка для логирования и диагностики.
     *
     * @return строка-константа, идентифицирующая этот колбэк
     */
    @Override
    public String getCallbackName() {
        return "BackupMigrationCallback";
    }
}