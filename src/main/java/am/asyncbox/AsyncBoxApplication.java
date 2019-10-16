package am.asyncbox;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@Profile("with-future")
@SpringBootApplication
public class AsyncBoxApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AsyncBoxApplication.class)
                .profiles("with-future")
                .web(WebApplicationType.SERVLET)
                .run(args);
    }
}
