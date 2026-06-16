package com.seckill.controller;

import com.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/{productId}")
    public String seckill(@PathVariable Long productId,
                          @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return seckillService.seckill(productId, userId);
    }

    @GetMapping("/stock/{productId}")
    public Integer getStock(@PathVariable Long productId) {
        return seckillService.getRedisStock(productId);
    }
}
