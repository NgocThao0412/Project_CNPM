package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"app", "BUS", "client", "controller", "DAO"})
public class RouteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RouteServiceApplication.class, args);
    }
}
