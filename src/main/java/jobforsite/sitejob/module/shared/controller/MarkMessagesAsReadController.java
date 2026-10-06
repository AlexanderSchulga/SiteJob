package jobforsite.sitejob.module.shared.controller;

import jobforsite.sitejob.module.shared.services.MarkMessagesAsReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MarkMessagesAsReadController {

    private final MarkMessagesAsReadService service;

    @PostMapping("/api/messages/{postId}/markAsRead")
    public ResponseEntity<?> personal(@PathVariable Long postId, Authentication auth) {
        return service.markMessagesAsRead(postId, auth);
    }

    @PostMapping("/api/messages/job/{postId}/markAsRead")
    public ResponseEntity<?> job(@PathVariable Long postId, Authentication auth) {
        return service.markMessagesAsReadJob(postId, auth);
    }

    @PostMapping("/api/messages/vac/{postId}/markAsRead")
    public ResponseEntity<?> vac(@PathVariable Long postId, Authentication auth) {
        return service.markMessagesAsReadVac(postId, auth);
    }

    @PostMapping("/api/messages/res/{resId}/markAsRead")
    public ResponseEntity<?> res(@PathVariable Long resId, Authentication auth) {
        return service.markMessagesAsReadRes(resId, auth);
    }
}
