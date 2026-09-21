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
package me.zhengjie.invest.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.BinanceC2cOrder;
import me.zhengjie.invest.service.BinanceC2cOrderService;
import me.zhengjie.invest.domain.dto.BinanceC2cOrderQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.utils.PageResult;
import java.util.Collections;

/**
* @author genghui
* @date 2026-09-21
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安C2C订单")
@RequestMapping("/api/binanceC2cOrder")
public class BinanceC2cOrderController {

    private final BinanceC2cOrderService binanceC2cOrderService;

    @PutMapping("/sync")
    @Log("同步币安C2C订单")
    @ApiOperation("同步币安C2C订单")
    @PreAuthorize("@el.check('binanceC2cOrder:sync')")
    public ResponseEntity<Object> syncBinanceC2cOrder(@RequestParam Integer uid) {
        int count = binanceC2cOrderService.sync(uid);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceC2cOrder:list')")
    public void exportBinanceC2cOrder(HttpServletResponse response, BinanceC2cOrderQueryCriteria criteria) throws IOException {
        binanceC2cOrderService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询币安C2C订单")
    @PreAuthorize("@el.check('binanceC2cOrder:list')")
    public ResponseEntity<PageResult<BinanceC2cOrder>> queryBinanceC2cOrder(BinanceC2cOrderQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(binanceC2cOrderService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安C2C订单")
    @ApiOperation("新增币安C2C订单")
    @PreAuthorize("@el.check('binanceC2cOrder:add')")
    public ResponseEntity<Object> createBinanceC2cOrder(@Validated @RequestBody BinanceC2cOrder resources){
        binanceC2cOrderService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安C2C订单")
    @ApiOperation("修改币安C2C订单")
    @PreAuthorize("@el.check('binanceC2cOrder:edit')")
    public ResponseEntity<Object> updateBinanceC2cOrder(@Validated @RequestBody BinanceC2cOrder resources){
        binanceC2cOrderService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安C2C订单")
    @ApiOperation("删除币安C2C订单")
    @PreAuthorize("@el.check('binanceC2cOrder:del')")
    public ResponseEntity<Object> deleteBinanceC2cOrder(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        binanceC2cOrderService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
