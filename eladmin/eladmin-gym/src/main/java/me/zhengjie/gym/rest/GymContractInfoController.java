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
package me.zhengjie.gym.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.domain.vo.GymContractInfoQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;

import me.zhengjie.gym.service.GymMemberInfoService;
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
public class GymContractInfoController {

    private final GymContractInfoService gymContractInfoService;

    private final GymMemberInfoService gymMemberInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsContractInfo:list')")
    public void exportJljsContractInfo(HttpServletResponse response, GymContractInfoQueryCriteria criteria) throws IOException {
        gymContractInfoService.download(gymContractInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询合同管理")
    @ApiOperation("查询合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:list')")
    public ResponseEntity<PageResult<GymContractInfo>> queryJljsContractInfo(GymContractInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(gymContractInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增合同管理")
    @ApiOperation("新增合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:add')")
    public ResponseEntity<Object> createJljsContractInfo(@Validated @RequestBody GymContractInfo resources){
        gymContractInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改合同管理")
    @ApiOperation("修改合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:edit')")
    public ResponseEntity<Object> updateJljsContractInfo(@Validated @RequestBody GymContractInfo resources){
        gymContractInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除合同管理")
    @ApiOperation("删除合同管理")
    @PreAuthorize("@el.check('jljsContractInfo:del')")
    public ResponseEntity<Object> deleteJljsContractInfo(@RequestBody List<String> ids) {
        gymContractInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}