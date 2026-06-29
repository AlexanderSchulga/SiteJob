package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.repository.ImageRepo;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления изображениями, связанными с постами, услугами, вакансиями и резюме.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Сохранение новых изображений</li>
 *   <li>Отложенное удаление (помечение на удаление)</li>
 *   <li>Физическое удаление отмеченных файлов и записей из БД</li>
 *   <li>Полное удаление всех изображений сущности</li>
 * </ul>
 *
 * <p>Используется мягкое удаление через флаг {@code markedForDeletion},
 * чтобы избежать потери данных при отмене действий пользователя.
 */
@RequiredArgsConstructor
@Service
public class ImageService {


    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final ImageRepo imageRepository;


    /**
     * Проверяет общий размер всех файлов и размер каждого файла отдельно.
     *
     * @param files массив файлов для проверки
     * @param maxTotalSizeMB максимальный общий размер в MB
     * @param maxFileSizeMB максимальный размер одного файла в MB
     * @throws IllegalArgumentException если превышен лимит
     */
    public void validateFilesSize(MultipartFile[] files, int maxTotalSizeMB, int maxFileSizeMB) {
        if (files == null || files.length == 0) {
            return;
        }

        long maxTotalBytes = maxTotalSizeMB * 1024L * 1024L;
        long maxFileBytes = maxFileSizeMB * 1024L * 1024L;
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            long fileSize = file.getSize();

            // Проверка каждого файла отдельно
            if (fileSize > maxFileBytes) {
                throw new IllegalArgumentException(
                        String.format("Файл \"%s\" слишком большой: %.2f MB. Максимум: %d MB",
                                file.getOriginalFilename(),
                                fileSize / (1024.0 * 1024.0),
                                maxFileSizeMB
                        )
                );
            }

            totalSize += fileSize;
        }

        // Проверка общего размера
        if (totalSize > maxTotalBytes) {
            throw new IllegalArgumentException(
                    String.format("Общий размер файлов слишком большой: %.2f MB. Максимум: %d MB",
                            totalSize / (1024.0 * 1024.0),
                            maxTotalSizeMB
                    )
            );
        }
    }

    /**
     * Сохраняет изображения для переданной сущности (пост, услуга, вакансия или резюме).
     * <p>
     * Файлы сохраняются в директории {@code ./uploads} с уникальным именем.
     * Для каждой сущности устанавливается соответствующая связь.
     * Новые изображения всегда помечаются как не подлежащие удалению.
     *
     * @param files   массив загруженных файлов
     * @param post    пост (может быть null)
     * @param postJob услуга (может быть null)
     * @param vacancy вакансия (может быть null)
     * @param resume  резюме (может быть null)
     * @throws IOException если не удаётся создать директорию или сохранить файл
     */
    public void saveImages(MultipartFile[] files, PostText post, PostTextJob postJob, Vacancy vacancy, Resume resume) throws IOException {
        if (files == null || files.length == 0) {
            return;
        }

        // ========== СЕРВЕРНАЯ ВАЛИДАЦИЯ РАЗМЕРА ФАЙЛОВ ==========
        // Проверяем размер ДО сохранения на диск
        validateFilesSize(files, 20, 20);
        // ========================================================

        File projectDir = new File("").getAbsoluteFile();
        File uploadDir = new File(projectDir, "uploads");

        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("Не удалось создать директорию: " + uploadDir.getAbsolutePath());
            }
        }

        log.debug("Директория загрузки: {}", uploadDir.getAbsolutePath());

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = generateFileName(file.getOriginalFilename());
                File dest = new File(uploadDir, fileName);
                file.transferTo(dest);

                String webPath = "/uploads/" + fileName;
                ImagesFoto image = new ImagesFoto(webPath);

                if (post != null) {
                    image.setPost(post);
                } else if (postJob != null) {
                    image.setPostjob(postJob);
                } else if (vacancy != null) {
                    image.setVacancy(vacancy);
                } else if (resume != null) {
                    image.setResume(resume);
                }

                image.setMarkedForDeletion(false);
                imageRepository.save(image);
                log.info("Сохранено новое изображение: {}", webPath);
            }
        }
    }

    /**
     * Отмечает изображения на удаление (мягкое удаление).
     * <p>
     * Изображения не удаляются сразу, а только получают флаг {@code markedForDeletion = true}.
     * Это позволяет отменить действие до финального удаления.
     *
     * @param imageIds список ID изображений для пометки
     */
    @Transactional
    public void markImagesForDeletion(List<Long> imageIds) {
        if (imageIds != null && !imageIds.isEmpty()) {
            Iterable<ImagesFoto> images = imageRepository.findAllById(imageIds);
            for (ImagesFoto image : images) {
                image.setMarkedForDeletion(true);
                imageRepository.save(image);
            }
            log.info("Отмечено на удаление изображений: {}", imageIds.size());
        }
    }

    /**
     * Снимает отметку на удаление с изображений.
     * <p>
     * Используется, если пользователь передумал удалять изображения до финального сохранения.
     *
     * @param imageIds список ID изображений, с которых нужно снять пометку
     */
    @Transactional
    public void unmarkImagesForDeletion(List<Long> imageIds) {
        if (imageIds != null && !imageIds.isEmpty()) {
            Iterable<ImagesFoto> images = imageRepository.findAllById(imageIds);
            for (ImagesFoto image : images) {
                image.setMarkedForDeletion(false);
                imageRepository.save(image);
            }
            log.info("Снята отметка на удаление с изображений: {}", imageIds.size());
        }
    }

    /**
     * Удаляет все изображения, отмеченные на удаление (физически и из БД).
     * <p>
     * Обычно вызывается при финальной публикации или подтверждении действия на странице модерации.
     */
    @Transactional
    public void deleteMarkedImages() {
        List<ImagesFoto> markedImages = imageRepository.findByMarkedForDeletionTrue();

        for (ImagesFoto image : markedImages) {
            deletePhysicalFile(image.getImagePath());
        }

        imageRepository.deleteAllMarkedForDeletion();
        log.info("Удалено отмеченных изображений: {}", markedImages.size());
    }

    /**
     * Удаляет отмеченные изображения, связанные с конкретным постом.
     *
     * @param post пост, для которого удаляются помеченные изображения
     */
    @Transactional
    public void deleteMarkedImagesForPost(PostText post) {
        if (post == null) return;

        List<ImagesFoto> markedImages = imageRepository.findMarkedForDeletionByPost(post);
        for (ImagesFoto image : markedImages) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteMarkedForDeletionByPost(post);
        log.info("Удалены отмеченные изображения для поста ID: {}", post.getId());
    }

    /**
     * Удаляет отмеченные изображения, связанные с конкретной услугой.
     *
     * @param postJob услуга, для которой удаляются помеченные изображения
     */
    @Transactional
    public void deleteMarkedImagesForPostJob(PostTextJob postJob) {
        if (postJob == null) return;

        List<ImagesFoto> markedImages = imageRepository.findMarkedForDeletionByPostjob(postJob);
        for (ImagesFoto image : markedImages) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteMarkedForDeletionByPostjob(postJob);
        log.info("Удалены отмеченные изображения для услуги ID: {}", postJob.getId());
    }

    /**
     * Удаляет отмеченные изображения, связанные с конкретной вакансией.
     *
     * @param vacancy вакансия, для которой удаляются помеченные изображения
     */
    @Transactional
    public void deleteMarkedImagesForVacancy(Vacancy vacancy) {
        if (vacancy == null) return;

        List<ImagesFoto> markedImages = imageRepository.findMarkedForDeletionByVacancy(vacancy);
        for (ImagesFoto image : markedImages) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteMarkedForDeletionByVacancy(vacancy);
        log.info("Удалены отмеченные изображения для вакансии ID: {}", vacancy.getId());
    }

    /**
     * Удаляет отмеченные изображения, связанные с конкретным резюме.
     *
     * @param resume резюме, для которого удаляются помеченные изображения
     */
    @Transactional
    public void deleteMarkedImagesForResume(Resume resume) {
        if (resume == null) return;

        List<ImagesFoto> markedImages = imageRepository.findMarkedForDeletionByResume(resume);
        for (ImagesFoto image : markedImages) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteMarkedForDeletionByResume(resume);
        log.info("Удалены отмеченные изображения для резюме ID: {}", resume.getId());
    }

    /**
     * Устаревший метод для совместимости. Вместо немедленного удаления отмечает изображения на удаление.
     *
     * @param imageIds список ID изображений
     * @deprecated используйте {@link #markImagesForDeletion(List)}
     */
    @Deprecated
    @Transactional
    public void deleteImagesByIds(List<Long> imageIds) {
        markImagesForDeletion(imageIds);
    }

    /**
     * Полностью удаляет все изображения поста (физически и из БД) при удалении самого поста.
     *
     * @param post пост, изображения которого нужно удалить
     */
    @Transactional
    public void deletePostImages(PostText post) {
        if (post == null) return;

        List<ImagesFoto> images = imageRepository.findByPost(post);
        for (ImagesFoto image : images) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteByPost(post);
        log.info("Полностью удалены все изображения для поста ID: {}", post.getId());
    }

    /**
     * Полностью удаляет все изображения услуги при удалении самой услуги.
     *
     * @param postJob услуга, изображения которой нужно удалить
     */
    @Transactional
    public void deletePostJobImages(PostTextJob postJob) {
        if (postJob == null) return;

        List<ImagesFoto> images = imageRepository.findByPostjob(postJob);
        for (ImagesFoto image : images) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteByPostjob(postJob);
        log.info("Полностью удалены все изображения для услуги ID: {}", postJob.getId());
    }

    /**
     * Полностью удаляет все изображения вакансии при удалении самой вакансии.
     *
     * @param vacancy вакансия, изображения которой нужно удалить
     */
    @Transactional
    public void deleteVacancyImages(Vacancy vacancy) {
        if (vacancy == null) return;

        List<ImagesFoto> images = imageRepository.findByVacancy(vacancy);
        for (ImagesFoto image : images) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteByVacancy(vacancy);
        log.info("Полностью удалены все изображения для вакансии ID: {}", vacancy.getId());
    }

    /**
     * Полностью удаляет все изображения резюме при удалении самого резюме.
     *
     * @param resume резюме, изображения которого нужно удалить
     */
    @Transactional
    public void deleteResumeImages(Resume resume) {
        if (resume == null) return;

        List<ImagesFoto> images = imageRepository.findByResume(resume);
        for (ImagesFoto image : images) {
            deletePhysicalFile(image.getImagePath());
        }
        imageRepository.deleteByResume(resume);
        log.info("Полностью удалены все изображения для резюме ID: {}", resume.getId());
    }

    /**
     * Полностью удаляет все изображения, связанные с пользователем (из всех постов, услуг, вакансий и резюме).
     *
     * @param user пользователь, все изображения которого нужно удалить
     */
    @Transactional
    public void deleteAllUserImages(User user) {
        if (user == null) return;

        List<PostText> userPosts = postRepo.findByUser(user);
        for (PostText post : userPosts) {
            deletePostImages(post);
        }

        List<PostTextJob> userJobs = postRepoJob.findByUser(user);
        for (PostTextJob job : userJobs) {
            deletePostJobImages(job);
        }

        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        for (Vacancy vacancy : userVacancies) {
            deleteVacancyImages(vacancy);
        }

        List<Resume> userResume = postResumeRepository.findByUser(user);
        for (Resume resume : userResume) {
            deleteResumeImages(resume);
        }
        log.info("Полностью удалены все изображения для пользователя ID: {}", user.getId());
    }

    /**
     * Удаляет физический файл с диска.
     *
     * @param imagePath путь к изображению (например, "/uploads/xxx.jpg")
     */
    private void deletePhysicalFile(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return;
            }

            String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            File projectDir = new File("").getAbsoluteFile();
            File uploadDir = new File(projectDir, "uploads");
            File file = new File(uploadDir, fileName);

            if (file.exists()) {
                if (!file.delete()) {
                    log.warn("Не удалось удалить файл: {}", file.getAbsolutePath());
                } else {
                    log.debug("Файл успешно удалён: {}", file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении файла {}: {}", imagePath, e.getClass().getSimpleName(), e);
        }
    }

    /**
     * Генерирует уникальное имя файла с сохранением расширения.
     *
     * @param originalFileName исходное имя файла
     * @return уникальное имя файла
     */
    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * Возвращает расширение файла из его имени.
     *
     * @param fileName имя файла
     * @return расширение (по умолчанию "jpg")
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "jpg";
    }

    /**
     * Возвращает количество изображений для поста.
     *
     * @param post пост
     * @return количество изображений
     */
    public int getImageCountForPost(PostText post) {
        return imageRepository.countByPost(post);
    }

    /**
     * Возвращает количество изображений для услуги.
     *
     * @param postJob услуга
     * @return количество изображений
     */
    public int getImageCountForPostJob(PostTextJob postJob) {
        return imageRepository.countByPostjob(postJob);
    }

    /**
     * Возвращает количество изображений для вакансии.
     *
     * @param vacancy вакансия
     * @return количество изображений
     */
    public int getImageCountForVacancy(Vacancy vacancy) {
        return imageRepository.countByVacancy(vacancy);
    }

    /**
     * Возвращает количество изображений для резюме.
     *
     * @param resume резюме
     * @return количество изображений
     */
    public int getImageCountForResume(Resume resume) {
        return imageRepository.countByResume(resume);
    }

    /**
     * Возвращает количество изображений, отмеченных на удаление, для поста.
     *
     * @param post пост
     * @return количество отмеченных изображений
     */
    public int getMarkedImagesCountForPost(PostText post) {
        return imageRepository.findMarkedForDeletionByPost(post).size();
    }
}