package com.wong.api.client.fallback;

import com.wong.api.client.PayClient;
import com.wong.api.dto.PayOrderDTO;
import com.wong.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class PayClientFallback implements FallbackFactory<PayClient> {
    @Override
    public PayClient create(Throwable cause) {
        return new PayClient() {
            @Override
            public PayOrderDTO queryPayOrderByBizOrderNo(Long id) {
                return null;
            }

            @Override
            public void updatePayOrderStatusByOrderId(Long orderId, Integer status) {
                log.error("更新支付单状态失败",cause);
                throw new BizIllegalException(cause);
            }


        };
    }
}