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
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.domain.vo.InvestKlinesRecordQueryCriteria;
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
* @date 2025-07-30
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "投资K线管理")
@RequestMapping("/api/investKlinesRecord")
public class InvestKlinesRecordController {

    private final InvestKlinesRecordService investKlinesRecordService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('investKlinesRecord:list')")
    public void exportInvestKlinesRecord(HttpServletResponse response, InvestKlinesRecordQueryCriteria criteria) throws IOException {
        investKlinesRecordService.download(investKlinesRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询投资K线")
    @ApiOperation("查询投资K线")
    @PreAuthorize("@el.check('investKlinesRecord:list')")
    public ResponseEntity<PageResult<InvestKlinesRecord>> queryInvestKlinesRecord(InvestKlinesRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(investKlinesRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增投资K线")
    @ApiOperation("新增投资K线")
    @PreAuthorize("@el.check('investKlinesRecord:add')")
    public ResponseEntity<Object> createInvestKlinesRecord(@Validated @RequestBody InvestKlinesRecord resources){
        investKlinesRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改投资K线")
    @ApiOperation("修改投资K线")
    @PreAuthorize("@el.check('investKlinesRecord:edit')")
    public ResponseEntity<Object> updateInvestKlinesRecord(@Validated @RequestBody InvestKlinesRecord resources){
        investKlinesRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除投资K线")
    @ApiOperation("删除投资K线")
    @PreAuthorize("@el.check('investKlinesRecord:del')")
    public ResponseEntity<Object> deleteInvestKlinesRecord(@RequestBody List<Long> ids) {
        investKlinesRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}