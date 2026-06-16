package com.seckill.mq;

import com.seckill.service.SeckillService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "seckill-order-topic",
        selectorExpression = "order-create",
        consumerGroup = "${rocketmq-consumer.group}"
)
public class OrderConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final SeckillService seckillService;
    private final ObjectMapper objectMapper;

    public OrderConsumer(SeckillService seckillService, ObjectMapper objectMapper) {
        this.seckillService = seckillService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        log.info("收到订单消息: {}", message);
        try {
            JsonNode json = objectMapper.readTree(message);
            Long productId = json.get("productId").asLong();
            Long userId = json.get("userId").asLong();
            Integer quantity = json.get("quantity").asInt();

            seckillService.createOrderFromMQ(productId, userId, quantity);
        } catch (JsonProcessingException e) {
            log.error("订单消息解析失败", e);
        }
    }
}
