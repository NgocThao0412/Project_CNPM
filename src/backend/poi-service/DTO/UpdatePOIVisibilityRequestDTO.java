package DTO;

import jakarta.validation.constraints.NotNull;

public record UpdatePOIVisibilityRequestDTO(

        @NotNull(message = "Vui lòng chọn trạng thái hiển thị")
        Boolean visible
) {
}