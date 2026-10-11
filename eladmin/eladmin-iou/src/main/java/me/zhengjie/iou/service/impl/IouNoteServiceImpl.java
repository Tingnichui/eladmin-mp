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
package me.zhengjie.iou.service.impl;

import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.iou.mapper.IouUserMapper;
import me.zhengjie.exception.BadRequestException;
import org.springframework.util.StringUtils;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.iou.service.IouNoteService;
import me.zhengjie.iou.domain.dto.IouNoteQueryCriteria;
import me.zhengjie.iou.mapper.IouNoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author chunhui
* @date 2026-10-10
**/
@Service
@RequiredArgsConstructor
public class IouNoteServiceImpl extends ServiceImpl<IouNoteMapper, IouNote> implements IouNoteService {

    private final IouNoteMapper iouNoteMapper;
    private final RedisUtils redisUtils;
    private final IouIdentitySupport identitySupport;
    private final IouUserMapper iouUserMapper;

    @Override
    public PageResult<IouNote> queryAll(IouNoteQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(iouNoteMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(IouNote resources) {
        if (resources.getId() != null) {
            throw new BadRequestException("新增借条不能指定ID");
        }
        prepare(resources, true);
        iouNoteMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(IouNote resources) {
        IouNote iouNote = iouNoteMapper.selectOne(new QueryWrapper<IouNote>().eq("id", resources.getId()).last("FOR UPDATE"));
        if (iouNote == null) {
            throw new BadRequestException("借条不存在");
        }
        assertEditable(iouNote);
        iouNote.copy(resources);
        prepare(iouNote, false);
        iouNote.setUpdateTime(null);
        iouNote.setRowVersion(iouNote.getRowVersion() == null ? 1L : iouNote.getRowVersion() + 1L);
        iouNoteMapper.updateById(iouNote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<IouNote> existing = iouNoteMapper.selectList(new QueryWrapper<IouNote>().in("id", ids).last("FOR UPDATE"));
        for (IouNote note : existing) assertEditable(note);
        iouNoteMapper.deleteBatchIds(ids);
    }

    private void assertEditable(IouNote note) {
        if (note.getCreateRequestKey() != null) {
            throw new BadRequestException("用户端草稿及还款计划请通过用户端修改或撤销");
        }
        if (note.getTermsLockedAt() != null || note.getBorrowerSignedAt() != null || note.getLenderSignedAt() != null
            || Integer.valueOf(1).equals(note.getBorrowerSignStatus()) || Integer.valueOf(2).equals(note.getBorrowerSignStatus())
            || Integer.valueOf(1).equals(note.getLenderSignStatus()) || Integer.valueOf(2).equals(note.getLenderSignStatus())
            || Integer.valueOf(7).equals(note.getStatus())) {
            throw new BadRequestException("签署处理中或已锁定的借条不能通过后台修改或删除");
        }
    }

    private void prepare(IouNote note, boolean create) {
        IouUser creator = iouUserMapper.selectById(note.getCreatorUserId());
        if (creator == null) {
            throw new BadRequestException("发起人用户不存在");
        }
        if (create || Integer.valueOf(7).equals(note.getStatus())) assertEditable(note);
        if (StringUtils.hasText(note.getBorrowerIdNo())) {
            note.setBorrowerIdNoHash(identitySupport.hash(note.getBorrowerIdNo()));
            note.setBorrowerIdNoEncrypted(identitySupport.encrypt(note.getBorrowerIdNo()));
        }
        if (StringUtils.hasText(note.getLenderIdNo())) {
            note.setLenderIdNoHash(identitySupport.hash(note.getLenderIdNo()));
            note.setLenderIdNoEncrypted(identitySupport.encrypt(note.getLenderIdNo()));
        }
        note.setBorrowerIdNo(null);
        note.setLenderIdNo(null);
        if (note.getCreatorRole() != null) {
            if (note.getCreatorRole() != 1 && note.getCreatorRole() != 2) throw new BadRequestException("发起人身份不合法");
            if (!Integer.valueOf(2).equals(creator.getIdVerifyStatus()) || !StringUtils.hasText(creator.getIdNoHash())
                || !StringUtils.hasText(creator.getIdNoEncrypted()) || !StringUtils.hasText(creator.getRealName())) {
                throw new BadRequestException("发起人身份尚未核验");
            }
            if (note.getCreatorRole() == 1) {
                note.setBorrowerName(creator.getRealName()); note.setBorrowerIdNoHash(creator.getIdNoHash());
                note.setBorrowerIdNoEncrypted(creator.getIdNoEncrypted());
            } else {
                note.setLenderName(creator.getRealName()); note.setLenderIdNoHash(creator.getIdNoHash());
                note.setLenderIdNoEncrypted(creator.getIdNoEncrypted());
            }
        }
        boolean borrowerComplete = StringUtils.hasText(note.getBorrowerName()) && StringUtils.hasText(note.getBorrowerIdNoHash()) && StringUtils.hasText(note.getBorrowerIdNoEncrypted());
        boolean lenderComplete = StringUtils.hasText(note.getLenderName()) && StringUtils.hasText(note.getLenderIdNoHash()) && StringUtils.hasText(note.getLenderIdNoEncrypted());
        boolean borrowerEmpty = !StringUtils.hasText(note.getBorrowerName()) && !StringUtils.hasText(note.getBorrowerIdNoHash()) && !StringUtils.hasText(note.getBorrowerIdNoEncrypted());
        boolean lenderEmpty = !StringUtils.hasText(note.getLenderName()) && !StringUtils.hasText(note.getLenderIdNoHash()) && !StringUtils.hasText(note.getLenderIdNoEncrypted());
        if (borrowerEmpty) note.setBorrowerName(null);
        if (lenderEmpty) note.setLenderName(null);
        if (!borrowerComplete || !lenderComplete) {
            boolean pendingDraft = Integer.valueOf(0).equals(note.getStatus()) &&
                ((Integer.valueOf(1).equals(note.getCreatorRole()) && borrowerComplete && lenderEmpty)
                || (Integer.valueOf(2).equals(note.getCreatorRole()) && lenderComplete && borrowerEmpty));
            if (!pendingDraft) throw new BadRequestException("双方身份须完整，待邀请草稿须指定本人身份并将对方信息留空");
        }
        if (note.getBorrowerIdNoHash() != null && note.getBorrowerIdNoHash().equals(note.getLenderIdNoHash())) {
            throw new BadRequestException("借款人与出借人不能是同一人");
        }
        if (note.getLoanType() < 1 || note.getLoanType() > 2 || note.getStatus() < 0 || note.getStatus() > 7) {
            throw new BadRequestException("借款类型或状态不合法");
        }
        if (note.getPrincipalAmount().signum() <= 0 || note.getAnnualRate().signum() < 0
            || note.getInterestAmount().signum() < 0 || note.getServiceFee().signum() < 0
            || note.getTotalAmount().compareTo(note.getPrincipalAmount().add(note.getInterestAmount())) != 0) {
            throw new BadRequestException("金额不合法，应还总额须等于本金加利息");
        }
        if (note.getDueDate().before(note.getLoanDate())) {
            throw new BadRequestException("还款日期不能早于借款日期");
        }
        note.setCreateTime(null);
        note.setUpdateTime(null);
        note.setBorrowerSignedAt(null);
        note.setLenderSignedAt(null);
        note.setEffectiveAt(null);
    }

    @Override
    public void download(IouNoteQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("借条编号");
        headers.add("发起人ID");
        headers.add("借款人");
        headers.add("出借人");
        headers.add("借款类型");
        headers.add("本金");
        headers.add("年利率（0.15表示15%）");
        headers.add("借款日期");
        headers.add("最终还款日期");
        headers.add("总利息");
        headers.add("应还本金加利息");
        headers.add("借款用途");
        headers.add("备注");
        headers.add("服务费");
        headers.add("借款人签署时间");
        headers.add("出借人签署时间");
        headers.add("协议文件存储标识");
        headers.add("借条状态");
        headers.add("生效时间");
        headers.add("取消原因");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<IouNote> records = iouNoteMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (IouNote iouNote : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("借条编号", iouNote.getNoteNo());
                    map.put("发起人ID", getExportValue(iouNote.getCreatorUserId()));
                    map.put("借款人", iouNote.getBorrowerName());
                    map.put("出借人", iouNote.getLenderName());
                    map.put("借款类型", getDictLabel("iou_note_loan_type", iouNote.getLoanType()));
                    map.put("本金", iouNote.getPrincipalAmount());
                    map.put("年利率（0.15表示15%）", iouNote.getAnnualRate());
                    map.put("借款日期", iouNote.getLoanDate());
                    map.put("最终还款日期", iouNote.getDueDate());
                    map.put("总利息", iouNote.getInterestAmount());
                    map.put("应还本金加利息", iouNote.getTotalAmount());
                    map.put("借款用途", iouNote.getPurpose());
                    map.put("备注", iouNote.getRemark());
                    map.put("服务费", iouNote.getServiceFee());
                    map.put("借款人签署时间", iouNote.getBorrowerSignedAt());
                    map.put("出借人签署时间", iouNote.getLenderSignedAt());
                    map.put("协议文件存储标识", iouNote.getAgreementFile());
                    map.put("借条状态", getDictLabel("iou_note_status", iouNote.getStatus()));
                    map.put("生效时间", iouNote.getEffectiveAt());
                    map.put("取消原因", iouNote.getCancelReason());
                    map.put("创建时间", iouNote.getCreateTime());
                    map.put("更新时间", iouNote.getUpdateTime());
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }

    private Object getDictLabel(String dictName, Object dictValue) {
        String label = redisUtils.getDictLabel(dictName, dictValue);
        return label == null || label.length() == 0 ? getExportValue(dictValue) : label;
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
}
