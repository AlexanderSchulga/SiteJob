package jobforsite.sitejob;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SupportRateLimitMultiUserLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // Статистика для каждого пользователя
    private ConcurrentHashMap<String, UserStats> userStatsMap = new ConcurrentHashMap<>();

    // Общая статистика
    private AtomicInteger totalSuccessCount = new AtomicInteger(0);
    private AtomicInteger totalRateLimitedCount = new AtomicInteger(0);
    private AtomicInteger totalErrorCount = new AtomicInteger(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    // ID существующего тикета для тестирования
    private static final Long TEST_TICKET_ID = 72L;
    private static final Long TEST_REPLY_ID = 1L;

    // Класс для хранения статистики по пользователю
    private static class UserStats {
        String username;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        UserStats(String username) {
            this.username = username;
        }
    }

    @Test
    public void testRateLimitWithMultipleUsers() throws InterruptedException {
        // Количество пользователей
        int userCount = 5;
        // Количество потоков на пользователя
        int threadsPerUser = 10;
        // Запросов на поток
        int requestsPerThread = 3;

        int totalRequests = userCount * threadsPerUser * requestsPerThread;

        // Получаем реальных пользователей из базы
        List<User> testUsers = new ArrayList<>();

        // Добавляем существующих пользователей
        String[] usernames = {"fef", "tet", "wew", "uuu", "vvv"};

        for (String username : usernames) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Тестовый пользователь не найден: " + username));
            testUsers.add(user);
            userStatsMap.put(username, new UserStats(username));
        }

        ExecutorService executor = Executors.newFixedThreadPool(userCount * threadsPerUser);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // Сброс счетчиков
        totalSuccessCount.set(0);
        totalRateLimitedCount.set(0);
        totalErrorCount.set(0);
        totalResponseTime.set(0);

        System.out.println("=== Starting Multi-User Rate Limit Load Test ===");
        System.out.println("Users: " + userCount);
        System.out.println("Threads per user: " + threadsPerUser);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Rate limit: 10 requests per 60 seconds per user");
        System.out.println("=================================================");

        long startTime = System.currentTimeMillis();

        // Запускаем потоки для каждого пользователя
        for (User user : testUsers) {
            for (int t = 0; t < threadsPerUser; t++) {
                executor.submit(new UserRequestTask(user, requestsPerThread, latch));
            }
        }

        latch.await();

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        int totalProcessed = totalSuccessCount.get() + totalRateLimitedCount.get();

        System.out.println("\n=== Overall Test Results ===");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Total successful requests: " + totalSuccessCount.get());
        System.out.println("Total rate limited requests: " + totalRateLimitedCount.get());
        System.out.println("Total errors: " + totalErrorCount.get());
        System.out.println("Total requests: " + (totalProcessed + totalErrorCount.get()));

        if (totalProcessed > 0) {
            System.out.println("Average response time: " + (totalResponseTime.get() / totalProcessed) + " ms");
        }

        System.out.println("Requests per second: " + (totalRequests * 1000.0 / totalTime));

        // Выводим статистику по каждому пользователю
        System.out.println("\n=== Per-User Results ===");
        for (UserStats stats : userStatsMap.values()) {
            int userTotal = stats.successCount.get() + stats.rateLimitedCount.get();
            System.out.printf("User: %s%n", stats.username);
            System.out.printf("  Success: %d%n", stats.successCount.get());
            System.out.printf("  Rate limited: %d%n", stats.rateLimitedCount.get());
            System.out.printf("  Errors: %d%n", stats.errorCount.get());
            System.out.printf("  Total: %d%n", userTotal);
            if (userTotal > 0) {
                System.out.printf("  Avg response time: %d ms%n",
                        stats.totalResponseTime.get() / userTotal);
            }
            System.out.println();
        }

        // Проверки для каждого пользователя
        for (UserStats stats : userStatsMap.values()) {
            // Каждый пользователь должен иметь rate limited запросы
            assertThat(stats.rateLimitedCount.get())
                    .withFailMessage("User %s should have rate limited requests", stats.username)
                    .isGreaterThan(0);

            // Каждый пользователь не должен превышать лимит в 10 успешных запросов
            assertThat(stats.successCount.get())
                    .withFailMessage("User %s exceeded rate limit (successful requests: %d)",
                            stats.username, stats.successCount.get())
                    .isLessThanOrEqualTo(10);

            // Нет ошибок
            assertThat(stats.errorCount.get())
                    .withFailMessage("User %s had errors: %d", stats.username, stats.errorCount.get())
                    .isEqualTo(0);
        }

        // Общие проверки
        assertThat(totalRateLimitedCount.get()).isGreaterThan(0);
        assertThat(totalErrorCount.get()).isEqualTo(0);
    }

    // Внутренний класс для выполнения запросов от имени пользователя
    private class UserRequestTask implements Runnable {
        private final User user;
        private final int requestsPerThread;
        private final CountDownLatch latch;

        UserRequestTask(User user, int requestsPerThread, CountDownLatch latch) {
            this.user = user;
            this.requestsPerThread = requestsPerThread;
            this.latch = latch;
        }

        @Override
        public void run() {
            // Создаем аутентификацию для конкретного пользователя
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            UserStats userStats = userStatsMap.get(user.getUsername());

            for (int j = 0; j < requestsPerThread; j++) {
                try {
                    long requestStartTime = System.currentTimeMillis();

                    mockMvc.perform(get("/message/support/{ticketId}", TEST_TICKET_ID)
                                    .param("replyId", TEST_REPLY_ID.toString())
                                    .with(securityContext(context))
                                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                            .andExpect(status().is3xxRedirection())
                            .andDo(result -> {
                                String location = result.getResponse().getRedirectedUrl();
                                long responseTime = System.currentTimeMillis() - requestStartTime;

                                if (location != null && location.contains("/message/")) {
                                    // Успешный запрос
                                    successCountHandler(userStats, responseTime);
                                } else if (location != null && location.contains("support_service")) {
                                    if (result.getFlashMap().containsKey("messageType") &&
                                            "warning".equals(result.getFlashMap().get("messageType"))) {
                                        // Rate limited
                                        rateLimitedHandler(userStats, responseTime);
                                    } else {
                                        // Успешный запрос с другим редиректом
                                        successCountHandler(userStats, responseTime);
                                    }
                                } else {
                                    // Неожиданный редирект
                                    errorHandler(userStats);
                                }
                            });

                } catch (Exception e) {
                    errorHandler(userStats);
                    System.err.printf("Error for user %s: %s%n", user.getUsername(), e.getMessage());
                } finally {
                    latch.countDown();
                }
            }

            SecurityContextHolder.clearContext();
        }

        private void successCountHandler(UserStats userStats, long responseTime) {
            userStats.successCount.incrementAndGet();
            userStats.totalResponseTime.addAndGet(responseTime);
            totalSuccessCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            System.out.printf("User %s - Success! Response time: %d ms%n",
                    user.getUsername(), responseTime);
        }

        private void rateLimitedHandler(UserStats userStats, long responseTime) {
            userStats.rateLimitedCount.incrementAndGet();
            userStats.totalResponseTime.addAndGet(responseTime);
            totalRateLimitedCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            System.out.printf("User %s - Rate limited! Response time: %d ms%n",
                    user.getUsername(), responseTime);
        }

        private void errorHandler(UserStats userStats) {
            userStats.errorCount.incrementAndGet();
            totalErrorCount.incrementAndGet();
            System.err.printf("User %s - Error!%n", user.getUsername());
        }
    }
}