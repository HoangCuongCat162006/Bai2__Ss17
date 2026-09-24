package com.example.bai2__ss17.controller;

import com.example.bai2__ss17.model.MenuItem;
import com.example.bai2__ss17.service.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * API lấy danh sách thực đơn theo ID nhà hàng.
     * GET /restaurants/{restaurantId}/menu
     *
     * @param restaurantId ID của nhà hàng
     * @return Danh sách món ăn
     */
    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable Long restaurantId) {
        long startTime = System.currentTimeMillis();
        List<MenuItem> menu = restaurantService.getMenuByRestaurantId(restaurantId);
        long responseTime = System.currentTimeMillis() - startTime;
        log.info("API GET /restaurants/{}/menu phản hồi thành công sau {} ms", restaurantId, responseTime);
        return ResponseEntity.ok(menu);
    }
}
