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
import me.zhengjie.invest.domain.InvestProduct;
import me.zhengjie.invest.service.InvestProductService;
import me.zhengjie.invest.domain.dto.InvestProductQueryCriteria;
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
@Api(tags = "投资产品管理")
@RequestMapping("/api/investProduct")
public class InvestProductController {

    private final InvestProductService investProductService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('investProduct:list')")
    public void exportInvestProduct(HttpServletResponse response, InvestProductQueryCriteria criteria) throws IOException {
        investProductService.download(investProductService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询投资产品")
    @ApiOperation("查询投资产品")
    @PreAuthorize("@el.check('investProduct:list')")
    public ResponseEntity<PageResult<InvestProduct>> queryInvestProduct(InvestProductQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(investProductService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增投资产品")
    @ApiOperation("新增投资产品")
    @PreAuthorize("@el.check('investProduct:add')")
    public ResponseEntity<Object> createInvestProduct(@Validated @RequestBody InvestProduct resources){
        investProductService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改投资产品")
    @ApiOperation("修改投资产品")
    @PreAuthorize("@el.check('investProduct:edit')")
    public ResponseEntity<Object> updateInvestProduct(@Validated @RequestBody InvestProduct resources){
        investProductService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除投资产品")
    @ApiOperation("删除投资产品")
    @PreAuthorize("@el.check('investProduct:del')")
    public ResponseEntity<Object> deleteInvestProduct(@RequestBody List<Integer> ids) {
        investProductService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}