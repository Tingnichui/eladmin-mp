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
import me.zhengjie.gym.domain.JljsCoachInfo;
import me.zhengjie.gym.service.JljsCoachInfoService;
import me.zhengjie.gym.domain.vo.JljsCoachInfoQueryCriteria;
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
@Api(tags = "教练管理管理")
@RequestMapping("/api/jljsCoachInfo")
public class JljsCoachInfoController {

    private final JljsCoachInfoService jljsCoachInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsCoachInfo:list')")
    public void exportJljsCoachInfo(HttpServletResponse response, JljsCoachInfoQueryCriteria criteria) throws IOException {
        jljsCoachInfoService.download(jljsCoachInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询教练管理")
    @ApiOperation("查询教练管理")
    @PreAuthorize("@el.check('jljsCoachInfo:list')")
    public ResponseEntity<PageResult<JljsCoachInfo>> queryJljsCoachInfo(JljsCoachInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(jljsCoachInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增教练管理")
    @ApiOperation("新增教练管理")
    @PreAuthorize("@el.check('jljsCoachInfo:add')")
    public ResponseEntity<Object> createJljsCoachInfo(@Validated @RequestBody JljsCoachInfo resources){
        jljsCoachInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改教练管理")
    @ApiOperation("修改教练管理")
    @PreAuthorize("@el.check('jljsCoachInfo:edit')")
    public ResponseEntity<Object> updateJljsCoachInfo(@Validated @RequestBody JljsCoachInfo resources){
        jljsCoachInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除教练管理")
    @ApiOperation("删除教练管理")
    @PreAuthorize("@el.check('jljsCoachInfo:del')")
    public ResponseEntity<Object> deleteJljsCoachInfo(@RequestBody List<String> ids) {
        jljsCoachInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}