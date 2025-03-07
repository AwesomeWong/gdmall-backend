package com.wong.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wong.common.domain.PageDTO;
import com.wong.search.domain.po.Item;
import com.wong.search.domain.po.ItemDoc;
import com.wong.search.domain.query.ItemPageQuery;
import com.wong.search.domain.vo.CategoryAndBrandVo;

import java.io.IOException;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface ISearchService extends IService<Item> {

    PageDTO<ItemDoc> esSearch(ItemPageQuery query) throws IOException;

    CategoryAndBrandVo getFilters(ItemPageQuery query);
}
