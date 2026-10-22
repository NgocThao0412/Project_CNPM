package com.sgu.audioguide.controller;
import com.sgu.audioguide.BUS.*;
import com.sgu.audioguide.DTO.*;
import com.sgu.audioguide.client.AuthServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
/** Adds only the missing GET admin / PUT / PATCH / areas routes. Never duplicates POST /api/admin/pois. */
@RestController public class POIManagementController {
    private final POIManagementBUS bus;
    private final AuthServiceClient auth;
    public POIManagementController(POIManagementBUS bus,AuthServiceClient auth) {
        this.bus=bus;
        this.auth=auth;
    } @GetMapping("/api/admin/pois") public List<POIManagementDTO> list(HttpServletRequest r) {
        auth.requireAdmin(r);
        return bus.list();
    } @GetMapping("/api/campus-areas") public List<Map<String,Object>> areas() {
        return bus.areas();
    } @PutMapping("/api/admin/pois/{id}") public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody CreatePOIRequestDTO dto,BindingResult validation,HttpServletRequest r) {
        auth.requireAdmin(r);
        if(validation.hasErrors())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,validation.getFieldErrors().getFirst().getDefaultMessage());
        bus.update(id,dto);
        return Map.of("id",id,"message","Đã cập nhật POI");
    }
    public record Visibility(@NotNull Boolean visible) {
    } @PatchMapping("/api/admin/pois/{id}/visibility") public Map<String,Object> visibility(@PathVariable Long id,@Valid @RequestBody Visibility dto,BindingResult validation,HttpServletRequest r) {
        auth.requireAdmin(r);
        if(validation.hasErrors())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái hiển thị là bắt buộc");
        bus.visibility(id,dto.visible());
        return Map.of("id",id,"visible",dto.visible());
    } @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Thao tác không hợp lệ":e.getReason()));
    } @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message","Mã POI trùng hoặc dữ liệu liên kết không hợp lệ"));
    } @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> unavailable(DataAccessException e) {
        return ResponseEntity.status(503).body(Map.of("message","Chưa thể cập nhật MySQL. Dữ liệu chưa được xác nhận lưu."));
    }
}
