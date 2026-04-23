package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    public void processProduct(Product product) {
        switch (product.getType()) {
            case NORMAL -> processNormalProduct(product);
            case SEASONAL -> processSeasonalProduct(product);
            case EXPIRABLE -> processExpirableProduct(product);
        }
    }

    private void processNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementStock(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product);
        }
    }

    private void processSeasonalProduct(Product product) {
        LocalDate now = LocalDate.now();

        if (isInSeason(product, now) && product.getAvailable() > 0) {
            decrementStock(product);
        } else if (isResupplyAfterSeasonEnd(product, now)) {
            markOutOfStock(product);
            notificationService.sendOutOfStockNotification(product.getName());
        } else if (isBeforeSeasonStart(product, now)) {
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            notifyDelay(product);
        }
    }

    private void processExpirableProduct(Product product) {
        LocalDate now = LocalDate.now();

        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(now)) {
            decrementStock(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            markOutOfStock(product);
        }
    }

    private boolean isInSeason(Product product, LocalDate now) {
        return now.isAfter(product.getSeasonStartDate()) && now.isBefore(product.getSeasonEndDate());
    }

    private boolean isResupplyAfterSeasonEnd(Product product, LocalDate now) {
        return now.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate());
    }

    private boolean isBeforeSeasonStart(Product product, LocalDate now) {
        return product.getSeasonStartDate().isAfter(now);
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }

    private void markOutOfStock(Product product) {
        product.setAvailable(0);
        productRepository.save(product);
    }

    private void notifyDelay(Product product) {
        product.setLeadTime(product.getLeadTime());
        productRepository.save(product);
        notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
    }
}
