package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_CACHE_PREFIX = "product:";

    public ProductService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public IPage<Product> page(int current, int size) {
        return page(new Page<>(current, size));
    }

    public Product getByIdWithCache(Long id) {
        String key = PRODUCT_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return objectMapper.convertValue(cached, Product.class);
        }

        Product product = getById(id);
        if (product != null) {
            redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
        }
        return product;
    }
}
