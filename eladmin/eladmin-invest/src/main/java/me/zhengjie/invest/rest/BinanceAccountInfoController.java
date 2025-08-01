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
package me.zhengjie.invest.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.domain.vo.BinanceAccountInfoQueryCriteria;
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
* @date 2025-08-02
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安账户管理")
@RequestMapping("/api/binanceAccountInfo")
public class BinanceAccountInfoController {

    private final BinanceAccountInfoService binanceAccountInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceAccountInfo:list')")
    public void exportBinanceAccountInfo(HttpServletResponse response, BinanceAccountInfoQueryCriteria criteria) throws IOException {
        binanceAccountInfoService.download(binanceAccountInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询币安账户")
    @ApiOperation("查询币安账户")
    @PreAuthorize("@el.check('binanceAccountInfo:list')")
    public ResponseEntity<PageResult<BinanceAccountInfo>> queryBinanceAccountInfo(BinanceAccountInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(binanceAccountInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安账户")
    @ApiOperation("新增币安账户")
    @PreAuthorize("@el.check('binanceAccountInfo:add')")
    public ResponseEntity<Object> createBinanceAccountInfo(@Validated @RequestBody BinanceAccountInfo resources){
        binanceAccountInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安账户")
    @ApiOperation("修改币安账户")
    @PreAuthorize("@el.check('binanceAccountInfo:edit')")
    public ResponseEntity<Object> updateBinanceAccountInfo(@Validated @RequestBody BinanceAccountInfo resources){
        binanceAccountInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安账户")
    @ApiOperation("删除币安账户")
    @PreAuthorize("@el.check('binanceAccountInfo:del')")
    public ResponseEntity<Object> deleteBinanceAccountInfo(@RequestBody List<Integer> ids) {
        binanceAccountInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}