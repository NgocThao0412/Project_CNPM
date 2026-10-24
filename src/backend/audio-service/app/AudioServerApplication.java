package app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"BUS", "config", "controller", "DAO", "DTO", "util"})
public class AudioServerApplication {public static void main(String[] args){SpringApplication.run(AudioServerApplication.class,args);}}
