package controller;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HealthController {
  @Value("${spring.application.name}") String service;
  @GetMapping("/api/health") public Map<String,String> health(){return Map.of("status","OK","service",service);}
}
