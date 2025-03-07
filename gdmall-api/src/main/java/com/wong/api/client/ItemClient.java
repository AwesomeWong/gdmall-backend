package com.wong.api.client;

import com.wong.api.client.fallback.ItemClientFallback;
import com.wong.api.config.DefaultFeignConfig;
import com.wong.api.dto.ItemDTO;
import com.wong.api.dto.OrderDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * @author shine
 * @version 1.0
 */

@FeignClient(value = "item-service",
    configuration = DefaultFeignConfig.class,
    fallbackFactory = ItemClientFallback.class)
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @GetMapping("/items/{id}")
    ItemDTO queryItemById(@PathVariable("id") Long id);

    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);

    @PutMapping("/items//stock/restore")
    void restoreStock( List<OrderDetailDTO> orderDetails);
}
