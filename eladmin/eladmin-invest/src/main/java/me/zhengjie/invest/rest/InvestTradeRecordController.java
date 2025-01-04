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
import me.zhengjie.invest.domain.InvestTradeRecord;
import me.zhengjie.invest.service.InvestTradeRecordService;
import me.zhengjie.invest.domain.vo.InvestTradeRecordQueryCriteria;
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
* @date 2025-01-04
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "投资记录管理")
@RequestMapping("/api/investTradeRecord")
public class InvestTradeRecordController {

    private final InvestTradeRecordService investTradeRecordService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('investTradeRecord:list')")
    public void exportInvestTradeRecord(HttpServletResponse response, InvestTradeRecordQueryCriteria criteria) throws IOException {
        investTradeRecordService.download(investTradeRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询投资记录")
    @ApiOperation("查询投资记录")
    @PreAuthorize("@el.check('investTradeRecord:list')")
    public ResponseEntity<PageResult<InvestTradeRecord>> queryInvestTradeRecord(InvestTradeRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(investTradeRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增投资记录")
    @ApiOperation("新增投资记录")
    @PreAuthorize("@el.check('investTradeRecord:add')")
    public ResponseEntity<Object> createInvestTradeRecord(@Validated @RequestBody InvestTradeRecord resources){
        investTradeRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改投资记录")
    @ApiOperation("修改投资记录")
    @PreAuthorize("@el.check('investTradeRecord:edit')")
    public ResponseEntity<Object> updateInvestTradeRecord(@Validated @RequestBody InvestTradeRecord resources){
        investTradeRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除投资记录")
    @ApiOperation("删除投资记录")
    @PreAuthorize("@el.check('investTradeRecord:del')")
    public ResponseEntity<Object> deleteInvestTradeRecord(@RequestBody List<Integer> ids) {
        investTradeRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}