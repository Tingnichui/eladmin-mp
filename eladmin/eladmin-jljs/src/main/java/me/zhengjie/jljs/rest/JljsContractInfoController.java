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
package me.zhengjie.jljs.rest;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.annotation.Log;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.jljs.domain.JljsContractInfo;
import me.zhengjie.jljs.domain.JljsMemberInfo;
import me.zhengjie.jljs.service.JljsContractInfoService;
import me.zhengjie.jljs.domain.vo.JljsContractInfoQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;

import me.zhengjie.jljs.service.JljsMemberInfoService;
import me.zhengjie.utils.SecurityUtils;
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
* @date 2024-06-30
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "合同管理管理")
@RequestMapping("/api/jljsContractInfo")
public class JljsContractInfoController {

    private final JljsContractInfoService jljsContractInfoService;

    private final JljsMemberInfoService jljsMemberInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsContractInfo:list')")
    public void exportJljsContractInfo(HttpServletResponse response, JljsContractInfoQueryCriteria criteria) throws IOException {
        jljsContractInfoService.download(jljsContractInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询合同管理")
    @ApiOperation("查询合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:list')")
    public ResponseEntity<PageResult<JljsContractInfo>> queryJljsContractInfo(JljsContractInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(jljsContractInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增合同管理")
    @ApiOperation("新增合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:add')")
    public ResponseEntity<Object> createJljsContractInfo(@Validated @RequestBody JljsContractInfo resources){
        jljsContractInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改合同管理")
    @ApiOperation("修改合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:edit')")
    public ResponseEntity<Object> updateJljsContractInfo(@Validated @RequestBody JljsContractInfo resources){
        jljsContractInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除合同管理")
    @ApiOperation("删除合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:del')")
    public ResponseEntity<Object> deleteJljsContractInfo(@RequestBody List<String> ids) {
        jljsContractInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}