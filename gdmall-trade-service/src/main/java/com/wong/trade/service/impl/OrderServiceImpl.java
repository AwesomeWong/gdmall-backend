package com.wong.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wong.api.client.CartClient;
import com.wong.api.client.ItemClient;
import com.wong.api.client.PayClient;
import com.wong.api.dto.ItemDTO;
import com.wong.api.dto.OrderDetailDTO;
import com.wong.api.dto.OrderFormDTO;
import com.wong.common.exception.BadRequestException;
import com.wong.common.utils.BeanUtils;
import com.wong.common.utils.UserContext;
import com.wong.trade.constants.MQConstants;
import com.wong.trade.domain.po.Order;
import com.wong.trade.domain.po.OrderDetail;
import com.wong.trade.mapper.OrderMapper;
import com.wong.trade.service.IOrderDetailService;
import com.wong.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.扣减库存
//        try {
//            itemClient.deductStock(detailDTOS);
//        } catch (Exception e) {
//            throw new RuntimeException("库存不足！");
//        }
        try {
            rabbitTemplate.convertAndSend("trade.topic","order.create",itemIds, message -> {
                message.getMessageProperties().setHeader("user-info", UserContext.getUser());
                return message;
            });
        }catch (Exception e){
            log.error("清空购物车的消息发送失败，商品id：{}", itemIds, e);
        }

        // 5.发送延迟消息，检测订单支付状态
        rabbitTemplate.convertAndSend(
                MQConstants.DELAY_EXCHANGE_NAME,
                MQConstants.DELAY_ORDER_KEY,
                order.getId(),
                message -> {
                    // 延迟消息的时间是15分钟，测试方便改成10秒
                    message.getMessageProperties().setDelay(10000);
                    return message;
                }
        );
        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
    }

    @Override
    public void cancelOrder(Long orderId) {
        lambdaUpdate()
                .set(Order::getStatus,5)
                .eq(Order::getId, orderId)
                .update();
        payClient.updatePayOrderStatusByOrderId(orderId,2);
        List<OrderDetail> list = detailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        List<OrderDetailDTO> orderDetailDTOS = BeanUtils.copyToList(list, OrderDetailDTO.class);
        itemClient.restoreStock(orderDetailDTOS);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}