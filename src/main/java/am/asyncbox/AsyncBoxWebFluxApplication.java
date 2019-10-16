package am.asyncbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AsyncBoxWebFluxApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AsyncBoxApplication.class);
        app.setAdditionalProfiles("with-webflux");

        app.run(args);
    }
}
