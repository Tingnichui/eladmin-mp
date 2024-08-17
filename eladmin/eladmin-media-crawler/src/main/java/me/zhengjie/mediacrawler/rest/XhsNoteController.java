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
package me.zhengjie.mediacrawler.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.mediacrawler.domain.XhsNote;
import me.zhengjie.mediacrawler.service.XhsNoteService;
import me.zhengjie.mediacrawler.domain.vo.XhsNoteQueryCriteria;
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
* @date 2024-08-17
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "小红书笔记管理")
@RequestMapping("/api/xhsNote")
public class XhsNoteController {

    private final XhsNoteService xhsNoteService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('xhsNote:list')")
    public void exportXhsNote(HttpServletResponse response, XhsNoteQueryCriteria criteria) throws IOException {
        xhsNoteService.download(xhsNoteService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询小红书笔记")
    @ApiOperation("查询小红书笔记")
    @PreAuthorize("@el.check('xhsNote:list')")
    public ResponseEntity<PageResult<XhsNote>> queryXhsNote(XhsNoteQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(xhsNoteService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增小红书笔记")
    @ApiOperation("新增小红书笔记")
    @PreAuthorize("@el.check('xhsNote:add')")
    public ResponseEntity<Object> createXhsNote(@Validated @RequestBody XhsNote resources){
        xhsNoteService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改小红书笔记")
    @ApiOperation("修改小红书笔记")
    @PreAuthorize("@el.check('xhsNote:edit')")
    public ResponseEntity<Object> updateXhsNote(@Validated @RequestBody XhsNote resources){
        xhsNoteService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除小红书笔记")
    @ApiOperation("删除小红书笔记")
    @PreAuthorize("@el.check('xhsNote:del')")
    public ResponseEntity<Object> deleteXhsNote(@RequestBody List<Integer> ids) {
        xhsNoteService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}