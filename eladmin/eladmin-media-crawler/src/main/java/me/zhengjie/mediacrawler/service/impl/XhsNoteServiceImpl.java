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
package me.zhengjie.mediacrawler.service.impl;

import me.zhengjie.mediacrawler.domain.XhsNote;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.mediacrawler.service.XhsNoteService;
import me.zhengjie.mediacrawler.domain.vo.XhsNoteQueryCriteria;
import me.zhengjie.mediacrawler.mapper.XhsNoteMapper;
import me.zhengjie.utils.enums.DatasourceEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2024-08-17
**/
@Service
@RequiredArgsConstructor
public class XhsNoteServiceImpl extends ServiceImpl<XhsNoteMapper, XhsNote> implements XhsNoteService {

    private final XhsNoteMapper xhsNoteMapper;

    @Override
    public PageResult<XhsNote> queryAll(XhsNoteQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(xhsNoteMapper.findAll(criteria, page));
    }

    @Override
    public List<XhsNote> queryAll(XhsNoteQueryCriteria criteria){
        return xhsNoteMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(XhsNote resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(XhsNote resources) {
        XhsNote xhsNote = getById(resources.getId());
        xhsNote.copy(resources);
        saveOrUpdate(xhsNote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<XhsNote> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (XhsNote xhsNote : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("用户ID", xhsNote.getUserId());
            map.put("用户昵称", xhsNote.getNickname());
            map.put("用户头像地址", xhsNote.getAvatar());
            map.put("评论时的IP地址", xhsNote.getIpLocation());
            map.put("记录添加时间戳", xhsNote.getAddTs());
            map.put("记录最后修改时间戳", xhsNote.getLastModifyTs());
            map.put("笔记ID", xhsNote.getNoteId());
            map.put("笔记类型(normal | video)", xhsNote.getType());
            map.put("笔记标题", xhsNote.getTitle());
            map.put("笔记描述", xhsNote.getDesc());
            map.put("视频地址", xhsNote.getVideoUrl());
            map.put("笔记发布时间戳", xhsNote.getTime());
            map.put("笔记最后更新时间戳", xhsNote.getLastUpdateTime());
            map.put("笔记点赞数", xhsNote.getLikedCount());
            map.put("笔记收藏数", xhsNote.getCollectedCount());
            map.put("笔记评论数", xhsNote.getCommentCount());
            map.put("笔记分享数", xhsNote.getShareCount());
            map.put("笔记封面图片列表", xhsNote.getImageList());
            map.put("标签列表", xhsNote.getTagList());
            map.put("笔记详情页的URL", xhsNote.getNoteUrl());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}