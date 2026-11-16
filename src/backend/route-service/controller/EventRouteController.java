package controller;

import BUS.EventRouteBUS;
import client.AuthServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventRouteController {
    private final EventRouteBUS bus;
    private final AuthServiceClient auth;
    public EventRouteController(EventRouteBUS bus,AuthServiceClient auth){this.bus=bus;this.auth=auth;}
    @GetMapping("/api/event-routes")
    public List<Map<String,Object>> publicRoutes(){return bus.all(false);}
    @GetMapping("/api/event-routes/{id}")
    public Map<String,Object> publicRoute(@PathVariable long id){return bus.one(id,false);}
    @GetMapping("/api/admin/event-routes")
    public List<Map<String,Object>> adminRoutes(HttpServletRequest request){auth.requireAdmin(request);return bus.all(true);}
    @GetMapping("/api/admin/event-routes/{id}")
    public Map<String,Object> adminRoute(@PathVariable long id,HttpServletRequest request){auth.requireAdmin(request);return bus.one(id,true);}
    @PostMapping("/api/admin/event-routes")
    public Map<String,Object> create(@RequestBody Map<String,Object> body,HttpServletRequest request){
        auth.requireAdmin(request);return Map.of("id",bus.save(0,body));
    }
    @PutMapping("/api/admin/event-routes/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest request){
        auth.requireAdmin(request);return Map.of("id",bus.save(id,body));
    }
    @DeleteMapping("/api/admin/event-routes/{id}")
    public void delete(@PathVariable long id,HttpServletRequest request){auth.requireAdmin(request);bus.delete(id);}
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,String> notFound(NoSuchElementException ex){return Map.of("message",ex.getMessage());}
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(IllegalArgumentException ex){return Map.of("message",ex.getMessage());}
}
