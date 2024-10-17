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
import me.zhengjie.gym.domain.GymMemberInfo;
import me.zhengjie.gym.service.GymMemberInfoService;
import me.zhengjie.gym.domain.vo.GymMemberInfoQueryCriteria;
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
@Api(tags = "会员管理管理")
@RequestMapping("/api/jljsMemberInfo")
public class GymMemberInfoController {

    private final GymMemberInfoService gymMemberInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsMemberInfo:list')")
    public void exportJljsMemberInfo(HttpServletResponse response, GymMemberInfoQueryCriteria criteria) throws IOException {
        gymMemberInfoService.download(gymMemberInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询会员管理")
    @ApiOperation("查询会员管理")
    @PreAuthorize("@el.check('jljsMemberInfo:list')")
    public ResponseEntity<PageResult<GymMemberInfo>> queryJljsMemberInfo(GymMemberInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(gymMemberInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增会员管理")
    @ApiOperation("新增会员管理")
    @PreAuthorize("@el.check('jljsMemberInfo:add')")
    public ResponseEntity<Object> createJljsMemberInfo(@Validated @RequestBody GymMemberInfo resources){
        gymMemberInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改会员管理")
    @ApiOperation("修改会员管理")
    @PreAuthorize("@el.check('jljsMemberInfo:edit')")
    public ResponseEntity<Object> updateJljsMemberInfo(@Validated @RequestBody GymMemberInfo resources){
        gymMemberInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除会员管理")
    @ApiOperation("删除会员管理")
    @PreAuthorize("@el.check('jljsMemberInfo:del')")
    public ResponseEntity<Object> deleteJljsMemberInfo(@RequestBody List<String> ids) {
        gymMemberInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}