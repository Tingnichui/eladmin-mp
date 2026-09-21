/*
*  Copyright 2019-2025 Zheng Jie
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package me.zhengjie.invest.service;

import me.zhengjie.invest.domain.BinanceC2cOrder;
import me.zhengjie.invest.domain.dto.BinanceC2cOrderQueryCriteria;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.utils.PageResult;

/**
* @description 服务接口
* @author genghui
* @date 2026-09-21
**/
public interface BinanceC2cOrderService extends IService<BinanceC2cOrder> {

    /**
    * 从币安同步指定账户的 C2C 历史订单
    * @param uid 币安账户用户编号
    * @return 本次接收并写入的订单数量
    */
    int sync(Integer uid);

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param page 分页参数
    * @return PageResult
    */
    PageResult<BinanceC2cOrder> queryAll(BinanceC2cOrderQueryCriteria criteria, Page<Object> page);

    /**
    * 创建
    * @param resources /
    */
    void create(BinanceC2cOrder resources);

    /**
    * 编辑
    * @param resources /
    */
    void update(BinanceC2cOrder resources);

    /**
    * 多选删除
    * @param ids /
    */
    void deleteAll(List<Long> ids);

    /**
    * 导出数据
    * @param criteria 条件参数
    * @param response /
    * @throws IOException /
    */
    void download(BinanceC2cOrderQueryCriteria criteria, HttpServletResponse response) throws IOException;
}
