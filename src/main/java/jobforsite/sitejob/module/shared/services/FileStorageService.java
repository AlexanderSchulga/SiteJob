package jobforsite.sitejob.module.shared.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Сервис для управления файлами на диске.
 *
 * <p>Обеспечивает:
 * <ul>
 *   <li>Удаление отдельных файлов по пути</li>
 *   <li>Массовое удаление нескольких файлов</li>
 *   <li>Удаление всех файлов, принадлежащих пользователю</li>
 * </ul>
 *
 * <p>Файлы хранятся в директории, указанной в конфигурации {@code upload.path}.
 * При удалении файлов пользователя используется поиск по подстроке в имени файла.
 *
 * @author Team
 * @version 1.0
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadPath; // Базовая директория для хранения загруженных файлов

    /**
     * Удаляет файл по указанному пути.
     *
     * <p>Метод извлекает имя файла из полного пути (поддерживает пути с "/" или "\"),
     * затем формирует абсолютный путь в директории {@code uploadPath} и пытается удалить файл.
     *
     * @param filePath полный путь к файлу (может содержать имя файла или путь с директориями)
     * @return {@code true} если файл успешно удален, {@code false} если файл не существует
     *         или произошла ошибка
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.debug("deleteFile called with null or empty path, skipping");
            return false;
        }

        try {
            // Извлекаем имя файла из полного пути
            String fileName = extractFileName(filePath);
            Path path = Paths.get(uploadPath, fileName);

            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("File successfully deleted: {}", fileName);
            } else {
                log.warn("File not found: {}", fileName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Удаляет несколько файлов.
     *
     * <p>Последовательно вызывает {@link #deleteFile(String)} для каждого пути.
     * Ошибки при удалении отдельных файлов не прерывают процесс.
     *
     * @param filePaths список полных путей к файлам для удаления
     */
    public void deleteFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            log.debug("deleteFiles called with null or empty list, skipping");
            return;
        }

        log.debug("Attempting to delete {} files", filePaths.size());
        for (String filePath : filePaths) {
            deleteFile(filePath);
        }
    }

    /**
     * Удаляет все файлы пользователя на основе идентификатора в имени файла.
     *
     * <p>Метод обходит всю директорию загрузок и удаляет файлы,
     * в имени которых содержится указанный идентификатор пользователя.
     *
     * <p><b>Внимание:</b> Этот подход эффективен только при небольшом количестве файлов.
     * Для больших объемов рекомендуется хранить маппинг файлов в базе данных.
     *
     * @param userIdentifier технический идентификатор пользователя (может быть ID,
     *                       но не username или email)
     */
    public void deleteAllUserFiles(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isEmpty()) {
            log.debug("deleteAllUserFiles called with null or empty identifier, skipping");
            return;
        }

        log.debug("Deleting all files for user identifier: {}", userIdentifier);

        try {
            Path userDir = Paths.get(uploadPath);
            if (!Files.exists(userDir)) {
                log.debug("Upload directory does not exist: {}", uploadPath);
                return;
            }

            final int[] deletedCount = {0};

            // Проходим по всем файлам в директории загрузок
            Files.walkFileTree(userDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    // Проверяем, принадлежит ли файл пользователю
                    // Предполагаем, что в имени файла есть идентификатор пользователя
                    if (fileName.contains(userIdentifier)) {
                        try {
                            Files.deleteIfExists(file);
                            deletedCount[0]++;
                            log.info("Deleted user file: {}", fileName);
                        } catch (IOException e) {
                            log.error("Error deleting user file: {}", fileName, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (deletedCount[0] > 0) {
                log.info("Deleted {} files for user identifier {}", deletedCount[0], userIdentifier);
            } else {
                log.debug("No files found for user identifier {}", userIdentifier);
            }
        } catch (IOException e) {
            log.error("Error while deleting user files for identifier {}", userIdentifier, e);
        }
    }

    /**
     * Извлекает имя файла из полного пути.
     *
     * <p>Поддерживает пути с прямыми слешами ("/") и обратными слешами ("\\").
     * Если разделители не найдены, возвращает исходную строку.
     *
     * @param filePath полный путь к файлу
     * @return имя файла (последняя часть пути)
     */
    private String extractFileName(String filePath) {
        // Если путь содержит слеши, берем последнюю часть
        if (filePath.contains("/")) {
            return filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        if (filePath.contains("\\")) {
            return filePath.substring(filePath.lastIndexOf("\\") + 1);
        }
        return filePath;
    }
}