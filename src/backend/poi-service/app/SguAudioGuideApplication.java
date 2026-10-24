package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"BUS", "client", "controller", "DAO", "DTO"})
public class SguAudioGuideApplication {

	public static void main(String[] args) {
		SpringApplication.run(SguAudioGuideApplication.class, args);
	}

}
