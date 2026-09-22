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
public class SupportRateLimitLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger rateLimitedCount = new AtomicInteger(0);
    private AtomicInteger errorCount = new AtomicInteger(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    // ID существующего тикета для тестирования
    private static final Long TEST_TICKET_ID = 72L;
    private static final Long TEST_REPLY_ID = 1L;

    @Test
    public void testRateLimitWithConcurrentRequests() throws InterruptedException {
        int threadCount = 50;
        int requestsPerThread = 3;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        successCount.set(0);
        rateLimitedCount.set(0);
        errorCount.set(0);
        totalResponseTime.set(0);

        System.out.println("=== Starting Rate Limit Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Rate limit: 10 requests per 60 seconds");
        System.out.println("=====================================");

        long startTime = System.currentTimeMillis();

        // ПОЛУЧАЕМ РЕАЛЬНОГО ПОЛЬЗОВАТЕЛЯ ИЗ БАЗЫ
        User realUser = userRepository.findByUsername("fef")
                .orElseThrow(() -> new RuntimeException("Тестовый пользователь не найден"));

        // Создаем аутентификацию с реальным пользователем
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        realUser,
                        null,
                        realUser.getAuthorities()
                );

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

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
                                    totalResponseTime.addAndGet(responseTime);

                                    if (location != null && location.contains("/message/")) {
                                        successCount.incrementAndGet();
                                        System.out.printf("Success! Response time: %d ms%n", responseTime);
                                    } else if (location != null && location.contains("support_service")) {
                                        if (result.getFlashMap().containsKey("messageType") &&
                                                "warning".equals(result.getFlashMap().get("messageType"))) {
                                            rateLimitedCount.incrementAndGet();
                                            System.out.printf("Rate limited! Response time: %d ms%n", responseTime);
                                        } else {
                                            successCount.incrementAndGet();
                                            System.out.printf("Success! Response time: %d ms%n", responseTime);
                                        }
                                    } else {
                                        errorCount.incrementAndGet();
                                        System.out.printf("Unexpected redirect: %s%n", location);
                                    }
                                });

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await();

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        int totalProcessed = successCount.get() + rateLimitedCount.get();

        System.out.println("\n=== Test Results ===");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Rate limited requests: " + rateLimitedCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Total requests: " + (totalProcessed + errorCount.get()));

        if (totalProcessed > 0) {
            System.out.println("Average response time: " + (totalResponseTime.get() / totalProcessed) + " ms");
        }

        System.out.println("Requests per second: " + (totalRequests * 1000.0 / totalTime));

        // Проверки
        assertThat(rateLimitedCount.get()).isGreaterThan(0);
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        assertThat(errorCount.get()).isEqualTo(0);
    }

}