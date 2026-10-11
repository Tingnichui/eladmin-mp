package me.zhengjie.modules.iou;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.mapper.IouNoteMapper;
import me.zhengjie.iou.mapper.IouUserMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/** H5 只读查询。身份只由服务端登录账号确定，响应不包含证件、关联散列或文件存储地址。 */
@Service
@RequiredArgsConstructor
public class IouAppQueryService {
    private final IouUserMapper users;
    private final IouNoteMapper notes;
    private static final String[] READ_COLUMNS = {"id", "note_no", "creator_user_id", "borrower_name",
        "lender_name", "borrower_id_no_hash", "lender_id_no_hash", "loan_type", "principal_amount",
        "annual_rate", "loan_date", "due_date", "interest_amount", "total_amount", "purpose", "remark",
        "service_fee", "borrower_signed_at", "lender_signed_at", "status", "effective_at", "cancel_reason",
        "create_time", "update_time", "creator_role", "repayment_method", "row_version", "terms_locked_at", "borrower_sign_status", "lender_sign_status"};

    private IouUser person(long sysUserId) {
        IouUser user = users.selectOne(new LambdaQueryWrapper<IouUser>().eq(IouUser::getSysUserId, sysUserId));
        if (user == null || !Integer.valueOf(2).equals(user.getIdVerifyStatus())
            || user.getIdNoHash() == null || !user.getIdNoHash().matches("[a-f0-9]{64}")) {
            throw new BadRequestException("身份信息尚未核验，暂不能查询借条");
        }
        return user;
    }

    /** 必须是当事人；草稿额外限制为本人发起。 */
    QueryWrapper<IouNote> visible(IouUser user) {
        return new QueryWrapper<IouNote>()
            .and(q -> q.eq("borrower_id_no_hash", user.getIdNoHash()).or().eq("lender_id_no_hash", user.getIdNoHash()))
            .and(q -> q.ne("status", 0).or().eq("creator_user_id", user.getId()));
    }

    QueryWrapper<IouNote> filtered(IouUser user, String role, Integer status, boolean pending) {
        QueryWrapper<IouNote> query = visible(user);
        if ("borrower".equals(role)) query.eq("borrower_id_no_hash", user.getIdNoHash());
        else if ("lender".equals(role)) query.eq("lender_id_no_hash", user.getIdNoHash());
        else if (!"all".equals(role)) throw new BadRequestException("借条身份筛选不合法");
        if (status != null) {
            if (status < 0 || status > 7) throw new BadRequestException("借条状态筛选不合法");
            query.eq("status", status);
        }
        if (pending) {
            // 已发起或已签署的不再计入“待我确认”。这里只查询，不执行签署。
            query.eq("status", 1).ne("creator_user_id", user.getId()).and(q -> q
                .nested(b -> b.eq("borrower_id_no_hash", user.getIdNoHash()).isNull("borrower_signed_at"))
                .or(l -> l.eq("lender_id_no_hash", user.getIdNoHash()).isNull("lender_signed_at")));
        }
        return query;
    }

    public Map<String, Object> list(long sysUserId, String role, Integer status, boolean pending, int page, int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 50) throw new BadRequestException("分页参数不合法");
        IouUser user = person(sysUserId);
        Page<IouNote> result = notes.selectPage(new Page<>(page, size), filtered(user, role, status, pending)
            .select(READ_COLUMNS).orderByDesc("create_time", "id"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getRecords().stream().map(n -> view(n, user, false)).collect(Collectors.toList()));
        response.put("totalElements", result.getTotal());
        return response;
    }

    public Map<String, Object> detail(long sysUserId, long id) {
        IouUser user = person(sysUserId);
        IouNote note = notes.selectOne(visible(user).select(READ_COLUMNS).eq("id", id));
        if (note == null) throw new BadRequestException("借条不存在或无权查看");
        return view(note, user, true);
    }

    public Map<String, Object> summary(long sysUserId) {
        IouUser user = person(sysUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("borrowed", aggregate(user, "borrower_id_no_hash"));
        result.put("lent", aggregate(user, "lender_id_no_hash"));
        result.put("pendingCount", notes.selectCount(filtered(user, "all", null, true)));
        return result;
    }

    private Map<String, Object> aggregate(IouUser user, String column) {
        List<Map<String, Object>> rows = notes.selectMaps(visible(user).eq(column, user.getIdNoHash()).eq("status", 2)
            .select("COUNT(*) AS noteCount", "COALESCE(SUM(principal_amount), 0) AS principal"));
        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", row.get("noteCount"));
        result.put("principal", new BigDecimal(row.get("principal").toString()).setScale(2).toPlainString());
        return result;
    }

    Map<String, Object> view(IouNote note, IouUser user, boolean detail) {
        boolean borrower = user.getIdNoHash().equals(note.getBorrowerIdNoHash());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", note.getId().toString());
        result.put("noteNo", note.getNoteNo());
        result.put("role", borrower ? "borrower" : "lender");
        String counterparty = borrower ? note.getLenderName() : note.getBorrowerName();
        result.put("counterpartyName", counterparty == null ? "待对方加入" : counterparty);
        result.put("principalAmount", money(note.getPrincipalAmount()));
        result.put("dueDate", date(note.getDueDate()));
        result.put("status", note.getStatus());
        result.put("createTime", time(note.getCreateTime()));
        result.put("stage", me.zhengjie.iou.service.IouNoteBusinessService.stage(note));
        result.put("version", String.valueOf(note.getRowVersion() == null ? 0 : note.getRowVersion()));
        result.put("creatorRole", note.getCreatorRole());
        result.put("legacy", note.getRepaymentMethod() == null);
        if (detail) {
            result.put("borrowerName", note.getBorrowerName());
            result.put("lenderName", note.getLenderName());
            result.put("loanType", note.getLoanType());
            result.put("annualRate", note.getAnnualRate() == null ? null : note.getAnnualRate().toPlainString());
            result.put("loanDate", date(note.getLoanDate()));
            result.put("interestAmount", money(note.getInterestAmount()));
            result.put("totalAmount", money(note.getTotalAmount()));
            result.put("serviceFee", money(note.getServiceFee()));
            result.put("purpose", note.getPurpose());
            result.put("remark", note.getRemark());
            result.put("borrowerSignedAt", time(note.getBorrowerSignedAt()));
            result.put("lenderSignedAt", time(note.getLenderSignedAt()));
            result.put("effectiveAt", time(note.getEffectiveAt()));
            result.put("cancelReason", note.getCancelReason());
        }
        return result;
    }
    private String money(BigDecimal value) { return value == null ? null : value.setScale(2).toPlainString(); }
    private String date(Timestamp value) { return value == null ? null : value.toLocalDateTime().toLocalDate().toString(); }
    private String time(Timestamp value) { return value == null ? null : value.toLocalDateTime().toString(); }
}
