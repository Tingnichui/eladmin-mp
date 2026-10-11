package me.zhengjie.iou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.domain.dto.IouNoteConditions;
import me.zhengjie.iou.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;

/** H5 use cases. Real identity is resolved server-side; handwritten signing is synchronous. */
@Service
@RequiredArgsConstructor
public class IouNoteBusinessService {
    private final IouNoteMapper notes;
    private final IouNotePlanMapper plans;
    private final IouUserMapper users;
    private final IouRepaymentCalculator calculator;
    private final IouNoteOptions options;
    private final IouAgreementStore store;
    private final ObjectMapper json=new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);

    public IouUser person(long sysId,boolean lock){
        QueryWrapper<IouUser> q=new QueryWrapper<IouUser>().eq("sys_user_id",sysId);if(lock)q.last("FOR UPDATE");
        IouUser u=users.selectOne(q);
        if(u==null||!Integer.valueOf(2).equals(u.getIdVerifyStatus())||u.getIdNoHash()==null||!u.getIdNoHash().matches("[a-f0-9]{64}")||u.getRealName()==null||u.getIdNoEncrypted()==null)
            fail(403,"IDENTITY_NOT_VERIFIED","请先完成实名核验");
        return u;
    }
    public Map<String,Object> options(long sysId){person(sysId,false);return options.view();}
    public Map<String,Object> quote(long sysId,IouNoteConditions input){person(sysId,false);return quoteView(calculator.calculate(input));}
    private Map<String,Object> quoteView(IouRepaymentCalculator.Quote q){return map("conditions",q.getConditions(),"summary",q.getSummary(),"plan",planView(q.getPlan()),"interestRule",q.getInterestRule(),"calculationVersion",q.getCalculationVersion(),"warnings",q.getWarnings());}
    private static List<Map<String,Object>> planView(List<IouNotePlan> rows){List<Map<String,Object>>r=new ArrayList<>();for(IouNotePlan p:rows)r.add(map("periodNo",p.getPeriodNo(),"dueDate",p.getDueDate().toString(),"principalAmount",money(p.getPrincipalAmount()),"interestAmount",money(p.getInterestAmount()),"totalAmount",money(p.getTotalAmount()),"remainingPrincipal",money(p.getRemainingPrincipal())));return r;}

    @Transactional public Map<String,Object> create(long sysId,String key,IouNoteConditions input){
        enabled();validateKey(key);IouUser u=person(sysId,true);validateRole(input);IouRepaymentCalculator.Quote q=calculator.calculate(input);
        String requestHash=IouAgreementStore.hash(encode(q.getConditions()));
        IouNote old=notes.selectOne(new QueryWrapper<IouNote>().eq("creator_user_id",u.getId()).eq("create_request_key",key));
        if(old!=null){if(!requestHash.equals(old.getCreateRequestHash()))fail(409,"IDEMPOTENCY_CONFLICT","同一操作编号的内容已变化");return view(old,u);}
        IouNote n=new IouNote();n.setCreatorUserId(u.getId());n.setNoteNo("IOU"+UUID.randomUUID().toString().replace("-","").substring(0,28));
        n.setStatus(0);n.setRowVersion(0L);n.setBorrowerSignStatus(0);n.setLenderSignStatus(0);n.setServiceFee(BigDecimal.ZERO.setScale(2));
        n.setCreateRequestKey(key);n.setCreateRequestHash(requestHash);apply(n,u,q);
        if(notes.insert(n)!=1)throw new IllegalStateException("草稿保存失败");savePlan(n.getId(),q.getPlan());return view(n,u);
    }
    @Transactional public Map<String,Object> update(long sysId,long id,String version,IouNoteConditions input){
        enabled();IouUser u=person(sysId,false);IouNote n=note(id,true);owner(n,u);editable(n);version(n,version);validateRole(input);
        IouRepaymentCalculator.Quote q=calculator.calculate(input);apply(n,u,q);
        // Explicit NULL fields cannot rely on MyBatis' default NOT_NULL update strategy.
        UpdateWrapper<IouNote>w=new UpdateWrapper<IouNote>().eq("id",id)
            .set("borrower_name",n.getBorrowerName()).set("borrower_id_no_hash",n.getBorrowerIdNoHash()).set("borrower_id_no_encrypted",n.getBorrowerIdNoEncrypted())
            .set("lender_name",n.getLenderName()).set("lender_id_no_hash",n.getLenderIdNoHash()).set("lender_id_no_encrypted",n.getLenderIdNoEncrypted())
            .set("fixed_interest_amount",n.getFixedInterestAmount()).set("first_due_date",n.getFirstDueDate()).set("repayment_cycle_months",n.getRepaymentCycleMonths());
        n.setRowVersion(n.getRowVersion()+1);if(notes.update(n,w)!=1)throw new IllegalStateException("草稿更新失败");
        plans.delete(new QueryWrapper<IouNotePlan>().eq("note_id",id));savePlan(id,q.getPlan());return view(n,u);
    }
    @Transactional public void delete(long sysId,long id,String version){
        IouUser u=person(sysId,false);IouNote n=notes.selectOne(new QueryWrapper<IouNote>().eq("id",id).last("FOR UPDATE"));
        if(n==null)return;
        owner(n,u);editable(n);version(n,version);
        plans.delete(new QueryWrapper<IouNotePlan>().eq("note_id",id));notes.deleteById(id);
    }
    private void apply(IouNote n,IouUser u,IouRepaymentCalculator.Quote q){
        IouNoteConditions c=q.getConditions();boolean borrower="borrower".equals(c.getCreatorRole());n.setCreatorRole(borrower?1:2);
        n.setBorrowerName(borrower?u.getRealName():null);n.setBorrowerIdNoHash(borrower?u.getIdNoHash():null);n.setBorrowerIdNoEncrypted(borrower?u.getIdNoEncrypted():null);
        n.setLenderName(borrower?null:u.getRealName());n.setLenderIdNoHash(borrower?null:u.getIdNoHash());n.setLenderIdNoEncrypted(borrower?null:u.getIdNoEncrypted());
        n.setPrincipalAmount(new BigDecimal(c.getPrincipalAmount()));n.setAnnualRate(new BigDecimal(c.getAnnualRate()));
        n.setLoanDate(day(c.getLoanDate()));n.setDueDate(day(q.getSummary().get("dueDate")));n.setFirstDueDate(day(q.getPlan().get(0).getDueDate().toString()));
        n.setInterestAmount(new BigDecimal(q.getSummary().get("interestAmount")));n.setTotalAmount(new BigDecimal(q.getSummary().get("totalAmount")));
        n.setFixedInterestAmount(c.getFixedInterestAmount()==null?null:new BigDecimal(c.getFixedInterestAmount()));n.setRepaymentMethod(c.getRepaymentMethod());
        n.setLoanType("once".equals(c.getRepaymentMethod())?1:2);n.setInstallmentCount(q.getPlan().size());n.setRepaymentCycleMonths(c.getRepaymentCycleMonths());
        n.setPurpose(c.getPurpose());n.setRemark(c.getRemark());n.setPrepaymentRule(c.getPrepaymentRule());n.setOverdueTerms(c.getOverdueTerms());
        n.setInterestRule(q.getInterestRule());n.setCalculationVersion(q.getCalculationVersion());
    }
    private void savePlan(long id,List<IouNotePlan> rows){for(IouNotePlan row:rows){row.setNoteId(id);if(plans.insert(row)!=1)throw new IllegalStateException("计划保存失败");}}
    private List<IouNotePlan> rows(long id){return plans.selectList(new QueryWrapper<IouNotePlan>().eq("note_id",id).orderByAsc("period_no"));}
    public Map<String,Object> detail(long sysId,long id){IouUser u=person(sysId,false);IouNote n=note(id,false);participant(n,u);return view(n,u);}
    public Map<String,Object> plan(long sysId,long id){IouUser u=person(sysId,false);IouNote n=note(id,false);participant(n,u);return map("calculationVersion",n.getCalculationVersion(),"interestRule",n.getInterestRule(),"content",planView(rows(id)));}
    private Map<String,Object> view(IouNote n,IouUser u){
        String stage=stage(n);boolean borrower=u.getIdNoHash().equals(n.getBorrowerIdNoHash());
        Map<String,Object> v=map("id",n.getId().toString(),"noteNo",n.getNoteNo(),"status",n.getStatus(),"stage",stage,"version",String.valueOf(n.getRowVersion()==null?0:n.getRowVersion()),
            "creatorRole",n.getCreatorRole()==null?null:Integer.valueOf(1).equals(n.getCreatorRole())?"borrower":"lender","role",borrower?"borrower":"lender",
            "borrowerName",n.getBorrowerName(),"lenderName",n.getLenderName(),"counterpartyName",borrower?n.getLenderName():n.getBorrowerName(),
            "principalAmount",money(n.getPrincipalAmount()),"annualRate",n.getAnnualRate()==null?null:n.getAnnualRate().toPlainString(),"loanType",n.getLoanType(),"loanDate",date(n.getLoanDate()),"dueDate",date(n.getDueDate()),
            "interestAmount",money(n.getInterestAmount()),"totalAmount",money(n.getTotalAmount()),"serviceFee",money(n.getServiceFee()),"purpose",n.getPurpose(),"remark",n.getRemark(),
            "borrowerSignedAt",time(n.getBorrowerSignedAt()),"lenderSignedAt",time(n.getLenderSignedAt()),"effectiveAt",time(n.getEffectiveAt()),"createTime",time(n.getCreateTime()),"cancelReason",n.getCancelReason(),
            "mySignStatus",borrower?n.getBorrowerSignStatus():n.getLenderSignStatus(),"counterpartySignStatus",borrower?n.getLenderSignStatus():n.getBorrowerSignStatus(),"legacy",n.getRepaymentMethod()==null);
        v.put("conditions",conditions(n));v.put("plan",planView(rows(n.getId())));v.put("actions",actions(n,u));v.put("calculationVersion",n.getCalculationVersion());v.put("interestRule",n.getInterestRule());
        if(v.get("counterpartyName")==null)v.put("counterpartyName","待对方加入");return v;
    }
    private IouNoteConditions conditions(IouNote n){
        if(n.getRepaymentMethod()==null)return null;
        IouNoteConditions c=new IouNoteConditions();c.setCreatorRole(Integer.valueOf(1).equals(n.getCreatorRole())?"borrower":"lender");
        c.setPrincipalAmount(money(n.getPrincipalAmount()));c.setAnnualRate(n.getAnnualRate().toPlainString());c.setInterestFree(n.getAnnualRate().signum()==0&&n.getInterestAmount().signum()==0);
        c.setRepaymentMethod(n.getRepaymentMethod());c.setLoanDate(date(n.getLoanDate()));c.setPurpose(n.getPurpose());c.setRemark(n.getRemark());c.setPrepaymentRule(n.getPrepaymentRule());c.setOverdueTerms(n.getOverdueTerms());
        if("once".equals(n.getRepaymentMethod()))c.setDueDate(date(n.getDueDate()));
        else if("custom".equals(n.getRepaymentMethod())){List<IouNoteConditions.CustomPeriod>p=new ArrayList<>();for(IouNotePlan row:rows(n.getId())){IouNoteConditions.CustomPeriod a=new IouNoteConditions.CustomPeriod();a.setDueDate(row.getDueDate().toString());a.setPrincipalAmount(money(row.getPrincipalAmount()));p.add(a);}c.setCustomPlan(p);}
        else{c.setInstallmentCount(n.getInstallmentCount());c.setRepaymentCycleMonths(n.getRepaymentCycleMonths());c.setFirstDueDate(date(n.getFirstDueDate()));}
        if("flat".equals(n.getRepaymentMethod()))c.setFixedInterestAmount(money(n.getFixedInterestAmount()));return c;
    }
    public Map<String,Object> preview(long sysId,long id){IouUser u=person(sysId,false);IouNote n=note(id,false);participant(n,u);return previewView(n,u);}
    private Map<String,Object> baseTerms(IouNote n){return map("templateVersion","handwritten-v1","calculationVersion",n.getCalculationVersion(),"interestRule",n.getInterestRule(),"noteNo",n.getNoteNo(),"conditions",conditions(n),"plan",planView(rows(n.getId())),"principalAmount",money(n.getPrincipalAmount()),"interestAmount",money(n.getInterestAmount()),"totalAmount",money(n.getTotalAmount()),"serviceFeePolicy","DISABLED","initiatorName",Integer.valueOf(1).equals(n.getCreatorRole())?n.getBorrowerName():n.getLenderName());}
    private String termsHash(IouNote n){return n.getTermsHash()==null?IouAgreementStore.hash(encode(baseTerms(n))):n.getTermsHash();}
    private String previewHash(IouNote n,IouUser u){return IouAgreementStore.hash(termsHash(n)+":"+u.getIdNoHash()+":"+u.getRealName()+":"+n.getRowVersion());}
    private Map<String,Object> previewView(IouNote n,IouUser u){return map("noteId",n.getId().toString(),"version",String.valueOf(n.getRowVersion()),"previewHash",previewHash(n,u),"termsHash",termsHash(n),"terms",baseTerms(n),"borrowerName",n.getBorrowerName(),"lenderName",n.getLenderName(),"joiningName",u.getRealName(),"actions",actions(n,u));}

    /** Atomic local signing: signature files plus evidence are created before the single DB commit. */
    @Transactional public Map<String,Object> sign(long sysId,long id,String version,String hash,String key,String reference,String signature,boolean confirmed,String remote,String agent){
        if(!options.canSign())fail(503,"CONFIG_NOT_READY","签署尚未开放");validateKey(key);if(!confirmed)fail(400,"CONFIRMATION_REQUIRED","请核对条款与还款计划");
        IouUser u=person(sysId,true);IouNote n=note(id,true);boolean initiator=u.getId().equals(n.getCreatorUserId());
        if(!initiator){checkReference(reference,id);if(u.getIdNoHash().equals(initiatorHash(n)))fail(403,"SELF_SIGN_NOT_ALLOWED","本人不能签署另一方");}
        if(n.getRepaymentMethod()==null)fail(409,"LEGACY_NOTE","历史借条不支持当前签署流程");
        boolean borrower=initiator?Integer.valueOf(1).equals(n.getCreatorRole()):!Integer.valueOf(1).equals(n.getCreatorRole());
        Integer status=borrower?n.getBorrowerSignStatus():n.getLenderSignStatus();
        String priorKey=borrower?n.getBorrowerSignRequestId():n.getLenderSignRequestId();
        byte[] png=IouAgreementStore.signature(signature);String signHash=IouAgreementStore.hash(png);
        Map<String,Object> context=n.getActiveSignContext()==null?new TreeMap<>():decode(n.getActiveSignContext());
        String side=borrower?"borrower":"lender";
        if(Integer.valueOf(2).equals(status)) {
            String bound=borrower?n.getBorrowerIdNoHash():n.getLenderIdNoHash();
            Map<String,Object> prior=context.get(side) instanceof Map?(Map<String,Object>)context.get(side):Collections.emptyMap();
            if(u.getIdNoHash().equals(bound)&&key.equals(priorKey)&&signHash.equals(prior.get("signatureHash"))&&Objects.equals(hash,prior.get("previewHash")))return view(n,u);
            fail(409,"NOTE_ALREADY_SIGNED","本方已经签署或借条已由其他人签署");
        }
        if(initiator){owner(n,u);editable(n);}else if(!Integer.valueOf(7).equals(n.getStatus())||!Integer.valueOf(2).equals(Integer.valueOf(1).equals(n.getCreatorRole())?n.getBorrowerSignStatus():n.getLenderSignStatus()))fail(409,"NOTE_NOT_SIGNABLE","借条当前不能签署");
        version(n,version);if(hash==null||!previewHash(n,u).equals(hash))fail(409,"PREVIEW_CHANGED","预览已变化，请重新核对");
        String initiatorName=Integer.valueOf(1).equals(n.getCreatorRole())?n.getBorrowerName():n.getLenderName();
        if(initiator&&(!u.getIdNoHash().equals(initiatorHash(n))||!u.getRealName().equals(initiatorName)))fail(409,"IDENTITY_CHANGED","本人身份已变化，请更新草稿或重新发起");
        IouRepaymentCalculator.Quote verified=calculator.calculate(conditions(n));
        if(!IouRepaymentCalculator.VERSION.equals(n.getCalculationVersion())
            ||!encode(planView(verified.getPlan())).equals(encode(planView(rows(id))))
            ||!verified.getSummary().get("interestAmount").equals(money(n.getInterestAmount()))
            ||!verified.getSummary().get("totalAmount").equals(money(n.getTotalAmount())))
            fail(409,"INVALID_PLAN","已保存条件与计划不一致，请重新核对");
        Timestamp now=new Timestamp(System.currentTimeMillis());
        if(initiator){n.setTermsSnapshot(encode(baseTerms(n)));n.setTermsHash(IouAgreementStore.hash(n.getTermsSnapshot()));n.setTermsLockedAt(now);}
        else {if(borrower){n.setBorrowerName(u.getRealName());n.setBorrowerIdNoHash(u.getIdNoHash());n.setBorrowerIdNoEncrypted(u.getIdNoEncrypted());}
            else{n.setLenderName(u.getRealName());n.setLenderIdNoHash(u.getIdNoHash());n.setLenderIdNoEncrypted(u.getIdNoEncrypted());}}
        String filePrefix=n.getId()+"/"+UUID.randomUUID().toString();
        String imageFile=store.write(filePrefix+".png",png);
        Map<String,Object> evidence=map("method","HANDWRITTEN","requestId",key,"name",u.getRealName(),"identityHash",u.getIdNoHash(),"identityEncrypted",u.getIdNoEncrypted(),"signedAt",time(now),"termsHash",n.getTermsHash(),"signatureHash",signHash,"signatureFile",imageFile,"previewHash",hash,"remoteAddress",limit(remote,128),"userAgent",limit(agent,512));
        String evidenceText=encode(evidence);String evidenceSeal=store.seal("iou-handwritten-evidence-v1:"+evidenceText);
        String evidenceFile=store.write(filePrefix+".json",encode(map("evidence",evidence,"seal",evidenceSeal)).getBytes(StandardCharsets.UTF_8));
        context.put(side,evidence);n.setActiveSignContext(encode(context));
        String html=render(n,context,false);String signedFile=store.write(filePrefix+".html",html.getBytes(StandardCharsets.UTF_8));
        if(borrower){n.setBorrowerSignStatus(2);n.setBorrowerSignedAt(now);n.setBorrowerSignRequestId(key);n.setBorrowerSignEvidenceId(evidenceFile);n.setBorrowerSignFile(signedFile);n.setBorrowerSignFileHash(IouAgreementStore.hash(html));}
        else{n.setLenderSignStatus(2);n.setLenderSignedAt(now);n.setLenderSignRequestId(key);n.setLenderSignEvidenceId(evidenceFile);n.setLenderSignFile(signedFile);n.setLenderSignFileHash(IouAgreementStore.hash(html));}
        if(initiator)n.setStatus(7);
        else{String finalHtml=render(n,context,true);n.setAgreementFile(store.write(filePrefix+"-final.html",finalHtml.getBytes(StandardCharsets.UTF_8)));n.setAgreementHash(IouAgreementStore.hash(finalHtml));n.setStatus(2);n.setEffectiveAt(now);}
        n.setRowVersion(n.getRowVersion()+1);if(notes.updateById(n)!=1)throw new IllegalStateException("签署保存失败");return view(n,u);
    }
    private String render(IouNote n,Map<String,Object> context,boolean complete){
        StringBuilder h=new StringBuilder("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>借条</title><style>body{max-width:800px;margin:24px auto;padding:20px;font:16px/1.8 sans-serif}table{width:100%;border-collapse:collapse}td,th{border:1px solid #ccc;padding:6px}img{max-width:280px}pre{white-space:pre-wrap;overflow-wrap:anywhere}h1{text-align:center}</style></head><body><h1>借条</h1>");
        h.append("<p>编号：").append(IouAgreementStore.escape(n.getNoteNo())).append("</p><p>借款人：").append(IouAgreementStore.escape(n.getBorrowerName()==null?"待实名签署者加入":n.getBorrowerName())).append("；出借人：").append(IouAgreementStore.escape(n.getLenderName()==null?"待实名签署者加入":n.getLenderName())).append("</p>");
        h.append("<p>本金人民币 ").append(money(n.getPrincipalAmount())).append(" 元，借款日期 ").append(date(n.getLoanDate())).append("，最终还款日期 ").append(date(n.getDueDate())).append("。借款用途：").append(IouAgreementStore.escape(n.getPurpose())).append("。</p>");
        h.append("<p>本息合计 ").append(money(n.getTotalAmount())).append(" 元，总利息 ").append(money(n.getInterestAmount())).append(" 元。还款方式：").append(IouAgreementStore.escape(n.getRepaymentMethod())).append("，年利率比例：").append(n.getAnnualRate().toPlainString()).append("。固定总利息方式以约定总利息分配，各期不按余额计算利息。</p>");
        h.append("<p>标准周期的等额本息/等额本金按年利率×周期月数/12计息，非整周期首期及其他按天方式按实际天数/365计息；包含起日、不含止日，金额四舍五入至分，最后一期调整尾差。规则版本：").append(IouAgreementStore.escape(n.getCalculationVersion())).append("。</p>");
        h.append("<p>提前还款约定：").append("actual".equals(n.getPrepaymentRule())?"允许提前还款，按双方约定重算利息；固定总利息减免须双方确认。":"提前结清金额由双方协商确认。")
            .append("逾期约定：").append(IouAgreementStore.escape(n.getOverdueTerms())).append("。其他约定：").append(IouAgreementStore.escape(n.getRemark())).append("。</p>");
        h.append("<p>本期平台服务费为0元。签署是借款条款确认，不表示平台已放款或已确认实际资金交付。发起人先签署固定条款，另一位实名用户确认并手写签署后加入；另一方不得修改条件，不需要发起人再次确认。</p><table><tr><th>期次</th><th>日期</th><th>本金</th><th>利息</th><th>合计</th></tr>");
        for(IouNotePlan p:rows(n.getId()))h.append("<tr><td>").append(p.getPeriodNo()).append("</td><td>").append(p.getDueDate()).append("</td><td>").append(money(p.getPrincipalAmount())).append("</td><td>").append(money(p.getInterestAmount())).append("</td><td>").append(money(p.getTotalAmount())).append("</td></tr>");h.append("</table>");
        for(String side:Arrays.asList("borrower","lender"))if(context.get(side) instanceof Map){Map<String,Object>e=(Map<String,Object>)context.get(side);byte[] image=store.read((String)e.get("signatureFile"),(String)e.get("signatureHash"));h.append("<p>").append("borrower".equals(side)?"借款人":"出借人").append("：").append(IouAgreementStore.escape((String)e.get("name"))).append("；签署时间：").append(IouAgreementStore.escape((String)e.get("signedAt"))).append("</p><img alt=\"手写签名\" src=\"data:image/png;base64,").append(Base64.getEncoder().encodeToString(image)).append("\">");}
        h.append("<p>锁定条款SHA256：").append(n.getTermsHash()).append("。</p>");
        if(complete){String original=Integer.valueOf(1).equals(n.getCreatorRole())?n.getBorrowerSignFileHash():n.getLenderSignFileHash();h.append("<p>本文件为原签借款条件与另一方加入的最终记录；原签文件SHA256：").append(original).append("。原签文件保持原样保存。</p>");}
        else if(context.containsKey("borrower")&&context.containsKey("lender"))h.append("<p>另一方已接受原借款条件并手写签署，最终借条另行归档；发起人原签文件保持原样保存。</p>");
        else h.append("<p>发起人已签借款条件，另一方尚待加入；不是双方已完成的借条。</p>");
        return h.append("</body></html>").toString();
    }
    public Map<String,Object> current(long sysId,long id,String reference){IouUser u=person(sysId,false);IouNote n=note(id,false);participant(n,u);boolean b=u.getIdNoHash().equals(n.getBorrowerIdNoHash());return map("noteId",String.valueOf(id),"status",n.getStatus(),"stage",stage(n),"version",String.valueOf(n.getRowVersion()),"signStatus",b?n.getBorrowerSignStatus():n.getLenderSignStatus(),"requestId",b?n.getBorrowerSignRequestId():n.getLenderSignRequestId());}
    public Map<String,Object> share(long sysId,long id){IouUser u=person(sysId,false);IouNote n=note(id,false);owner(n,u);if(!Integer.valueOf(7).equals(n.getStatus()))fail(409,"NOTE_NOT_SIGNABLE","当前借条不能邀请签署");String ref=id+"."+store.seal("iou-share-v1:"+id);return map("reference",ref,"shareUrl",options.getShareBaseUrl().replaceAll("/+$","")+"/#/notes/join?reference="+ref,"status",n.getStatus(),"canSign",options.canSign());}
    public Map<String,Object> shared(long sysId,String ref){long id=checkReference(ref,null);IouUser u=person(sysId,false);IouNote n=note(id,false);
        if(u.getIdNoHash().equals(n.getBorrowerIdNoHash())||u.getIdNoHash().equals(n.getLenderIdNoHash()))return previewView(n,u);
        if(!Integer.valueOf(7).equals(n.getStatus()))return map("noteId",String.valueOf(id),"status",n.getStatus(),"stage",stage(n),"actions",Collections.emptyList());
        Map<String,Object>r=previewView(n,u);r.put("joiningRole",Integer.valueOf(1).equals(n.getCreatorRole())?"lender":"borrower");r.put("actions",options.canSign()?Arrays.asList("SIGN_COUNTERPARTY"):Collections.emptyList());return r;
    }
    private long checkReference(String ref,Long expected){
        if(ref==null||!ref.matches("[0-9]{1,19}\\.[a-f0-9]{64}"))fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");
        long id;try{id=Long.parseLong(ref.substring(0,ref.indexOf('.')));}catch(NumberFormatException e){fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");return 0;}
        String provided=ref.substring(ref.indexOf('.')+1), actual=store.seal("iou-share-v1:"+id);
        if(id<=0||expected!=null&&expected!=id||!MessageDigest.isEqual(provided.getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII)))fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");return id;
    }
    @Transactional public Map<String,Object> cancel(long sysId,long id,String version,String reason){IouUser u=person(sysId,false);IouNote n=note(id,true);owner(n,u);
        if(Integer.valueOf(4).equals(n.getStatus()))return view(n,u);version(n,version);
        if(!Arrays.asList(0,7).contains(n.getStatus()))fail(409,"NOTE_NOT_CANCELLABLE","当前借条不能撤销");
        if(Integer.valueOf(1).equals(n.getBorrowerSignStatus())||Integer.valueOf(1).equals(n.getLenderSignStatus()))fail(409,"SIGNING_IN_PROGRESS","请等待签署结果");
        if(reason!=null&&reason.length()>255)fail(400,"INVALID_CONDITIONS","取消原因最多255字");n.setStatus(4);n.setCancelReason(reason);n.setRowVersion(n.getRowVersion()+1);notes.updateById(n);return view(n,u);
    }
    public byte[] agreement(long sysId,long id){IouUser u=person(sysId,false);IouNote n=note(id,false);participant(n,u);if(n.getAgreementFile()==null)fail(409,"AGREEMENT_NOT_READY","最终借条尚未生成");return store.read(n.getAgreementFile(),n.getAgreementHash());}
    private List<String> actions(IouNote n,IouUser u){List<String>a=new ArrayList<>();a.add("VIEW");if(n.getRepaymentMethod()==null)return a;if(u.getId().equals(n.getCreatorUserId())){if(Integer.valueOf(0).equals(n.getStatus())&&n.getTermsLockedAt()==null){a.addAll(Arrays.asList("EDIT","DELETE","PREVIEW"));if(options.canSign())a.add("SIGN_INITIATOR");}if(Arrays.asList(0,7).contains(n.getStatus()))a.add("CANCEL");if(Integer.valueOf(7).equals(n.getStatus()))a.add("SHARE");}if(n.getAgreementFile()!=null&&n.getAgreementHash()!=null)a.add("VIEW_AGREEMENT");return a;}
    public static String stage(IouNote n){if(Integer.valueOf(1).equals(n.getStatus()))return "LEGACY_WAITING_CONFIRMATION";if(Integer.valueOf(5).equals(n.getStatus()))return "REJECTED";if(Integer.valueOf(4).equals(n.getStatus()))return "CANCELLED";if(Integer.valueOf(2).equals(n.getStatus()))return "EFFECTIVE";if(Integer.valueOf(3).equals(n.getStatus()))return "SETTLED";if(Integer.valueOf(6).equals(n.getStatus()))return "WAITING_FEE";if(Integer.valueOf(7).equals(n.getStatus()))return "WAITING_COUNTERPARTY";return n.getTermsLockedAt()==null?"DRAFT_EDITABLE":"TERMS_LOCKED";}
    private void enabled(){if(!options.isCreationEnabled())fail(503,"CONFIG_NOT_READY","发起借条暂未开放");}
    private IouNote note(long id,boolean lock){QueryWrapper<IouNote>q=new QueryWrapper<IouNote>().eq("id",id);if(lock)q.last("FOR UPDATE");IouNote n=notes.selectOne(q);if(n==null)fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");return n;}
    private static void owner(IouNote n,IouUser u){if(!u.getId().equals(n.getCreatorUserId()))fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");}
    private static void participant(IouNote n,IouUser u){if(!u.getIdNoHash().equals(n.getBorrowerIdNoHash())&&!u.getIdNoHash().equals(n.getLenderIdNoHash())||Integer.valueOf(0).equals(n.getStatus())&&!u.getId().equals(n.getCreatorUserId()))fail(404,"NOTE_NOT_FOUND","借条不存在或无权查看");}
    private static void editable(IouNote n){if(!Integer.valueOf(0).equals(n.getStatus())||n.getTermsLockedAt()!=null||n.getBorrowerSignedAt()!=null||n.getLenderSignedAt()!=null||Integer.valueOf(1).equals(n.getBorrowerSignStatus())||Integer.valueOf(1).equals(n.getLenderSignStatus()))fail(409,"TERMS_LOCKED","借条已锁定，不能修改");}
    private static void version(IouNote n,String value){if(value==null||!value.equals(String.valueOf(n.getRowVersion())))fail(409,"VERSION_CONFLICT","内容已变化，请重新加载");}
    private static void validateRole(IouNoteConditions c){if(c==null||!Arrays.asList("borrower","lender").contains(c.getCreatorRole()))fail(400,"INVALID_CONDITIONS","请选择本人借款身份");}
    private static void validateKey(String key){if(key==null||!key.matches("[a-zA-Z0-9_-]{16,64}"))fail(400,"INVALID_IDEMPOTENCY_KEY","请提供有效操作编号");}
    private static String initiatorHash(IouNote n){return Integer.valueOf(1).equals(n.getCreatorRole())?n.getBorrowerIdNoHash():n.getLenderIdNoHash();}
    private String encode(Object o){try{return json.writeValueAsString(o);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("内容编码失败");}}
    private Map<String,Object> decode(String s){try{return json.readValue(s,Map.class);}catch(java.io.IOException e){throw new IllegalStateException("签署上下文不可读");}}
    private static String money(BigDecimal b){return b==null?null:b.setScale(2).toPlainString();}
    private static Timestamp day(String d){return d==null?null:Timestamp.valueOf(LocalDate.parse(d).atStartOfDay());}
    private static String date(Timestamp t){return t==null?null:t.toLocalDateTime().toLocalDate().toString();}
    private static String time(Timestamp t){return t==null?null:t.toLocalDateTime().atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString();}
    private static String limit(String s,int max){return s==null?"":s.substring(0,Math.min(max,s.length()));}
    private static Map<String,Object> map(Object... values){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
    private static void fail(int status,String code,String message){throw new IouBusinessException(status,code,message);}
}
