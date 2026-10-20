package jobforsite.sitejob.module.auth.dto;

import lombok.Data;

/**
 * DTO для передачи данных регистрации между формой и контроллером
 *
 * Поля:
 * - username    - логин (только латиница + цифры)
 * - nameuse     - отображаемое имя (русские/английские буквы, цифры, пробелы)
 * - password    - пароль (латиница + цифры, минимум 6 символов)
 * - confirmPassword - подтверждение пароля
 * - email       - почта (только российские домены)
 * - phone       - основной телефон (+7XXXXXXXXXX)
 * - phonePost   - телефон для объявлений (опционально)
 * - region      - регион
 * - city        - город
 * - captcha     - подтверждение "не робот"
 * - license     - согласие с лицензией
 */
@Data
public class RegistrationDto {
    private String username;
    private String nameuse;
    private String password;
    private String confirmPassword;
    private String email;
    private String region;
    private String city;
    private Boolean captcha;
    private Boolean license;
}