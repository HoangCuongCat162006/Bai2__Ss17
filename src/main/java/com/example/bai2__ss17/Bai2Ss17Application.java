package com.example.bai2__ss17;

import com.example.bai2__ss17.model.MenuItem;
import com.example.bai2__ss17.repository.MenuItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class Bai2Ss17Application {

    public static void main(String[] args) {
        SpringApplication.run(Bai2Ss17Application.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(MenuItemRepository menuItemRepository) {
        return args -> {
            if (menuItemRepository.count() == 0) {
                menuItemRepository.save(new MenuItem(101L, "Phở Bò Tái Nạm", 55000.0));
                menuItemRepository.save(new MenuItem(101L, "Bún Chả Hà Nội", 60000.0));
                menuItemRepository.save(new MenuItem(102L, "Cơm Tấm Sườn Bì", 50000.0));
            }
        };
    }
}
