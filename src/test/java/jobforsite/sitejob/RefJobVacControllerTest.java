//package jobforsite.sitejob;
//
//import jobforsite.sitejob.module.service.model.PostTextJob;
//import jobforsite.sitejob.module.vacancy.model.Vacancy;
//import jobforsite.sitejob.module.service.repository.PostRepoJob;
//import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.test.context.TestPropertySource;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//
//import static io.restassured.RestAssured.given;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@TestPropertySource(locations = "classpath:application.properties", properties = {
//        "spring.security.csrf.enabled=false"
//})
//public class RefJobVacControllerTest {
//
//    @LocalServerPort
//    private int port;
//
//    @Autowired
//    private PostRepoJob postRepoJob;
//    @Autowired
//    private PostVacancyRepository postVacancyRepository;
//
//    private byte[] testImageBytes;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        // ????????? ???????? ??????????? ?? ????? static/images
//        Path imagePath = Paths.get("src", "main", "resources", "static", "images", "lilac.jpg");
//        if (Files.exists(imagePath)) {
//            testImageBytes = Files.readAllBytes(imagePath);
//        } else {
//            throw new RuntimeException("???? lilac.jpg ?? ?????? ?? ????: " + imagePath.toAbsolutePath());
//        }
//
//        testCreateSevice_Success_WithCSRF();
//        testCreateSevice_Success_WithCSRF();
//        testCreateSevice_Success_WithCSRF();
//        testCreateSevice_Success_WithCSRF();
//
////        testCreateVacancy_Success_WithCSRF();
////        testCreateVacancy_Success_WithCSRF();
////        testCreateVacancy_Success_WithCSRF();
////        testCreateVacancy_Success_WithCSRF();
//    }
//
//
//    @Test
//    void testCreateSevice_Success_WithCSRF() throws IOException {
//        // ?????????? SessionFilter ??? ??????????????? ?????????? ??????
//        io.restassured.filter.session.SessionFilter sessionFilter = new io.restassured.filter.session.SessionFilter();
//
//        // ??????? ???????? CSRF ????? ????? GET ?????? ?? ???????? ??????
//        String csrfToken = given()
//                .port(port)
//                .filter(sessionFilter)
//                .when()
//                .get("/login")
//                .then()
//                .statusCode(200)
//                .extract()
//                .cookie("XSRF-TOKEN");
//
//        // ????????? ????? ? CSRF ???????
//        given()
//                .port(port)
//                .filter(sessionFilter)
//                .cookie("XSRF-TOKEN", csrfToken)
//                .header("X-XSRF-TOKEN", csrfToken)
//                .contentType("application/x-www-form-urlencoded")
//                .formParam("username", "sev")
//                .formParam("password", "220411")
//                .formParam("_csrf", csrfToken) // ????????? CSRF ??? ???????? ?????
//                .when()
//                .post("/login")
//                .then()
//                .statusCode(302); // ??????? redirect ????? ????????? ??????
//
//        // ???????? ??????????? CSRF ????? ????? ??????
//        String newCsrfToken = given()
//                .port(port)
//                .filter(sessionFilter)
//                .when()
//                .get("page_two/viewstwojob")
//                .then()
//                .statusCode(200)
//                .extract()
//                .cookie("XSRF-TOKEN");
//
//        // ????????? POST ?????? ??? ???????? ??????
//        given()
//                .port(port)
//                .filter(sessionFilter)
//                .cookie("XSRF-TOKEN", newCsrfToken)
//                .header("X-XSRF-TOKEN", newCsrfToken)
//                .multiPart("title", "Test Title")
//                .multiPart("anons", "Test Anons")
//                .multiPart("full_text", "Test Full Text")
//                .multiPart("category", "electronics")
//                .multiPart("location", "Test Location")
//                .multiPart("moderation", "false")
//                .multiPart("files", "lilac.jpg", testImageBytes, "image/jpeg")
//                .contentType("multipart/form-data")
//                .when()
//                .post("page_two/viewstwojob")
//                .then()
//                .statusCode(302); // ??????? redirect ????? ????????? ????????
//
//        List<PostTextJob> posts = postRepoJob.findByTitleContainingIgnoreCase("Test Title");
//        assertFalse(posts.isEmpty(), "���� ������ ���� �������� � ���� ������");
//
//        PostTextJob savedPost = posts.get(0);
//        assertEquals("Test Title", savedPost.getTitle());
//        assertEquals("Test Anons", savedPost.getAnons());
//        assertEquals("Test Full Text", savedPost.getFull_text());
//        assertEquals("electronics", savedPost.getCategory());
//        assertEquals("Test Location", savedPost.getLocation());
//        assertFalse(savedPost.isModeration());
//
//        // ???????: ??????? ????????? ????
//        //postRepo.delete(savedPost);
//    }
//
//    @Test
//    void testCreateVacancy_Success_WithCSRF() throws IOException {
//        // �������� SessionFilter ��� ����������� ��������� ������
//        io.restassured.filter.session.SessionFilter sessionFilter = new io.restassured.filter.session.SessionFilter();
//
//        // ��������� CSRF ������ ����� GET ������ �� �������� ������
//        String csrfToken = given()
//                .port(port)
//                .filter(sessionFilter)
//                .when()
//                .get("/login")
//                .then()
//                .statusCode(200)
//                .extract()
//                .cookie("XSRF-TOKEN");
//
//        // ���������� ������ � CSRF �������
//        given()
//                .port(port)
//                .filter(sessionFilter)
//                .cookie("XSRF-TOKEN", csrfToken)
//                .header("X-XSRF-TOKEN", csrfToken)
//                .contentType("application/x-www-form-urlencoded")
//                .formParam("username", "ded")
//                .formParam("password", "220433")
//                .formParam("_csrf", csrfToken) // �������� CSRF ��� ��������������
//                .when()
//                .post("/login")
//                .then()
//                .statusCode(302); // ������� redirect ����� �������� ��������������
//
//        // ��������� ������ CSRF ������ ����� ������
//        String newCsrfToken = given()
//                .port(port)
//                .filter(sessionFilter)
//                .when()
//                .get("page_two/viewstwovac")  // ���������� ���� ��� ��������
//                .then()
//                .statusCode(200)
//                .extract()
//                .cookie("XSRF-TOKEN");
//
//        // ���������� POST ������� ��� �������� ��������
//        given()
//                .port(port)
//                .filter(sessionFilter)
//                .cookie("XSRF-TOKEN", newCsrfToken)
//                .header("X-XSRF-TOKEN", newCsrfToken)
//                .multiPart("title", "Test Vacancy Title")
//                .multiPart("cash", "1000 USD")  // ���������: cash ������ anons
//                .multiPart("full_text", "Test Vacancy Full Text")
//                .multiPart("category", "electronics")
//                .multiPart("location", "Test Location")
//                .multiPart("moderation", "false")
//                .multiPart("files", "lilac.jpg", testImageBytes, "image/jpeg")
//                .contentType("multipart/form-data")
//                .when()
//                .post("page_two/viewstwovac")  // ���������� ���� ��� ��������
//                .then()
//                .statusCode(302); // ������� redirect ����� ��������� ��������
//
//        List<Vacancy> posts = postVacancyRepository.findByTitleContainingIgnoreCase("Test Vacancy Title");
//        assertFalse(posts.isEmpty(), "���� ������ ���� �������� � ���� ������");
//
//        Vacancy savedPost = posts.get(0);
//        assertEquals("Test Vacancy Title", savedPost.getTitle());
//        assertEquals("1000 USD", savedPost.getCash());  // ��������� cash, � �� anons
//        assertEquals("Test Vacancy Full Text", savedPost.getFull_text());
//        assertEquals("electronics", savedPost.getCategory());
//        assertEquals("Test Location", savedPost.getLocation());
//        assertFalse(savedPost.isModeration());
//
//        // �������: �������� ���������� �����
//        // postVacancyRepository.delete(savedPost);
//    }
//}
