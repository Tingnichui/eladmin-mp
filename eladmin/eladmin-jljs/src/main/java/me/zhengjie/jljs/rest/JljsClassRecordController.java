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

import me.zhengjie.annotation.Log;
import me.zhengjie.jljs.domain.JljsClassRecord;
import me.zhengjie.jljs.service.JljsClassRecordService;
import me.zhengjie.jljs.domain.vo.JljsClassRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;
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
public class JljsClassRecordController {

    @Resource
    private JljsClassRecordService jljsClassRecordService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsClassRecord:list')")
    public void exportJljsClassRecord(HttpServletResponse response, JljsClassRecordQueryCriteria criteria) throws IOException {
        jljsClassRecordService.download(jljsClassRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询上课记录")
    @ApiOperation("查询上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:list')")
    public ResponseEntity<PageResult<JljsClassRecord>> queryJljsClassRecord(JljsClassRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(jljsClassRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增上课记录")
    @ApiOperation("新增上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:add')")
    public ResponseEntity<Object> createJljsClassRecord(@Validated @RequestBody JljsClassRecord resources){
        jljsClassRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改上课记录")
    @ApiOperation("修改上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:edit')")
    public ResponseEntity<Object> updateJljsClassRecord(@Validated @RequestBody JljsClassRecord resources){
        jljsClassRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除上课记录")
    @ApiOperation("删除上课记录")
    @PreAuthorize("@el.check('jljsClassRecord:del')")
    public ResponseEntity<Object> deleteJljsClassRecord(@RequestBody List<String> ids) {
        jljsClassRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}