package com.seckill.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "seckill-order-topic";
    private static final String TAG = "order-create";

    public OrderProducer(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderMessage(Long productId, Long userId, Integer quantity) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("productId", productId);
            message.put("userId", userId);
            message.put("quantity", quantity);

            String json = objectMapper.writeValueAsString(message);
            rocketMQTemplate.asyncSend(TOPIC + ":" + TAG, json, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("订单消息发送成功: productId={}, userId={}", productId, userId);
                }
                @Override
                public void onException(Throwable e) {
                    log.error("订单消息发送失败: productId={}, userId={}, error={}", productId, userId, e.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("订单消息序列化失败", e);
        }
    }
}
