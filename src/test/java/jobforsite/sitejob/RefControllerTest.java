package jobforsite.sitejob;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.RealEstatePostText;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties", properties = {
        "spring.security.csrf.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RefControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PostRepo postRepo;

    @Autowired
    private PostVacancyRepository postVacancyRepository;

    private byte[] testImageBytes;

    // ID созданного поста для архивации
    //private static Long postIdForArchive;


    @BeforeEach
    void setUp() throws IOException {
        // Загружаем тестовое изображение перед каждым тестом
        Path imagePath = Paths.get("src/test/M33.jpg");
        if (Files.exists(imagePath)) {
            testImageBytes = Files.readAllBytes(imagePath);
        } else {
            // Если файла нет, создаем фейковые байты для теста
            testImageBytes = new byte[]{1, 2, 3, 4, 5};
            System.out.println("Создан фейковый массив байтов для изображения");
        }
    }


    @RepeatedTest(10)
    void testCreateVacancyForArchive() {
        // Генерируем уникальные данные для каждого запуска
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTitle = "zaeblo_" + uniqueSuffix;

        System.out.println("=== Запуск теста " + uniqueSuffix + ": создание поста для архивации ===");

        // Создание SessionFilter для управления сессией
        io.restassured.filter.session.SessionFilter sessionFilter = new io.restassured.filter.session.SessionFilter();

        System.out.println("1. Получение CSRF токена...");
        // Получение CSRF токена
        String csrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        System.out.println("2. Аутентификация...");
        // Аутентификация с CSRF токеном
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "test")
                .formParam("password", "test")
                .formParam("_csrf", csrfToken)
                .when()
                .post("/login")
                .then()
                .statusCode(302);

        System.out.println("3. Получение нового CSRF токена после аутентификации...");
        // Получение нового CSRF токена после аутентификации
        String newCsrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/page_two/viewstwovac")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        System.out.println("4. Создание поста специально для архивации...");
        // Создание поста специально для архивации
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", newCsrfToken)
                .header("X-XSRF-TOKEN", newCsrfToken)
                .multiPart("title", uniqueTitle)
                .multiPart("cash", "999")
                .multiPart("full_text", "This post will be archived in the next test - " + uniqueSuffix)
                .multiPart("category", "electronics")
                .multiPart("location", "Archive City")
                .multiPart("experience", "1-3")
                .multiPart("workformat", "remote")
                .multiPart("moderation", "true")
                .multiPart("files", "test_image.jpg", testImageBytes, "image/jpeg")
                .contentType("multipart/form-data")
                .when()
                .post("/page_two/viewstwovac")
                .then()
                .statusCode(302);

        System.out.println("5. Проверка сохранения в БД...");
        // Проверка сохранения в БД
        List<Vacancy> posts = postVacancyRepository.findByTitleContainingIgnoreCase(uniqueTitle);
        assertFalse(posts.isEmpty(), "Пост для архивации должен быть сохранен в базу данных");

        Vacancy savedPost = posts.get(0);
        Long postIdForArchive = savedPost.getId();

        // Проверяем данные поста
        assertEquals(uniqueTitle, savedPost.getTitle());
        //assertEquals("999", savedPost.getCash());
        assertEquals("This post will be archived in the next test - " + uniqueSuffix, savedPost.getFull_text());
        assertEquals("electronics", savedPost.getCategory());
        assertEquals("Archive City", savedPost.getLocation());
        assertTrue(savedPost.isModeration(), "Модерация должна быть true");
        assertNotNull(savedPost.getHashId(), "HashId не должен быть null");
        assertFalse(savedPost.isArchived(), "Пост не должен быть архивирован изначально");
        assertNull(savedPost.getArchivedAt(), "ArchivedAt должен быть null");

        System.out.println("Создан пост для архивации с ID: " + postIdForArchive + " и названием: " + uniqueTitle);

        // Очистка после теста (опционально)
        // postVacancyRepository.deleteById(postIdForArchive);
    }


    @Test
    void testManualArchiveByTitleViaService() {
        // Проверяем, что пост существует перед архивацией
        List<Vacancy> postsBeforeArchive = postVacancyRepository.findByTitleContainingIgnoreCase("zaeblo");
        assertFalse(postsBeforeArchive.isEmpty(), "Пост должен существовать перед архивацией");
        Vacancy postToArchive = postsBeforeArchive.get(0);

        // Выполняем ручную архивацию через прямое обновление в БД
        LocalDateTime now = LocalDateTime.now();
        postToArchive.setArchived(true);
        postToArchive.setArchivedAt(now);
        postVacancyRepository.save(postToArchive);

        // Проверяем результат
        Vacancy archivedPost = postVacancyRepository.findById(postToArchive.getId()).orElseThrow();
        assertTrue(archivedPost.isArchived(), "Пост должен быть помечен как archived");
        assertNotNull(archivedPost.getArchivedAt(), "ArchivedAt должен быть установлен");

        System.out.println("Пост успешно заархивирован вручную: ID=" + archivedPost.getId());
    }


    @RepeatedTest(10)
    void testCreateProductForArchive() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTitle = "zaeblo_product_" + uniqueSuffix;

        System.out.println("=== Запуск теста " + uniqueSuffix + ": создание товара для архивации ===");

        io.restassured.filter.session.SessionFilter sessionFilter = new io.restassured.filter.session.SessionFilter();

        // 1. CSRF
        String csrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        // 2. Логин
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "test")
                .formParam("password", "test")
                .formParam("_csrf", csrfToken)
                .when()
                .post("/login")
                .then()
                .statusCode(302);

        // 3. Новый CSRF
        String newCsrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/page_two/viewstwo")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        // 4. Создание товара
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", newCsrfToken)
                .header("X-XSRF-TOKEN", newCsrfToken)
                .multiPart("title", uniqueTitle)
                .multiPart("price", "999")
                .multiPart("fullText", "This product will be archived - " + uniqueSuffix)
                .multiPart("category", "fleamarket")
                .multiPart("location", "Archive City")
                .multiPart("moderation", "true")
                .multiPart("files", "test_image.jpg", testImageBytes, "image/jpeg")
                .contentType("multipart/form-data")
                .when()
                .post("/page_two/viewstwo")
                .then()
                .statusCode(302);

        // 5. Проверка в БД
        List<PostText> posts = postRepo.findByTitleContainingIgnoreCase(uniqueTitle);
        assertFalse(posts.isEmpty(), "Товар должен быть сохранён в БД");

        PostText savedPost = posts.get(0);

        assertEquals(uniqueTitle, savedPost.getTitle());
        assertEquals(999, savedPost.getPrice());
        assertEquals("This product will be archived - " + uniqueSuffix, savedPost.getFull_text());
        assertEquals("fleamarket", savedPost.getCategory());
        assertEquals("Archive City", savedPost.getLocation());
        assertTrue(savedPost.isModeration(), "Модерация должна быть true");
        assertNotNull(savedPost.getHashId(), "HashId не должен быть null");
        assertFalse(savedPost.isArchived(), "Товар не должен быть архивирован изначально");
        assertNull(savedPost.getArchivedAt(), "ArchivedAt должен быть null");

        System.out.println("Создан товар для архивации с ID: " + savedPost.getId() + " и названием: " + uniqueTitle);
    }


    @Test
    void testManualArchiveProductByTitleViaService() {
        List<PostText> postsBeforeArchive = postRepo.findByTitleContainingIgnoreCase("zaeblo_product_");
        assertFalse(postsBeforeArchive.isEmpty(), "Товар должен существовать перед архивацией");
        PostText postToArchive = postsBeforeArchive.get(0);

        LocalDateTime now = LocalDateTime.now();
        postToArchive.setArchived(true);
        postToArchive.setArchivedAt(now);
        postRepo.save(postToArchive);

        PostText archivedPost = postRepo.findById(postToArchive.getId()).orElseThrow();
        assertTrue(archivedPost.isArchived(), "Товар должен быть помечен как archived");
        assertNotNull(archivedPost.getArchivedAt(), "ArchivedAt должен быть установлен");

        System.out.println("Товар успешно заархивирован вручную: ID=" + archivedPost.getId());
    }

    @RepeatedTest(10)
    void testCreateApartmentForArchive() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTitle = "zaeblo_apartment_" + uniqueSuffix;

        System.out.println("=== Запуск теста " + uniqueSuffix + ": создание квартиры для архивации ===");

        io.restassured.filter.session.SessionFilter sessionFilter =
                new io.restassured.filter.session.SessionFilter();

        // 1. CSRF
        String csrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        // 2. Логин test/test
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "test")
                .formParam("password", "test")
                .formParam("_csrf", csrfToken)
                .when()
                .post("/login")
                .then()
                .statusCode(302);

        // 3. Новый CSRF
        String newCsrfToken = given()
                .port(port)
                .filter(sessionFilter)
                .when()
                .get("/page_two/viewstwo")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");

        // 4. Создание квартиры
        given()
                .port(port)
                .filter(sessionFilter)
                .cookie("XSRF-TOKEN", newCsrfToken)
                .header("X-XSRF-TOKEN", newCsrfToken)
                .multiPart("title", uniqueTitle)
                .multiPart("price", "5000000")
                .multiPart("fullText", "Apartment for archive - " + uniqueSuffix)
                .multiPart("category", "realty")
                .multiPart("location", "Moscow")
                .multiPart("moderation", "true")
                // === Специфика квартиры ===
                .multiPart("propertyType", "apartment")
                .multiPart("roomsCount", "3")
                .multiPart("floorsApartment", "5")
                .multiPart("floorsCount", "9")
                .multiPart("hasElevator", "true")
                .multiPart("buildYear", "2010")
                .multiPart("materialUp", "brick")
                .multiPart("repairUp", "euro")
                // === Картинка ===
                .multiPart("files", "test_image.jpg", testImageBytes, "image/jpeg")
                .contentType("multipart/form-data")
                .when()
                .post("/page_two/viewstwo")
                .then()
                .statusCode(302);

        // 5. Проверка в БД
        List<PostText> posts = postRepo.findByTitleContainingIgnoreCase(uniqueTitle);
        assertFalse(posts.isEmpty(), "Квартира должна быть сохранена в БД");

        PostText savedPost = posts.get(0);

        assertEquals(uniqueTitle, savedPost.getTitle());
        assertEquals(5000000, savedPost.getPrice());
        assertEquals("Apartment for archive - " + uniqueSuffix, savedPost.getFull_text());
        assertEquals("realty", savedPost.getCategory());
        assertEquals("москва", savedPost.getLocation());
        assertTrue(savedPost.isModeration(), "Модерация должна быть true");
        assertNotNull(savedPost.getHashId(), "HashId не должен быть null");
        assertFalse(savedPost.isArchived(), "Квартира не должна быть архивирована изначально");
        assertNull(savedPost.getArchivedAt(), "ArchivedAt должен быть null");

        // === Проверяем, что это именно RealEstatePostText, а не базовый PostText ===
        assertTrue(savedPost instanceof RealEstatePostText,
                "Должен быть создан RealEstatePostText, а не базовый PostText");

        RealEstatePostText realty = (RealEstatePostText) savedPost;
        assertEquals("apartment", realty.getPropertyType());
        assertEquals(3, realty.getRoomsCount());
        assertEquals(5, realty.getFloorsApartment());
        assertEquals(9, realty.getFloorsCount());
        assertTrue(realty.getHasElevator());
        assertEquals(2010, realty.getBuildYear());
        assertEquals("brick", realty.getMaterialUp());
        assertEquals("euro", realty.getRepairUp());

        System.out.println("Создана квартира для архивации с ID: " + savedPost.getId()
                + " и названием: " + uniqueTitle);
    }


    @Test
    void testManualArchiveApartmentByTitleViaService() {
        List<PostText> postsBeforeArchive =
                postRepo.findByTitleContainingIgnoreCase("zaeblo_apartment_");
        assertFalse(postsBeforeArchive.isEmpty(),
                "Квартира должна существовать перед архивацией");

        PostText postToArchive = postsBeforeArchive.get(0);

        LocalDateTime now = LocalDateTime.now();
        postToArchive.setArchived(true);
        postToArchive.setArchivedAt(now);
        postRepo.save(postToArchive);

        PostText archivedPost = postRepo.findById(postToArchive.getId()).orElseThrow();
        assertTrue(archivedPost.isArchived(), "Квартира должна быть помечена как archived");
        assertNotNull(archivedPost.getArchivedAt(), "ArchivedAt должен быть установлен");

        System.out.println("Квартира успешно заархивирована вручную: ID=" + archivedPost.getId());
    }

}