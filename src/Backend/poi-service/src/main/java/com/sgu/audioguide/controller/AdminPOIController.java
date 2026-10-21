package com.sgu.audioguide.controller;

import com.sgu.audioguide.BUS.POIBUS;
import com.sgu.audioguide.DTO.CreatePOIRequestDTO;
import com.sgu.audioguide.client.AuthServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.sgu.audioguide.DTO.UpdatePOIRequestDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import com.sgu.audioguide.DTO.UpdatePOIVisibilityRequestDTO;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.NoSuchElementException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pois")
public class AdminPOIController {

    private final POIBUS poiBUS;
    private final AuthServiceClient authServiceClient;

    public AdminPOIController(
            POIBUS poiBUS,
            AuthServiceClient authServiceClient
    ) {
        this.poiBUS = poiBUS;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPOI(
            @Valid @RequestBody CreatePOIRequestDTO request,
            BindingResult validation,
            HttpServletRequest httpRequest
    ) {
        try {
            // POI Service gọi Auth Service để xác thực phiên.
            authServiceClient.requireAdmin(httpRequest);

            if (validation.hasErrors()) {
                Map<String, String> errors = new LinkedHashMap<>();

                for (FieldError error : validation.getFieldErrors()) {
                    errors.putIfAbsent(
                            error.getField(),
                            error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Giá trị không hợp lệ"
                    );
                }

                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Dữ liệu POI không hợp lệ",
                        "errors", errors
                ));
            }

            Long id = poiBUS.createPOI(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Thêm POI thành công",
                            "id", id
                    ));

        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of(
                            "message",
                            exception.getReason() != null
                            ? exception.getReason()
                            : "Không thể xác thực tài khoản"
                    ));

        } catch (DuplicateKeyException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Mã POI đã tồn tại"
                    ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", exception.getMessage()
                    ));

        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Dữ liệu liên quan đã thay đổi hoặc vi phạm ràng buộc"
                    ));

        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Chưa thể lưu POI, vui lòng thử lại"
                    ));
        }
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updatePOI(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePOIRequestDTO request,
            BindingResult validation,
            HttpServletRequest httpRequest
    ) {
        try {
            authServiceClient.requireAdmin(httpRequest);

            if (validation.hasErrors()) {
                Map<String, String> errors = new LinkedHashMap<>();

                for (FieldError error : validation.getFieldErrors()) {
                    errors.putIfAbsent(
                            error.getField(),
                            error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Giá trị không hợp lệ"
                    );
                }

                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Dữ liệu POI không hợp lệ",
                        "errors", errors
                ));
            }

            poiBUS.updatePOI(id, request);

            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật POI thành công",
                    "id", id
            ));

        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of(
                            "message",
                            exception.getReason() != null
                            ? exception.getReason()
                            : "Không thể xác thực tài khoản"
                    ));

        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", exception.getMessage()
                    ));

        } catch (DuplicateKeyException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Mã POI đã tồn tại"
                    ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", exception.getMessage()
                    ));

        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Dữ liệu liên quan đã thay đổi hoặc vi phạm ràng buộc"
                    ));

        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Chưa thể cập nhật POI, vui lòng thử lại"
                    ));
        }
    }

    @PatchMapping(
            value = "/{id}/visibility",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updateVisibility(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePOIVisibilityRequestDTO request,
            BindingResult validation,
            HttpServletRequest httpRequest
    ) {
        try {
            authServiceClient.requireAdmin(httpRequest);

            if (validation.hasErrors()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Vui lòng chọn trạng thái hiển thị"
                ));
            }

            poiBUS.updateVisibility(id, request.visible());

            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật trạng thái POI thành công",
                    "id", id,
                    "visible", request.visible()
            ));

        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of(
                            "message",
                            exception.getReason() != null
                            ? exception.getReason()
                            : "Không thể xác thực tài khoản"
                    ));

        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", exception.getMessage()
                    ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", exception.getMessage()
            ));

        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Chưa thể cập nhật trạng thái POI, vui lòng thử lại"
                    ));
        }
    }
}
