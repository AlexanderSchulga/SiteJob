package jobforsite.sitejob.module.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
//@Service
public class SmsRuService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${smsru.api-key}")
    private String apiKey;

    public String sendFlashCall(String phone, String userIp) {
        try {
            String url = "https://sms.ru/code/call";

            // ВСЕГДА передаем -1 для Flash Call
            // Это говорит SMS.RU: "звони с любого доступного IP, не привязывайся к клиенту"
            String finalIp = "-1";

            log.info("Flashcall для номера {}: IP клиента {}, используем -1", phone, userIp);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("api_id", apiKey)
                    .queryParam("phone", phone)
                    .queryParam("ip", finalIp);

            String fullUrl = builder.toUriString();

            log.info("Отправка Flashcall на URL: {}", fullUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            log.info("Ответ SMS.RU: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText();

            if ("OK".equals(status)) {
                String callId = root.path("call_id").asText();
                String code = root.path("code").asText();
                double cost = root.path("cost").asDouble();
                double balance = root.path("balance").asDouble();

                log.info("Flashcall успешно отправлен! call_id: {}, код: {}, стоимость: {} руб., баланс: {} руб.",
                        callId, code, cost, balance);
                return code;
            } else {
                String statusText = root.path("status_text").asText();
                log.error("Ошибка отправки Flashcall: status={}, text={}", status, statusText);
                return null;
            }

        } catch (Exception e) {
            log.error("Ошибка отправки Flashcall на номер {}: {}", phone, e.getMessage(), e);
            return null;
        }
    }
}