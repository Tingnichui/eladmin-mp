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
import me.zhengjie.gym.domain.GymClassRecord;
import me.zhengjie.gym.service.GymClassRecordService;
import me.zhengjie.gym.domain.vo.GymClassRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;

import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.service.GymMemberInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.*;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.utils.PageResult;

/**
* @author genghui
* @date 2024-07-02
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "上课记录管理")
@RequestMapping("/api/jljsClassRecord")
public class GymClassRecordController {

    @Resource
    private GymClassRecordService gymClassRecordService;
    private final GymMemberInfoService gymMemberInfoService;
    private final GymContractInfoService gymContractInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsClassRecord:list')")
    public void exportJljsClassRecord(HttpServletResponse response, GymClassRecordQueryCriteria criteria) throws IOException {
        gymClassRecordService.download(gymClassRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询上课记录")
    @ApiOperation("查询上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:list')")
    public ResponseEntity<PageResult<GymClassRecord>> queryJljsClassRecord(GymClassRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(gymClassRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增上课记录")
    @ApiOperation("新增上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:add')")
    public ResponseEntity<Object> createJljsClassRecord(@Validated @RequestBody GymClassRecord resources){
        gymClassRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改上课记录")
    @ApiOperation("修改上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:edit')")
    public ResponseEntity<Object> updateJljsClassRecord(@Validated @RequestBody GymClassRecord resources){
        gymClassRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除上课记录")
    @ApiOperation("删除上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:del')")
    public ResponseEntity<Object> deleteJljsClassRecord(@RequestBody List<String> ids) {
        gymClassRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}