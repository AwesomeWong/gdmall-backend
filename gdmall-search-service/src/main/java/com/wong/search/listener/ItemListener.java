package com.wong.search.listener;

import cn.hutool.json.JSONUtil;
import com.wong.api.client.ItemClient;
import com.wong.api.dto.ItemDTO;
import com.wong.common.utils.BeanUtils;
import com.wong.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItemListener {

    @Autowired
    private  ItemClient itemCient;

    @Resource
    private  RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://192.168.200.130:9200")
    ));

    public void common(Long id) throws IOException{
        ItemDTO itemDTO = itemCient.queryItemById(id);
        ItemDoc itemDoc = BeanUtils.copyBean(itemDTO, ItemDoc.class);
        itemDoc.setUpdateTime(LocalDateTime.now());

        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.index.queue", durable = "true"),
            exchange = @Exchange(value = "search.direct"),
            key = {"item.index"}
    ))
    public void listenItemIndex(Long id) throws IOException{
        log.info("监听到新增商品信息，商品id：{}",id);
        common(id);
        log.info("新增商品id为：{}的商品成功",id);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.delete.queue", durable = "true"),
            exchange = @Exchange(value = "search.direct"),
            key = {"item.delete"}
    ))
    public void listenItemDelete(Long id) throws IOException {
        log.info("监听到删除商品信息，商品id：{}",id);
        DeleteRequest request = new DeleteRequest("items", id.toString());
        client.delete(request, RequestOptions.DEFAULT);
        log.info("删除商品id为：{}的商品成功",id);
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.update.queue", durable = "true"),
            exchange = @Exchange(value = "search.direct"),
            key = {"item.update"}
    ))
    public void listenItemUpdate(Long id) throws IOException {
        log.info("监听到修改商品信息，商品id：{}",id);
        common(id);
        log.info("修改商品id为：{}的商品成功",id);
    }
}
