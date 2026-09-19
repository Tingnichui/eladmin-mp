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
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.service.BinanceSpotTradeMatchStateService;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchStateQueryCriteria;
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

/**
* @author genghui
* @date 2026-09-19
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "现货撮合状态")
@RequestMapping("/api/binanceSpotTradeMatchState")
public class BinanceSpotTradeMatchStateController {

    private final BinanceSpotTradeMatchStateService binanceSpotTradeMatchStateService;

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceSpotTradeMatchState:list')")
    public void exportBinanceSpotTradeMatchState(HttpServletResponse response, BinanceSpotTradeMatchStateQueryCriteria criteria) throws IOException {
        binanceSpotTradeMatchStateService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询现货撮合状态")
    @PreAuthorize("@el.check('binanceSpotTradeMatchState:list')")
    public ResponseEntity<PageResult<BinanceSpotTradeMatchState>> queryBinanceSpotTradeMatchState(BinanceSpotTradeMatchStateQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(binanceSpotTradeMatchStateService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增现货撮合状态")
    @ApiOperation("新增现货撮合状态")
    @PreAuthorize("@el.check('binanceSpotTradeMatchState:add')")
    public ResponseEntity<Object> createBinanceSpotTradeMatchState(@Validated @RequestBody BinanceSpotTradeMatchState resources){
        binanceSpotTradeMatchStateService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改现货撮合状态")
    @ApiOperation("修改现货撮合状态")
    @PreAuthorize("@el.check('binanceSpotTradeMatchState:edit')")
    public ResponseEntity<Object> updateBinanceSpotTradeMatchState(@Validated @RequestBody BinanceSpotTradeMatchState resources){
        binanceSpotTradeMatchStateService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除现货撮合状态")
    @ApiOperation("删除现货撮合状态")
    @PreAuthorize("@el.check('binanceSpotTradeMatchState:del')")
    public ResponseEntity<Object> deleteBinanceSpotTradeMatchState(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        binanceSpotTradeMatchStateService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
