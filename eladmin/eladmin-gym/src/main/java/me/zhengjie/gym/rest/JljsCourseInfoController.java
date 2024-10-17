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
import me.zhengjie.gym.domain.JljsCourseInfo;
import me.zhengjie.gym.service.JljsCourseInfoService;
import me.zhengjie.gym.domain.vo.JljsCourseInfoQueryCriteria;
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
* @date 2024-06-30
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "课程管理管理")
@RequestMapping("/api/jljsCourseInfo")
public class JljsCourseInfoController {

    private final JljsCourseInfoService jljsCourseInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsCourseInfo:list')")
    public void exportJljsCourseInfo(HttpServletResponse response, JljsCourseInfoQueryCriteria criteria) throws IOException {
        jljsCourseInfoService.download(jljsCourseInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询课程管理")
    @ApiOperation("查询课程管理")
    @PreAuthorize("@el.check('jljsCourseInfo:list')")
    public ResponseEntity<PageResult<JljsCourseInfo>> queryJljsCourseInfo(JljsCourseInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(jljsCourseInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增课程管理")
    @ApiOperation("新增课程管理")
    @PreAuthorize("@el.check('jljsCourseInfo:add')")
    public ResponseEntity<Object> createJljsCourseInfo(@Validated @RequestBody JljsCourseInfo resources){
        jljsCourseInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改课程管理")
    @ApiOperation("修改课程管理")
    @PreAuthorize("@el.check('jljsCourseInfo:edit')")
    public ResponseEntity<Object> updateJljsCourseInfo(@Validated @RequestBody JljsCourseInfo resources){
        jljsCourseInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除课程管理")
    @ApiOperation("删除课程管理")
    @PreAuthorize("@el.check('jljsCourseInfo:del')")
    public ResponseEntity<Object> deleteJljsCourseInfo(@RequestBody List<String> ids) {
        jljsCourseInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}