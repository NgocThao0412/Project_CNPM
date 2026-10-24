package DTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePOIRequestDTO(

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

        @NotNull(message = "Vui lòng nhập vĩ độ")
        @DecimalMin(value = "-90", message = "Vĩ độ phải từ -90 đến 90")
        @DecimalMax(value = "90", message = "Vĩ độ phải từ -90 đến 90")
        @Digits(
                integer = 3,
                fraction = 7,
                message = "Vĩ độ tối đa 7 chữ số thập phân"
        )
        BigDecimal latitude,

        @NotNull(message = "Vui lòng nhập kinh độ")
        @DecimalMin(value = "-180", message = "Kinh độ phải từ -180 đến 180")
        @DecimalMax(value = "180", message = "Kinh độ phải từ -180 đến 180")
        @Digits(
                integer = 3,
                fraction = 7,
                message = "Kinh độ tối đa 7 chữ số thập phân"
        )
        BigDecimal longitude,

        @NotNull(message = "Vui lòng nhập bán kính kích hoạt")
        @DecimalMin(
                value = "0",
                inclusive = false,
                message = "Bán kính kích hoạt phải lớn hơn 0"
        )
        @Digits(
                integer = 6,
                fraction = 2,
                message = "Bán kính tối đa 6 chữ số nguyên và 2 chữ số thập phân"
        )
        BigDecimal triggerRadiusM
) {
}