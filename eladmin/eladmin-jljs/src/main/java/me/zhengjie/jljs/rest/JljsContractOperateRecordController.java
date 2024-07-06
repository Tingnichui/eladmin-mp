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
import me.zhengjie.jljs.domain.JljsContractOperateRecord;
import me.zhengjie.jljs.service.JljsContractOperateRecordService;
import me.zhengjie.jljs.domain.vo.JljsContractOperateRecordQueryCriteria;
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
* @date 2024-07-02
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "合同操作记录管理")
@RequestMapping("/api/jljsContractOperateRecord")
public class JljsContractOperateRecordController {

    private final JljsContractOperateRecordService jljsContractOperateRecordService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('jljsContractOperateRecord:list')")
    public void exportJljsContractOperateRecord(HttpServletResponse response, JljsContractOperateRecordQueryCriteria criteria) throws IOException {
        jljsContractOperateRecordService.download(jljsContractOperateRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询合同操作记录")
    @ApiOperation("查询合同操作记录")
    @PreAuthorize("@el.check('jljsContractOperateRecord:list')")
    public ResponseEntity<PageResult<JljsContractOperateRecord>> queryJljsContractOperateRecord(JljsContractOperateRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(jljsContractOperateRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增合同操作记录")
    @ApiOperation("新增合同操作记录")
    @PreAuthorize("@el.check('jljsContractOperateRecord:add')")
    public ResponseEntity<Object> createJljsContractOperateRecord(@Validated @RequestBody JljsContractOperateRecord resources){
        jljsContractOperateRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改合同操作记录")
    @ApiOperation("修改合同操作记录")
    @PreAuthorize("@el.check('jljsContractOperateRecord:edit')")
    public ResponseEntity<Object> updateJljsContractOperateRecord(@Validated @RequestBody JljsContractOperateRecord resources){
        jljsContractOperateRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除合同操作记录")
    @ApiOperation("删除合同操作记录")
    @PreAuthorize("@el.check('jljsContractOperateRecord:del')")
    public ResponseEntity<Object> deleteJljsContractOperateRecord(@RequestBody List<String> ids) {
        jljsContractOperateRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/revoke")
    @Log("撤销合同操作记录")
    @ApiOperation("撤销合同操作记录")
    @PreAuthorize("@el.check('jljsContractOperateRecord:revoke')")
    public ResponseEntity<Object> deleteJljsContractOperateRecord(@RequestBody String id) {
        jljsContractOperateRecordService.revoke(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}