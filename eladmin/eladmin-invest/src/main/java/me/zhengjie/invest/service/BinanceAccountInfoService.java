/*
*  Copyright 2019-2023 Zheng Jie
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

import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.vo.BinanceAccountInfoQueryCriteria;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.utils.PageResult;

/**
* @description 服务接口
* @author genghui
* @date 2025-08-02
**/
public interface BinanceAccountInfoService extends IService<BinanceAccountInfo> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param page 分页参数
    * @return PageResult
    */
    PageResult<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria, Page<Object> page);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<BinanceAccountInfoDto>
    */
    List<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria);

    /**
    * 创建
    * @param resources /
    */
    void create(BinanceAccountInfo resources);

    /**
    * 编辑
    * @param resources /
    */
    void update(BinanceAccountInfo resources);

    /**
    * 多选删除
    * @param ids /
    */
    void deleteAll(List<Integer> ids);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<BinanceAccountInfo> all, HttpServletResponse response) throws IOException;

    List<BinanceAccountInfo> listUseApiAccount();

    List<BinanceAccountInfo> listAutoTradeAccount();

    void changeAutoTradeFlag(Integer id);

}