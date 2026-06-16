package com.seckill.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.mq.OrderProducer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderProducer orderProducer;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String LOCK_KEY_PREFIX = "seckill:lock:";
    private static final String ORDERED_USERS_KEY_PREFIX = "seckill:ordered:";

    private static final DefaultRedisScript<Long> DEDUCT_STOCK_SCRIPT;

    static {
        DEDUCT_STOCK_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_STOCK_SCRIPT.setLocation(new org.springframework.core.io.ClassPathResource("lua/deduct_stock.lua"));
        DEDUCT_STOCK_SCRIPT.setResultType(Long.class);
    }

    public SeckillService(StringRedisTemplate stringRedisTemplate,
                          RedissonClient redissonClient,
                          ProductMapper productMapper,
                          SeckillOrderMapper seckillOrderMapper,
                          OrderProducer orderProducer) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.productMapper = productMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.orderProducer = orderProducer;
    }

    public String seckill(Long productId, Long userId) {
        String userOrderedKey = ORDERED_USERS_KEY_PREFIX + productId;
        Boolean alreadyOrdered = stringRedisTemplate.opsForSet().isMember(userOrderedKey, userId.toString());
        if (Boolean.TRUE.equals(alreadyOrdered)) {
            return "您已经参与过该商品的秒杀，请勿重复下单";
        }

        String lockKey = LOCK_KEY_PREFIX + productId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                return "系统繁忙，请稍后再试";
            }

            try {
                String stockKey = STOCK_KEY_PREFIX + productId;
                Long result = stringRedisTemplate.execute(
                        DEDUCT_STOCK_SCRIPT,
                        Collections.singletonList(stockKey),
                        "1"
                );

                if (result != null && result == 1) {
                    stringRedisTemplate.opsForSet().add(userOrderedKey, userId.toString());
                    orderProducer.sendOrderMessage(productId, userId, 1);
                    log.info("秒杀成功: productId={}, userId={}", productId, userId);
                    return "秒杀成功，订单处理中";
                } else {
                    return "库存不足，秒杀失败";
                }
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "系统异常，请稍后再试";
        }
    }

    public Integer getRedisStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        String stock = stringRedisTemplate.opsForValue().get(stockKey);
        return stock != null ? Integer.parseInt(stock) : 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrderFromMQ(Long productId, Long userId, Integer quantity) {
        int updated = productMapper.update(
                null,
                new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, productId)
                        .ge(Product::getStock, quantity)
                        .setSql("stock = stock - " + quantity)
        );
        if (updated == 0) {
            log.warn("扣减库存失败，库存不足: productId={}", productId);
            return;
        }

        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setUserId(userId);
        order.setQuantity(quantity);
        order.setStatus(0);
        seckillOrderMapper.insert(order);

        log.info("订单创建成功: orderId={}, productId={}, userId={}", order.getId(), productId, userId);
    }
}
