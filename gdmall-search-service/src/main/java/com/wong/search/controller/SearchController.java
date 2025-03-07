package com.wong.search.controller;

import com.wong.common.domain.PageDTO;
import com.wong.search.domain.po.ItemDoc;
import com.wong.search.domain.query.ItemPageQuery;
import com.wong.search.domain.vo.CategoryAndBrandVo;
import com.wong.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDoc> search(ItemPageQuery query) throws IOException {
        // 分页查询
//        Page<Item> result = itemService.lambdaQuery()
//                .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
//                .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
//                .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
//                .eq(Item::getStatus, 1)
//                .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
//                .page(query.toMpPage("update_time", false));
        // 封装并返回
        return searchService.esSearch(query);
    }

    @ApiOperation("分类聚合接口")
    @PostMapping("/filters")
    public CategoryAndBrandVo getFilters(@RequestBody ItemPageQuery query) {
        return searchService.getFilters(query);
    }
}
