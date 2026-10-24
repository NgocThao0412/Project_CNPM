package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdatePOIRequestDTO(

        @NotNull(message = "Vui lòng chọn khu vực")
        @Positive(message = "Mã khu vực không hợp lệ")
        Long areaId,

        @NotBlank(message = "Vui lòng nhập mã POI")
        @Size(max = 50, message = "Mã POI tối đa 50 ký tự")
        String poiCode,

        @NotBlank(message = "Vui lòng nhập tên POI")
        @Size(max = 200, message = "Tên POI tối đa 200 ký tự")
        String poiName,

        String description
) {
}