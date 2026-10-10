/*
*  Copyright 2019-2025 Zheng Jie
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
package me.zhengjie.iou.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.service.IouUserService;
import me.zhengjie.iou.domain.dto.IouUserQueryCriteria;
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
* @author chunhui
* @date 2026-10-10
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "借条用户")
@RequestMapping("/api/iouUser")
public class IouUserController {

    private final IouUserService iouUserService;

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('iouUser:list')")
    public void exportIouUser(HttpServletResponse response, IouUserQueryCriteria criteria) throws IOException {
        iouUserService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询借条用户")
    @PreAuthorize("@el.check('iouUser:list')")
    public ResponseEntity<PageResult<IouUser>> queryIouUser(IouUserQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(iouUserService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @ApiOperation("新增借条用户")
    @PreAuthorize("@el.check('iouUser:add')")
    public ResponseEntity<Object> createIouUser(@Validated @RequestBody IouUser resources){
        iouUserService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @ApiOperation("修改借条用户")
    @PreAuthorize("@el.check('iouUser:edit')")
    public ResponseEntity<Object> updateIouUser(@Validated @RequestBody IouUser resources){
        iouUserService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @ApiOperation("删除借条用户")
    @PreAuthorize("@el.check('iouUser:del')")
    public ResponseEntity<Object> deleteIouUser(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        iouUserService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
