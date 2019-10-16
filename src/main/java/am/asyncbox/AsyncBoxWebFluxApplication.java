package am.asyncbox;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("with-webflux")
@SpringBootApplication
public class AsyncBoxWebFluxApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AsyncBoxWebFluxApplication.class)
                .profiles("with-webflux")
                .web(WebApplicationType.REACTIVE)
                .run(args);
    }

    @Bean
    public ReactiveWebServerFactory webFactory() {
        return new NettyReactiveWebServerFactory();
    }
}
