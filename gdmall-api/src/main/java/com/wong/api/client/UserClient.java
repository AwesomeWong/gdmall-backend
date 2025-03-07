package com.wong.api.client;

import com.wong.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author shine
 * @version 1.0
 */

@FeignClient(value = "user-service",
    configuration = DefaultFeignConfig.class)
public interface UserClient {

    @PutMapping("/users/money/deduct")
    public void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);
}
