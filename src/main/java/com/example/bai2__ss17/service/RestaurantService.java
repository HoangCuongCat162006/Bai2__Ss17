package com.example.bai2__ss17.service;

import com.example.bai2__ss17.model.MenuItem;
import com.example.bai2__ss17.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final MenuItemRepository menuItemRepository;

    public RestaurantService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    /**
     * Lấy danh sách thực đơn theo ID nhà hàng.
     * Sử dụng @Cacheable để triển khai Lazy Loading (truy vấn DB 1 lần, các lần sau lấy từ cache).
     *
     * @param id ID của nhà hàng
     * @return Danh sách món ăn của nhà hàng
     */
    @Cacheable(value = "restaurantMenu", key = "#id")
    public List<MenuItem> getMenuByRestaurantId(Long id) {
        log.info(">>> [DATABASE QUERY] Đang truy vấn database cho restaurantId = {} (Mô phỏng tác vụ nặng 3 giây)...", id);
        try {
            // Giả lập độ trễ truy vấn database phức tạp với nhiều câu lệnh JOIN
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread sleep bị gián đoạn", e);
        }

        List<MenuItem> menu = menuItemRepository.findByRestaurantId(id);
        log.info(">>> [DATABASE RESULT] Đã lấy {} món ăn từ database cho restaurantId = {}", menu.size(), id);
        return menu;
    }
}
