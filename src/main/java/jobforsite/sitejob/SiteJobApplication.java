package jobforsite.sitejob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiteJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteJobApplication.class, args);
    }

}
