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
import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.service.IouNoteService;
import me.zhengjie.iou.domain.dto.IouNoteQueryCriteria;
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
@Api(tags = "借条")
@RequestMapping("/api/iouNote")
public class IouNoteController {

    private final IouNoteService iouNoteService;

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('iouNote:list')")
    public void exportIouNote(HttpServletResponse response, IouNoteQueryCriteria criteria) throws IOException {
        iouNoteService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询借条")
    @PreAuthorize("@el.check('iouNote:list')")
    public ResponseEntity<PageResult<IouNote>> queryIouNote(IouNoteQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(iouNoteService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增借条")
    @ApiOperation("新增借条")
    @PreAuthorize("@el.check('iouNote:add')")
    public ResponseEntity<Object> createIouNote(@Validated @RequestBody IouNote resources){
        iouNoteService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改借条")
    @ApiOperation("修改借条")
    @PreAuthorize("@el.check('iouNote:edit')")
    public ResponseEntity<Object> updateIouNote(@Validated @RequestBody IouNote resources){
        iouNoteService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除借条")
    @ApiOperation("删除借条")
    @PreAuthorize("@el.check('iouNote:del')")
    public ResponseEntity<Object> deleteIouNote(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        iouNoteService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
