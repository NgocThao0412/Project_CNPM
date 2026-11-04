package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

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

        String description,

        // Nullable to retain compatibility with clients that only edit text fields.
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        @Digits(integer = 3, fraction = 7)
        BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        @Digits(integer = 3, fraction = 7)
        BigDecimal longitude,

        @DecimalMin(value = "0.01") @DecimalMax(value = "999999.99")
        BigDecimal triggerRadiusM
) {
}