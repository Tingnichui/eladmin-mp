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
import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.invest.service.BinanceSpotCorePositionService;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionAdjustRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionCandidate;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionLockRequest;
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
@Api(tags = "现货底仓")
@RequestMapping("/api/binanceSpotCorePosition")
public class BinanceSpotCorePositionController {

    private final BinanceSpotCorePositionService binanceSpotCorePositionService;

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceSpotCorePosition:list')")
    public void exportBinanceSpotCorePosition(HttpServletResponse response, BinanceSpotCorePositionQueryCriteria criteria) throws IOException {
        binanceSpotCorePositionService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:list')")
    public ResponseEntity<PageResult<BinanceSpotCorePosition>> queryBinanceSpotCorePosition(BinanceSpotCorePositionQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(binanceSpotCorePositionService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增现货底仓")
    @ApiOperation("新增现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:add')")
    public ResponseEntity<BinanceSpotCorePosition> createBinanceSpotCorePosition(
            @Validated @RequestBody BinanceSpotCorePositionLockRequest resources){
        return new ResponseEntity<>(binanceSpotCorePositionService.lock(resources), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Log("修改现货底仓")
    @ApiOperation("修改现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:edit')")
    public ResponseEntity<BinanceSpotCorePosition> updateBinanceSpotCorePosition(
            @PathVariable Long id,
            @Validated @RequestBody BinanceSpotCorePositionAdjustRequest resources){
        return ResponseEntity.ok(binanceSpotCorePositionService.adjust(id, resources));
    }

    @PutMapping("/{id}/release")
    @Log("解除现货底仓")
    @ApiOperation("解除现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:edit')")
    public ResponseEntity<BinanceSpotCorePosition> releaseBinanceSpotCorePosition(@PathVariable Long id) {
        return ResponseEntity.ok(binanceSpotCorePositionService.release(id));
    }

    @PutMapping("/batch-release")
    @Log("批量解除现货底仓")
    @ApiOperation("批量解除现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:edit')")
    public ResponseEntity<List<BinanceSpotCorePosition>> releaseBinanceSpotCorePositions(
            @RequestBody List<Long> ids) {
        return ResponseEntity.ok(binanceSpotCorePositionService.releaseAll(ids));
    }

    @GetMapping("/candidates")
    @ApiOperation("查询可设置底仓的现货持仓批次")
    @PreAuthorize("@el.check('binanceSpotCorePosition:list')")
    public ResponseEntity<List<BinanceSpotCorePositionCandidate>> candidates(@RequestParam Integer uid,
                                                                              @RequestParam String symbol) {
        return ResponseEntity.ok(binanceSpotCorePositionService.listCandidates(uid, symbol));
    }

    @DeleteMapping
    @Log("删除现货底仓")
    @ApiOperation("删除现货底仓")
    @PreAuthorize("@el.check('binanceSpotCorePosition:del')")
    public ResponseEntity<Object> deleteBinanceSpotCorePosition(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        binanceSpotCorePositionService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
