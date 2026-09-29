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
package me.zhengjie.invest.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.ResearchStrategyAlertEvents;
import me.zhengjie.invest.service.ResearchStrategyAlertEventsService;
import me.zhengjie.invest.domain.dto.ResearchStrategyAlertEventsQueryCriteria;
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
* @date 2026-09-29
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "策略告警事件")
@RequestMapping("/api/researchStrategyAlertEvents")
public class ResearchStrategyAlertEventsController {

    private final ResearchStrategyAlertEventsService researchStrategyAlertEventsService;

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('researchStrategyAlertEvents:list')")
    public void exportResearchStrategyAlertEvents(HttpServletResponse response, ResearchStrategyAlertEventsQueryCriteria criteria) throws IOException {
        researchStrategyAlertEventsService.download(criteria, response);
    }

    @GetMapping
    @ApiOperation("查询策略告警事件")
    @PreAuthorize("@el.check('researchStrategyAlertEvents:list')")
    public ResponseEntity<PageResult<ResearchStrategyAlertEvents>> queryResearchStrategyAlertEvents(ResearchStrategyAlertEventsQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
        return new ResponseEntity<>(researchStrategyAlertEventsService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增策略告警事件")
    @ApiOperation("新增策略告警事件")
    @PreAuthorize("@el.check('researchStrategyAlertEvents:add')")
    public ResponseEntity<Object> createResearchStrategyAlertEvents(@Validated @RequestBody ResearchStrategyAlertEvents resources){
        researchStrategyAlertEventsService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改策略告警事件")
    @ApiOperation("修改策略告警事件")
    @PreAuthorize("@el.check('researchStrategyAlertEvents:edit')")
    public ResponseEntity<Object> updateResearchStrategyAlertEvents(@Validated @RequestBody ResearchStrategyAlertEvents resources){
        researchStrategyAlertEventsService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除策略告警事件")
    @ApiOperation("删除策略告警事件")
    @PreAuthorize("@el.check('researchStrategyAlertEvents:del')")
    public ResponseEntity<Object> deleteResearchStrategyAlertEvents(@ApiParam(value = "传ID数组[]") @RequestBody List<Long> ids) {
        researchStrategyAlertEventsService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
