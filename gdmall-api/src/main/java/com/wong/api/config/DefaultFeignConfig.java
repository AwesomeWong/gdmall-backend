package com.wong.api.config;

import com.wong.api.client.fallback.ItemClientFallback;
import com.wong.api.client.fallback.PayClientFallback;
import com.wong.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
/**
 * @author shine
 * @version 1.0
 */


public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return (Logger.Level.FULL);
    }

    @Bean
    public ItemClientFallback itemClientFallback(){
        return new ItemClientFallback();
    }
    @Bean
    public PayClientFallback payClientFallback(){
        return new PayClientFallback();
    }
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 获取登录用户
                Long userId = UserContext.getUser();
                if(userId == null) {
                    // 如果为空则直接跳过
                    return;
                }
                // 如果不为空则放入请求头中，传递给下游微服务
                template.header("user-info", userId.toString());
            }
        };
    }
}
