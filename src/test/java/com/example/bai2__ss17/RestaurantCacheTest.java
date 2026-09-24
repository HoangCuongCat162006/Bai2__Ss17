package com.example.bai2__ss17;

import com.example.bai2__ss17.controller.RestaurantController;
import com.example.bai2__ss17.model.MenuItem;
import com.example.bai2__ss17.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RestaurantCacheTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RestaurantController restaurantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantController).build();
    }

    @Test
    @DisplayName("1. Kiểm tra Cache Miss (lần 1 tốn >= 3s) và Cache Hit (lần 2 < 20ms) qua RestaurantService")
    void testServiceCacheHitAndMiss() {
        Long restaurantId = 101L;

        // Xóa cache trước khi chạy kiểm thử
        Cache cache = cacheManager.getCache("restaurantMenu");
        if (cache != null) {
            cache.evict(restaurantId);
        }

        // Lần 1: Cache Miss - Giả lập trễ DB 3s
        long start1 = System.currentTimeMillis();
        List<MenuItem> menuCall1 = restaurantService.getMenuByRestaurantId(restaurantId);
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(menuCall1).isNotEmpty();
        assertThat(menuCall1).hasSize(2);
        assertThat(duration1).isGreaterThanOrEqualTo(3000L);

        // Kiểm tra cache đã lưu đúng key = 101L
        cache = cacheManager.getCache("restaurantMenu");
        assertThat(cache).isNotNull();
        Cache.ValueWrapper cachedValue = cache.get(restaurantId);
        assertThat(cachedValue).isNotNull();
        @SuppressWarnings("unchecked")
        List<MenuItem> cachedList = (List<MenuItem>) cachedValue.get();
        assertThat(cachedList).isEqualTo(menuCall1);

        // Lần 2: Cache Hit - Trả về ngay từ Cache, không qua DB (< 20ms)
        long start2 = System.currentTimeMillis();
        List<MenuItem> menuCall2 = restaurantService.getMenuByRestaurantId(restaurantId);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(menuCall2).isEqualTo(menuCall1);
        assertThat(duration2).isLessThan(20L);
    }

    @Test
    @DisplayName("2. Kiểm tra Cache động theo key = #id (các ID khác nhau cache riêng)")
    void testDynamicKeyCaching() {
        Long id101 = 101L;
        Long id999 = 999L;

        Cache cache = cacheManager.getCache("restaurantMenu");
        if (cache != null) {
            cache.evict(id101);
            cache.evict(id999);
        }

        // Gọi id 101
        restaurantService.getMenuByRestaurantId(id101);

        // Kiểm tra id 101 đã cache nhưng id 999 chưa cache
        assertThat(cache.get(id101)).isNotNull();
        assertThat(cache.get(id999)).isNull();
    }

    @Test
    @DisplayName("3. Kiểm tra API GET /restaurants/{restaurantId}/menu qua Controller (Lần 1 chậm, lần 2 < 20ms)")
    void testApiControllerPerformance() {
        Long restaurantId = 101L;

        // Xóa cache trước khi gọi API
        Cache cache = cacheManager.getCache("restaurantMenu");
        if (cache != null) {
            cache.evict(restaurantId);
        }

        // Lần 1 qua Controller: Cache Miss (~ 3000ms)
        long start1 = System.currentTimeMillis();
        ResponseEntity<List<MenuItem>> response1 = restaurantController.getMenu(restaurantId);
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(response1.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response1.getBody()).hasSize(2);
        assertThat(duration1).isGreaterThanOrEqualTo(3000L);

        // Lần 2 qua Controller: Cache Hit (< 20ms)
        long start2 = System.currentTimeMillis();
        ResponseEntity<List<MenuItem>> response2 = restaurantController.getMenu(restaurantId);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(response2.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response2.getBody()).isEqualTo(response1.getBody());
        assertThat(duration2).isLessThan(20L);
    }

    @Test
    @DisplayName("4. Kiểm tra định dạng JSON API trả về qua MockMvc")
    void testMockMvcEndpoint() throws Exception {
        Long restaurantId = 101L;

        mockMvc.perform(get("/restaurants/" + restaurantId + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dishName").value("Phở Bò"))
                .andExpect(jsonPath("$[0].price").value(55000.0))
                .andExpect(jsonPath("$[1].dishName").value("Bún Chả"))
                .andExpect(jsonPath("$[1].price").value(60000.0));
    }
}
