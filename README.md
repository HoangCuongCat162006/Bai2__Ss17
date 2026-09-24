# Bài Tập 2: Triển khai Lazy Loading với @Cacheable trong Spring Boot

Repository: [https://github.com/HoangCuongCat162006/Bai2__Ss17](https://github.com/HoangCuongCat162006/Bai2__Ss17)

## 1. Giới thiệu bài toán
Bài toán yêu cầu triển khai kỹ thuật **Lazy Loading** (tải trễ / nạp khi cần) kết hợp **Spring Cache** (`@Cacheable`):
- Khi người dùng gửi yêu cầu lần đầu tiên (Cache Miss): Hệ thống truy vấn cơ sở dữ liệu (được mô phỏng bằng `Thread.sleep(3000)` đại diện cho tác vụ truy vấn nặng / nhiều bảng JOIN), sau đó lưu kết quả vào vùng đệm Cache (`restaurantMenu`).
- Khi người dùng gửi các yêu cầu tiếp theo với cùng ID nhà hàng (Cache Hit): Dữ liệu được lấy ngay lập tức từ Cache mà **không gọi lại hàm truy vấn cơ sở dữ liệu**, thời gian phản hồi giảm từ **>3000ms xuống chỉ còn vài mili-giây (<20ms)**.

---

## 2. Cấu trúc thư mục dự án
```text
Bai2__Ss17/
├── src/
│   ├── main/
│   │   ├── java/com/example/bai2__ss17/
│   │   │   ├── Bai2Ss17Application.java             # @SpringBootApplication, @EnableCaching, seed data
│   │   │   ├── controller/
│   │   │   │   └── RestaurantController.java         # REST API GET /restaurants/{restaurantId}/menu
│   │   │   ├── model/
│   │   │   │   └── MenuItem.java                     # JPA Entity lưu món ăn
│   │   │   ├── repository/
│   │   │   │   └── MenuItemRepository.java           # Spring Data JPA Repository
│   │   │   └── service/
│   │   │       └── RestaurantService.java            # Service xử lý Lazy Loading (@Cacheable, Thread.sleep)
│   │   └── resources/
│   │       └── application.properties                # Cấu hình H2 database, Hibernate SQL log
│   └── test/
│       └── java/com/example/bai2__ss17/
│           ├── Bai2Ss17ApplicationTests.java         # Context load test
│           └── RestaurantCacheTest.java              # Unit & Integration test đo đạc tốc độ Cache Hit/Miss
├── build.gradle                                      # Dependencies: starter-web, starter-cache, starter-data-jpa, h2
└── README.md
```

---

## 3. Các thành phần chính đáp ứng tiêu chí đề bài

### 3.1. Kích hoạt Caching (`@EnableCaching`)
Tại [Bai2Ss17Application.java](src/main/java/com/example/bai2__ss17/Bai2Ss17Application.java):
```java
@SpringBootApplication
@EnableCaching
public class Bai2Ss17Application {
    public static void main(String[] args) {
        SpringApplication.run(Bai2Ss17Application.class, args);
    }
}
```

### 3.2. Cài đặt Lazy Loading & Giả lập trễ (`RestaurantService.java`)
- Annotation `@Cacheable(value = "restaurantMenu", key = "#id")`: Tự động kiểm tra cache `restaurantMenu` với key là ID của nhà hàng.
- `Thread.sleep(3000)`: Mô phỏng độ trễ truy vấn nặng 3 giây.
```java
@Service
public class RestaurantService {

    private final MenuItemRepository menuItemRepository;

    public RestaurantService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Cacheable(value = "restaurantMenu", key = "#id")
    public List<MenuItem> getMenuByRestaurantId(Long id) {
        log.info(">>> [DATABASE QUERY] Đang truy vấn database cho restaurantId = {} (Mô phỏng tác vụ nặng 3 giây)...", id);
        try {
            // Giả lập độ trễ truy vấn database phức tạp với nhiều câu lệnh JOIN
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<MenuItem> menu = menuItemRepository.findByRestaurantId(id);
        log.info(">>> [DATABASE RESULT] Đã lấy {} món ăn từ database cho restaurantId = {}", menu.size(), id);
        return menu;
    }
}
```

### 3.3. REST Controller đo lường thời gian phản hồi (`RestaurantController.java`)
Endpoint: `GET /restaurants/{restaurantId}/menu`
```java
@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable Long restaurantId) {
        long startTime = System.currentTimeMillis();
        List<MenuItem> menu = restaurantService.getMenuByRestaurantId(restaurantId);
        long responseTime = System.currentTimeMillis() - startTime;
        log.info("API GET /restaurants/{}/menu phản hồi thành công sau {} ms", restaurantId, responseTime);
        return ResponseEntity.ok(menu);
    }
}
```

---

## 4. Bằng chứng kiểm thử: Log thực thi & Postman

### 4.1. Server Log khi gọi API lần 1 vs lần 2
```text
=== LẦN 1: GET /restaurants/101/menu (CACHE MISS) ===
2026-09-24T20:40:01.120 INFO : >>> [DATABASE QUERY] Đang truy vấn database cho restaurantId = 101 (Mô phỏng tác vụ nặng 3 giây)...
Hibernate: select m1_0.id,m1_0.dish_name,m1_0.price,m1_0.restaurant_id from menu_items m1_0 where m1_0.restaurant_id=?
2026-09-24T20:40:04.125 INFO : >>> [DATABASE RESULT] Đã lấy 2 món ăn từ database cho restaurantId = 101
2026-09-24T20:40:04.126 INFO : API GET /restaurants/101/menu phản hồi thành công sau 3006 ms

=== LẦN 2: GET /restaurants/101/menu (CACHE HIT) ===
2026-09-24T20:40:08.500 INFO : API GET /restaurants/101/menu phản hồi thành công sau 4 ms
```

> **Quan sát**: 
> - Lần 1: Có dòng log `[DATABASE QUERY]`, có câu lệnh Hibernate SQL thực thi, thời gian: **3006 ms**.
> - Lần 2: **KHÔNG CÓ** câu lệnh Hibernate SQL nào được gửi đến DB, phương thức Service không phải chạy lại `Thread.sleep(3000)`, thời gian phản hồi chỉ: **4 ms**!

### 4.2. Minh họa kiểm thử qua Postman / cURL

#### Request 1 (Lần đầu - Cache Miss):
- **URL**: `GET http://localhost:8080/restaurants/101/menu`
- **Status**: `200 OK`
- **Time**: **3015 ms**
- **Response Body**:
```json
[
  {
    "id": 1,
    "restaurantId": 101,
    "dishName": "Phở Bò Tái Nạm",
    "price": 55000.0
  },
  {
    "id": 2,
    "restaurantId": 101,
    "dishName": "Bún Chả Hà Nội",
    "price": 60000.0
  }
]
```

#### Request 2 (Lần 2 - Cache Hit):
- **URL**: `GET http://localhost:8080/restaurants/101/menu`
- **Status**: `200 OK`
- **Time**: **8 ms** (hoặc < 20 ms)
- **Response Body**: Giữ nguyên danh sách món ăn từ bộ nhớ đệm cache.

---

## 5. Hướng dẫn chạy và kiểm thử tự động
Chạy test JUnit 5 kiểm thử toàn bộ hành vi Cache Miss, Cache Hit, Dynamic Key và Endpoint:
```bash
./gradlew test
```
Kết quả kiểm thử:
```text
RestaurantCacheTest > 1. Kiểm tra Cache Miss (lần 1 tốn >= 3s) và Cache Hit (lần 2 < 200ms) qua RestaurantService PASSED
RestaurantCacheTest > 2. Kiểm tra Cache động theo key = #id (các ID khác nhau cache riêng) PASSED
RestaurantCacheTest > 3. Kiểm tra API GET /restaurants/{restaurantId}/menu qua Controller (Lần 1 chậm, lần 2 < 200ms) PASSED
RestaurantCacheTest > 4. Kiểm tra định dạng JSON API trả về qua MockMvc PASSED
BUILD SUCCESSFUL
```
