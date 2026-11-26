package io.trove.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Trove 后端的单应用入口。
 * Entry point for the Trove backend.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@OpenAPIDefinition(info = @Info(title = "Trove API"))
public class TroveApplication {

    /**
     * 从命令行参数和外部环境加载配置并启动服务。
     * Starts the server using command-line arguments and external configuration.
     *
     * @param args Spring Boot 命令行参数 / Spring Boot command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TroveApplication.class, args);
    }
}
