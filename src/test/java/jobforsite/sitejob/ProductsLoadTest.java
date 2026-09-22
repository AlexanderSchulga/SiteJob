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
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductsLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // Статистика
    private ConcurrentHashMap<String, UserStats> userStatsMap = new ConcurrentHashMap<>();
    private AtomicInteger totalSuccessCount = new AtomicInteger(0);
    private AtomicInteger totalRateLimitedCount = new AtomicInteger(0);
    private AtomicInteger totalError500Count = new AtomicInteger(0);
    private AtomicInteger totalOtherErrorCount = new AtomicInteger(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    private static final String PRODUCTS_URL = "/page_one/viewsone/load-more";
    private static final int RATE_LIMIT = 60; // 60 запросов в минуту

    private static class UserStats {
        String username;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger error500Count = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        UserStats(String username) {
            this.username = username;
        }
    }

    @Test
    public void testRateLimitShouldReturn429() throws InterruptedException {
        // Один пользователь делает много запросов
        int requestsPerUser = 100;

        runSingleUserTest("fef", requestsPerUser);
    }

    @Test
    public void testRateLimitWithMultipleUsers() throws InterruptedException {
        // Несколько пользователей - каждый должен иметь свой лимит
        String[] users = {"fef", "tet", "wew"};
        int requestsPerUser = 80; // Больше лимита для каждого

        runMultiUserTest(users, requestsPerUser);
    }

    private void runSingleUserTest(String username, int requestCount) throws InterruptedException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        userStatsMap.put(username, new UserStats(username));

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(requestCount);

        System.out.println("\n=== Rate Limit Test for user: " + username + " ===");
        System.out.println("Requests: " + requestCount);
        System.out.println("Rate limit: " + RATE_LIMIT + " per minute");
        System.out.println("======================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            executor.submit(new RateLimitTestTask(user, PRODUCTS_URL, latch));
        }

        latch.await(30, TimeUnit.SECONDS);

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdownNow();

        printResults("Single User Test", totalTime, requestCount);

        // Проверки
        UserStats stats = userStatsMap.get(username);

        // Должны быть rate limited запросы
        assertThat(stats.rateLimitedCount.get())
                .withFailMessage("Expected rate limited requests > 0, but got 0. All responses: success=%d, error500=%d, other=%d",
                        stats.successCount.get(), stats.error500Count.get(), stats.otherErrorCount.get())
                .isGreaterThan(0);

        // Успешных запросов не должно быть больше лимита
        assertThat(stats.successCount.get())
                .withFailMessage("Success count %d exceeds rate limit %d",
                        stats.successCount.get(), RATE_LIMIT)
                .isLessThanOrEqualTo(RATE_LIMIT);

        // Не должно быть ошибок 500
        assertThat(stats.error500Count.get())
                .withFailMessage("Found %d errors 500", stats.error500Count.get())
                .isEqualTo(0);
    }

    private void runMultiUserTest(String[] usernames, int requestsPerUser) throws InterruptedException {
        int totalRequests = usernames.length * requestsPerUser;

        List<User> users = new ArrayList<>();
        for (String username : usernames) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
            users.add(user);
            userStatsMap.put(username, new UserStats(username));
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        System.out.println("\n=== Multi-User Rate Limit Test ===");
        System.out.println("Users: " + usernames.length);
        System.out.println("Requests per user: " + requestsPerUser);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Rate limit per user: " + RATE_LIMIT + " per minute");
        System.out.println("====================================");

        long startTime = System.currentTimeMillis();

        for (User user : users) {
            for (int i = 0; i < requestsPerUser; i++) {
                executor.submit(new RateLimitTestTask(user, PRODUCTS_URL, latch));
            }
        }

        latch.await(30, TimeUnit.SECONDS);

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdownNow();

        printResults("Multi-User Test", totalTime, totalRequests);

        // Проверки для каждого пользователя
        for (String username : usernames) {
            UserStats stats = userStatsMap.get(username);

            assertThat(stats.rateLimitedCount.get())
                    .withFailMessage("User %s should have rate limited requests", username)
                    .isGreaterThan(0);

            assertThat(stats.successCount.get())
                    .withFailMessage("User %s success count %d exceeds rate limit",
                            username, stats.successCount.get())
                    .isLessThanOrEqualTo(RATE_LIMIT);

            assertThat(stats.error500Count.get())
                    .withFailMessage("User %s has errors 500", username)
                    .isEqualTo(0);
        }
    }

    private class RateLimitTestTask implements Runnable {
        private final User user;
        private final String url;
        private final CountDownLatch latch;

        RateLimitTestTask(User user, String url, CountDownLatch latch) {
            this.user = user;
            this.url = url;
            this.latch = latch;
        }

        @Override
        public void run() {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            UserStats userStats = userStatsMap.get(user.getUsername());

            try {
                long requestStartTime = System.currentTimeMillis();

                MvcResult result = mockMvc.perform(get(url)
                                .param("category", "all")
                                .param("search", "")
                                .param("sort", "newest")
                                .param("priceSort", "none")
                                .param("region", "all")
                                .param("city", "all")
                                .param("page", "0")
                                .param("size", "9")
                                .with(securityContext(context))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andReturn();

                int status = result.getResponse().getStatus();
                String response = result.getResponse().getContentAsString();
                long responseTime = System.currentTimeMillis() - requestStartTime;

                synchronized (this) {
                    if (status == 200) {
                        if (response.contains("\"status\":\"success\"")) {
                            userStats.successCount.incrementAndGet();
                            userStats.totalResponseTime.addAndGet(responseTime);
                            totalSuccessCount.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);
                            System.out.printf("User %s - Success (200). Time: %d ms%n",
                                    user.getUsername(), responseTime);
                        } else {
                            userStats.otherErrorCount.incrementAndGet();
                            totalOtherErrorCount.incrementAndGet();
                            System.err.printf("User %s - Unexpected 200 response%n", user.getUsername());
                        }
                    } else if (status == 429) {
                        userStats.rateLimitedCount.incrementAndGet();
                        userStats.totalResponseTime.addAndGet(responseTime);
                        totalRateLimitedCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);
                        System.out.printf("User %s - RATE LIMITED (429). Time: %d ms%n",
                                user.getUsername(), responseTime);
                    } else if (status == 500) {
                        userStats.error500Count.incrementAndGet();
                        totalError500Count.incrementAndGet();
                        System.err.printf("User %s - ERROR 500. Time: %d ms%n",
                                user.getUsername(), responseTime);
                    } else {
                        userStats.otherErrorCount.incrementAndGet();
                        totalOtherErrorCount.incrementAndGet();
                        System.err.printf("User %s - Other status: %d%n",
                                user.getUsername(), status);
                    }
                }

            } catch (Exception e) {
                synchronized (this) {
                    userStats.otherErrorCount.incrementAndGet();
                    totalOtherErrorCount.incrementAndGet();
                    System.err.printf("User %s - Exception: %s%n", user.getUsername(), e.getMessage());
                }
            } finally {
                SecurityContextHolder.clearContext();
                latch.countDown();
            }
        }
    }

    private void printResults(String testName, long totalTime, int totalRequests) {
        int totalProcessed = totalSuccessCount.get() + totalRateLimitedCount.get() +
                totalError500Count.get() + totalOtherErrorCount.get();

        System.out.println("\n=== " + testName + " Results ===");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Successful (200): " + totalSuccessCount.get());
        System.out.println("Rate Limited (429): " + totalRateLimitedCount.get());
        System.out.println("Errors (500): " + totalError500Count.get());
        System.out.println("Other Errors: " + totalOtherErrorCount.get());
        System.out.println("Total processed: " + totalProcessed);
        System.out.println("Total requests expected: " + totalRequests);

        if (totalProcessed > 0) {
            System.out.println("Average response time: " + (totalResponseTime.get() / totalProcessed) + " ms");
        }

        System.out.println("\n=== Per-User Results ===");
        for (UserStats stats : userStatsMap.values()) {
            int userTotal = stats.successCount.get() + stats.rateLimitedCount.get() +
                    stats.error500Count.get() + stats.otherErrorCount.get();
            System.out.printf("User: %s%n", stats.username);
            System.out.printf("  Success (200): %d%n", stats.successCount.get());
            System.out.printf("  Rate Limited (429): %d%n", stats.rateLimitedCount.get());
            System.out.printf("  Error (500): %d%n", stats.error500Count.get());
            System.out.printf("  Other Errors: %d%n", stats.otherErrorCount.get());
            if (userTotal > 0) {
                System.out.printf("  Avg response time: %d ms%n",
                        stats.totalResponseTime.get() / userTotal);
            }
        }
    }
}