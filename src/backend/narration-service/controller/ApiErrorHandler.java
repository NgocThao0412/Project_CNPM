package controller;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class ApiErrorHandler {
   @ExceptionHandler(IllegalArgumentException.class)
   public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex){
      return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Dữ liệu không hợp lệ":ex.getMessage()));
   }
   @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<Map<String,String>> duplicate(DataIntegrityViolationException ex){
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Dữ liệu trùng hoặc vi phạm ràng buộc MySQL"));
   }
}
