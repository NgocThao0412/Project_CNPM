package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"BUS", "config", "controller", "DAO", "DTO"})
public class SguAudioGuideApplication {

	public static void main(String[] args) {
		SpringApplication.run(SguAudioGuideApplication.class, args);
	}

}
