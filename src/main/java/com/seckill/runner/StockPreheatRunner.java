package com.seckill.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class StockPreheatRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockPreheatRunner.class);

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    public StockPreheatRunner(ProductMapper productMapper, StringRedisTemplate stringRedisTemplate) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("开始预热商品库存到Redis...");

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<>());
        for (Product product : products) {
            String key = STOCK_KEY_PREFIX + product.getId();
            stringRedisTemplate.opsForValue().set(key, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);
            log.info("商品库存预热: productId={}, stock={}", product.getId(), product.getStock());
        }

        log.info("库存预热完成，共加载{}个商品", products.size());
    }
}
