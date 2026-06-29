package jobforsite.sitejob.module.auth.repository;

import jobforsite.sitejob.module.shared.model.LicenseUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с сущностью LicenseUser (лицензионные соглашения / тексты)
 *
 * Используется в LicenseController для получения текста лицензии и политики конфиденциальности
 */
public interface LicenseUserRepo extends JpaRepository<LicenseUser, Long> {

}