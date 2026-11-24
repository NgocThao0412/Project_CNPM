# Đồ án Công nghệ phần mềm JAVA – NHÓM 12

Ứng dụng thuyết minh đa ngôn ngữ theo vị trí GPS tại
Trường Đại học Sài Gòn tích hợp quy trình CI/CD.

## Thành viên nhóm

| STT | MSSV | Họ và tên |
| :-: | :--------: | -------------------------------------------------------------------------------------- |
| 1 | 3124411280 | [ LÊ THỊ NGỌC THẢO ](https://www.facebook.com/Ngthao.0412?locale=vi_VN)|
| 2 | 3124411208 | [ PHẠM THỊ BẢO NY ](https://www.facebook.com/tieeeeee1127?locale=vi_VN)|
| 3 | 3124411079 | [ NGUYỄN PHẠM ANH HÀO ](https://www.facebook.com/hao.nguyen.124845?locale=vi_VN)|


## Hướng dẫn cài đặt và chạy hệ thống

### 1. Yêu cầu môi trường

- Java JDK 21
- Apache Maven 3.9.x
- MySQL Server 8.x
- MySQL Workbench

### 2. Khởi tạo cơ sở dữ liệu

Mở MySQL Workbench, tạo database `sgu_audio_guide` và import file SQL trong thư mục `Database`.

Cấu hình tài khoản MySQL trong các file `application.properties` của backend và `config.properties` của Client.

### 3. Build dự án

Mở PowerShell tại thư mục gốc dự án và chạy:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd -f src/client/pom.xml clean package
```

### 4. Chạy Backend

Mở 6 cửa sổ Terminal riêng biệt tại thư mục gốc dự án, lần lượt chạy:

**Auth Service – Port 8081**
```powershell
java -jar src/backend/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
```

**POI Service – Port 8082**
```powershell
java -jar src/backend/poi-service/target/poi-service-0.0.1-SNAPSHOT.jar
```

**Narration Service – Port 8083**
```powershell
java -jar src/backend/narration-service/target/narration-server-1.0.0.jar
```

**Audio Service – Port 8084**
```powershell
java -jar src/backend/audio-service/target/audio-server-1.0.0.jar
```

**Visitor Service – Port 8085**
```powershell
java -jar src/backend/visitor-service/target/visitor-service-1.0.0.jar
```

**Route Service – Port 8086**
```powershell
java -jar src/backend/route-service/target/route-service-0.0.1-SNAPSHOT.jar
```

### 5. Chạy Client

Mở Terminal mới tại thư mục gốc dự án:

```powershell
java -jar src/client/target/Client.jar
```

Truy cập website:

**http://localhost:8080**

### Lưu ý

- Khởi động MySQL trước khi chạy các service.
- Đảm bảo các cổng 8080–8086 không bị chiếm dụng.
- Các thư mục `target` sẽ được Maven tạo lại khi build.
- Dừng ứng dụng bằng `Ctrl + C` tại các Terminal đang chạy.


