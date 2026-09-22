# Архитектура

## Сущности

### Объявления
- `PostText`, `PostTextJob`, `Vacancy`, `Resume` — основные типы
  объявлений (товары, услуги, вакансии, резюме)
- Папка `modelExtends` — дочерние сущности для `PostText`

### Комментарии
- `Comment*`, `CommentVacancy` — публичные комментарии
- `CommentPersonal*` — личные комментарии
  (суффикс — категория: без суффикса — товары, `Job` — услуги,
  `Vac` — вакансии, `Res` — резюме)

### Пользователи
- `User` — пользователь системы
- `Rating` — рейтинг
- `Blacklist` — чёрный список

### Общие
- `Favorites` — избранные объявления
- `ImageFoto` — изображения для объявлений
- `LicenseUser` — тексты пользовательских соглашений и политики
- `PostView` — счетчик просмотров объявлений
- `Reviews` — отзывы пользователей
- `SupportComment` — комментарии в системе поддержки


## Контроллеры

### Объявления
- `RefController`, `RefControllerJob`, `RefControllerVac`, `RefControllerRes` —
  управление товарами, услугами, вакансиями, резюме
  (суффикс — категория: без суффикса — товары, `Job` — услуги,
  `Vac` — вакансии, `Res` — резюме)

### Комментарии
- `LoadPaginationCommentPost*` — личные комментарии
- `LoadPaginationPublicCommentPost*` — публичные комментарии

### Авторизация
- `AuthController` — регистрация и авторизация
- `EmailController` — восстановление пароля через email
- `LicenseController` — лицензионное соглашение и политика

### Пользователи
- `UserController` — управление пользователями
- `UpdateUser` — редактирование пользователя админом
- `UpdateUserMe` — редактирование себя
- `UpdateUserPagination` — лента объявлений на странице пользователя
- `UpdateUserPost` — административное управление контентом пользователя
- `RatingController` — рейтинги

### Модерация и редактирование
- `ModerationApiController` — REST API для объявлений на модерации
- `LoadPaginationModeratController` — модерация объявлений
- `LoadPaginationRedactController` — загрузка данных в режиме редактирования
- `LoadPaginatRedactBlockedController` — заблокированные объявления
- `ModerationBlockedController`, `NoBlockedForRadactAndModerController` —
  блокировка и разблокировка
- `DeleteForRedactBlockedAndModerController` — удаление из модерации и блокировок
- `RedactorController` — редакторские функции от пользователя
- `SitemapController` — генерация карты сайта

### Общие (shared)
- `AllPostUserController` — все объявления пользователя
- `DeletePostUserController` — удаление постов пользователем
- `FavoriteController` — избранное
- `LoadPaginationController` — пагинация постов всех типов
- `PublicApiController` — публичное REST API
- `ReviewsController` — отзывы
- `TogglePostController` — блокировка и удаление с правами админа

### Базовые и глобальные
- `BaseController`, `BasePostController` — базовые абстрактные
- `HomeController` — главная страница
- `ArchiveController` — архивация и восстановление
- `NotificationController` — нотификации
- `CustomErrorController` — обработка ошибок HTTP

### Поддержка и сообщения
- `SupportController`, `SupportRedirectController`, `SupportSseController` —
  служба поддержки
- `LoadSupportPagination` — пагинация обращений
- `MessageTicketController`, `MessageRedirectController` — сообщения и тикеты

## Сервисы
#### Авторизация и верификация
- `EmailService` - почтовый сервис
- `SmsRuService` - сервис смс и звонки (не применяется)
- `VerificationCodeService` - верификация
#### Товары
- `extendsFilter` - папка с фильтрами подкатегорий
- `loadmore` - папка с сервисами загрузки постов подкатегорий
- `DefaultFilter` - дефолтный фильтр для постов
- `ExtendsProductService` - фильтрация и получение подкатегорий
- `ExtendsUpdateProductService` - обновление подкатегорий
- `FilterFactory` - фабрика фильтров подкатегорий
- `LoadMoreFilterFactory` - фабрика для получения сервисов загрузки постов
- `FilterRangeService` - маппинг значений подкатегорий в диапазоны
- `PostFactory` - фабрика для создания постов различных категорий продуктов
- `PostTextListService` - преобразование списков постов в DTO для отображения
- `PostTypeResolver` - определяет конкретный тип поста по его hashId
- `ProductService` - управление товарами

#### Пользовательские
- `OnlineStatusCleanupService` - очистка зависших онлайн статусов
- `SessionManagementService` - управление ссесиями пользователей
- `UserActivityService` - отслеживание активности пользователя
- `UserBlockReasonCacheService` - кеширование блокировки пользователя
- `UserCacheService` - кеширование данных пользователя
- `UserDetailsServiceImpl` - загрузка пользователя для Spring Security
- `UserService` - работа с пользователями и их рейтингами
- `UserStatisticsService` - статистика пользователя и его постов
- `UserUnblockService` - авто разблокировка пользователя с его постами

#### Глобальные
- `BlockedPostsCacheService`, `CacheCleanupService`, `ExtendsPostCacheService`,
`ModerationPostsCacheService`, `PostCacheService` - работа с кешем
- `EnhancedRateLimitService`, `EnhancedReviewsRateLimitService`, 
`RateLimitService` - rateLimit
- `BlockedPostsNotificationService` - проверки наличия заблокированных постов
- `CityService` - для работы с городами и регионами
- `FileStorageService` - управление файлами на диске
- `ImageService` - управление изображениями
- `PostArchiveService` - архивация и очистка старых
- `PostCleanupService` - автоочистка заблокированных
- `PostViewService` - для счетчика просмотров
- `SupportSseService` — SSE-уведомления в реальном времени:
  управление соединениями, статус онлайн, отправка событий
  (например, новый ответ в тикете поддержки)
- `FilterConfigService` - загрузка и предоставление конфигурации фильтров для категорий продуктов

## Репозитории
- `LicenseUserRepo` - лицензионные соглашения
- `repositoryExtendsPostText` - папка репозиторий дочерних сущностей товаров
- `CommentRepoPersonal`, `CommentRepoJobPersonal`,
`CommentRepoVacPersonal`, `CommentRepoResPersonal` — персональные комментарии
- `CommentRepository`, `CommentRepositoryJob`, `PostVacancyRepository`,
`PostResumeRepository` - обычные комментарии
- `PostRepo`, `PostRepoExtends`, `PostTextRepository` - основные репозитории товара
- `PostRepoJob`, `PostVacancyRepository`, `PostResumeRepository` - услуги, вакансии, резюме
- `BlacklistRepository`, `RatingRepository`, `UserRepository`, `UserDetailsServiceAuth` - работа с пользователями
- `FavorRepo` - избранное
- `ImageRepo` - изображения
- `PostViewRepository` - просмотры
- `ReviewsRepo` - отзывы
- `SupportCommentRepo` - поддержка



## Конфиги
- `EmailConfig` - почта
- `BaseCacheConfig`, `CacheConfig`, `CacheConfigModeration`, `CacheConfigReviews`, 
`LogoutCacheCleanupListener` - кеш
- `categoryconfig` - папка категорий
- `MvcConfig` - обработка статических ресурсов и страниц ошибок
- `RegionsConfig` - географические данные
- `SecurityConfig` - безопасность приложений
- `SessionEventListener` - сессии

## Фронтенд
Шаблоны на Thymeleaf, стили и скрипты — в `src/main/resources/`.
Структура: `templates/` — HTML, `static/` — CSS, JS, изображения, `geo/` - json.