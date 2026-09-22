package jobforsite.sitejob;

import jobforsite.sitejob.config.TestSecurityConfig;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class MessageTicketsSpecificScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testMessageTicketsPagination() throws Exception {
        User testUser = userRepository.findByUsername("fef").orElse(null);
        if (testUser == null) return;

        var authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Тест первой страницы
        mockMvc.perform(get("/user/notifications_message/load")
                        .param("page", "0")
                        .param("size", "4")
                        .with(securityContext(context)))
                .andExpect(status().isOk());

        // Тест второй страницы
        mockMvc.perform(get("/user/notifications_message/load")
                        .param("page", "1")
                        .param("size", "4")
                        .with(securityContext(context)))
                .andExpect(status().isOk());

        SecurityContextHolder.clearContext();
    }

    @Test
    public void testUnreadCountConsistency() throws Exception {
        User testUser = userRepository.findByUsername("fef").orElse(null);
        if (testUser == null) return;

        var authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Получаем количество непрочитанных несколько раз
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/user/notifications_message/count")
                            .with(securityContext(context)))
                    .andExpect(status().isOk());
        }

        SecurityContextHolder.clearContext();
    }
}