package me.zhengjie.iou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.mapper.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static me.zhengjie.iou.service.IouHuifuGateway.map;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix="iou.payment",name="huifu-legacy-enabled",havingValue="true")
public class IouFeePaymentService {
    private final IouNoteMapper notes;
    private final IouFeeOrderMapper orders;
    private final IouPaymentAttemptMapper attempts;
    private final IouNoteBusinessService business;
    private final IouPaymentOptions options;
    private final IouHuifuGateway gateway;
    private final TransactionTemplate tx;
    public IouFeePaymentService(IouNoteMapper notes, IouFeeOrderMapper orders, IouPaymentAttemptMapper attempts,
            IouNoteBusinessService business, IouPaymentOptions options, IouHuifuGateway gateway, PlatformTransactionManager manager) {
        this.notes=notes; this.orders=orders; this.attempts=attempts; this.business=business; this.options=options; this.gateway=gateway;
        tx=new TransactionTemplate(manager);
    }
    public Map<String,Object> create(long sysId, long noteId) {
        options.requireReady(); IouUser user=business.person(sysId,false);
        return tx.execute(status -> {
            IouNote note=note(noteId,true); borrower(note,user);
            IouFeeOrder order=orders.selectOne(new QueryWrapper<IouFeeOrder>().eq("note_id",noteId));
            if(order==null) {
                payable(note);
                order=new IouFeeOrder();order.setOrderNo("SF"+random());order.setNoteId(noteId);order.setBorrowerUserId(user.getId());
                order.setAmount(note.getServiceFee());order.setStatus("UNPAID");order.setFeeSnapshot(note.getTermsSnapshot());order.setCreateTime(now());
                requireWrite(orders.insert(order));
            }
            return view(order,user);
        });
    }
    public Map<String,Object> detail(long sysId,String orderNo) {
        IouUser user=business.person(sysId,false);IouFeeOrder order=read(orderNo,user);
        IouPaymentAttempt attempt=latest(order.getId());
        if(!"PAID".equals(order.getStatus())&&attempt!=null&&active(attempt)&&options.ready()
                &&(attempt.getCheckedAt()==null||attempt.getCheckedAt().getTime()<System.currentTimeMillis()-3000)) {
            try { reconcile(attempt); } catch(IouBusinessException ignored) { /* Preserve pending state on upstream outage. */ }
            order=read(orderNo,user);
        }
        return view(order,user);
    }
    public Map<String,Object> authorize(long sysId,String orderNo) {
        options.requireReady();IouUser user=business.person(sysId,false);
        return tx.execute(status -> {
            IouFeeOrder initial=read(orderNo,user);IouNote note=note(initial.getNoteId(),true);borrower(note,user);payable(note);
            IouFeeOrder order=lockedOrder(initial.getId());
            if("PAID".equals(order.getStatus()))throw error(409,"FEE_ALREADY_PAID","服务费已支付");
            IouPaymentAttempt attempt=latest(order.getId());
            if(attempt!=null&&active(attempt))throw error(409,"PAYMENT_PENDING","请先查询上次支付结果");
            String state=random();Timestamp expiry=new Timestamp(System.currentTimeMillis()+600000);
            requireWrite(orders.update(null,new UpdateWrapper<IouFeeOrder>().eq("id",order.getId())
                .set("oauth_state_hash",hash(state)).set("oauth_expires_at",expiry).set("oauth_consumed_at",null).set("payer_openid",null)));
            String url="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+options.getAppId()
                +"&redirect_uri="+url(options.getOauthCallbackUrl())+"&response_type=code&scope=snsapi_base&state="+state+"#wechat_redirect";
            return map("authorizationUrl",url);
        });
    }
    public String oauthCallback(String code,String state) {
        options.requireReady();
        if(code==null||code.length()>256||state==null||!state.matches("[a-f0-9]{32}"))throw error(400,"WECHAT_STATE_INVALID","微信授权已失效，请返回借条重新授权");
        // Consume state before exchanging code. A failed exchange needs a fresh authorization.
        IouFeeOrder consumed=tx.execute(status -> {
            IouFeeOrder found=orders.selectOne(new QueryWrapper<IouFeeOrder>().eq("oauth_state_hash",hash(state)));
            if(found==null)throw error(400,"WECHAT_STATE_INVALID","微信授权已失效，请返回借条重新授权");
            IouNote note=note(found.getNoteId(),true);payable(note);IouFeeOrder order=lockedOrder(found.getId());
            if(order.getOauthConsumedAt()!=null||order.getOauthExpiresAt()==null||order.getOauthExpiresAt().getTime()<System.currentTimeMillis()
                    ||!hash(state).equals(order.getOauthStateHash())||!"UNPAID".equals(order.getStatus()))
                throw error(400,"WECHAT_STATE_INVALID","微信授权已失效，请返回借条重新授权");
            order.setOauthConsumedAt(now());requireWrite(orders.updateById(order));return order;
        });
        String openid=gateway.oauthOpenid(code);
        tx.execute(status -> {
            note(consumed.getNoteId(),true);IouFeeOrder order=lockedOrder(consumed.getId());
            // A newer authorization may have replaced this state during the HTTP exchange.
            if(!hash(state).equals(order.getOauthStateHash()))throw error(409,"WECHAT_STATE_INVALID","微信授权已更新，请重新授权");
            order.setPayerOpenid(openid);requireWrite(orders.updateById(order));return null;
        });
        return options.getH5Origin().replaceAll("/+$","")+"/#/notes/payment?noteId="+consumed.getNoteId()+"&orderNo="+consumed.getOrderNo()+"&start=true";
    }
    public Map<String,Object> payment(long sysId,String orderNo) {
        options.requireReady();IouUser user=business.person(sysId,false);IouFeeOrder before=read(orderNo,user);
        IouPaymentAttempt previous=latest(before.getId());
        if(previous!=null&&active(previous))reconcile(previous);
        IouPaymentAttempt candidate=new IouPaymentAttempt();
        Map<String,Object> result=tx.execute(status -> {
            IouNote note=note(before.getNoteId(),true);borrower(note,user);IouFeeOrder order=lockedOrder(before.getId());
            if("PAID".equals(order.getStatus()))return map("action","PAID","order",view(order,user));
            payable(note);
            IouPaymentAttempt current=latest(order.getId());
            if(current!=null&&active(current))return paymentView(order,current,user);
            if(IouPaymentOptions.blank(order.getPayerOpenid()))return map("action","AUTHORIZE","order",view(order,user));
            candidate.setFeeOrderId(order.getId());candidate.setReqSeqId(random());candidate.setReqDate(IouHuifuGateway.today());
            candidate.setHuifuId(options.getHuifuId());candidate.setStatus("SUBMITTING");candidate.setCreateTime(now());
            candidate.setExpiresAt(new Timestamp(System.currentTimeMillis()+options.getExpireMinutes()*60000L));
            requireWrite(attempts.insert(candidate));return null;
        });
        if(result!=null)return result;
        // SUBMITTING is committed before any provider request. A timeout must be queried using this same request id.
        Map<String,Object> response=gateway.create(read(orderNo,user),candidate);
        tx.execute(status -> {
            IouNote note=note(before.getNoteId(),true);IouFeeOrder order=lockedOrder(before.getId());
            IouPaymentAttempt current=lockedAttempt(candidate.getId());
            if(!active(current)||"PAID".equals(order.getStatus()))return null;
            validateResponse(order,current,response,false);
            if(response.get("hf_seq_id")!=null)current.setHfSeqId(String.valueOf(response.get("hf_seq_id")));
            // Acceptance alone is not success. Always query before granting the paid service.
            if(response.get("pay_info") instanceof String) {
                payParameters((String)response.get("pay_info"));current.setPayInfo((String)response.get("pay_info"));current.setStatus("READY");
            }
            else current.setStatus("PENDING");
            requireWrite(attempts.updateById(current));return null;
        });
        return paymentView(read(orderNo,user),latest(before.getId()),user);
    }
    public String notify(String raw,String sign) {
        Map<String,Object> data=gateway.notification(raw,sign);
        String seq=String.valueOf(data.get("req_seq_id"));
        IouPaymentAttempt initial=attempts.selectOne(new QueryWrapper<IouPaymentAttempt>().eq("req_seq_id",seq));
        if(initial==null)throw error(400,"PAYMENT_ORDER_UNKNOWN","支付订单不存在");
        tx.execute(status -> {
            IouFeeOrder first=orders.selectById(initial.getFeeOrderId());note(first.getNoteId(),true);
            IouFeeOrder order=lockedOrder(first.getId());IouPaymentAttempt attempt=lockedAttempt(initial.getId());
            validateResponse(order,attempt,data,false);
            // Durable query trigger; do not call remote services within the provider's 5-second callback budget.
            if(active(attempt))requireWrite(attempts.update(null,new UpdateWrapper<IouPaymentAttempt>().eq("id",attempt.getId()).set("checked_at",new Timestamp(0))));
            return null;
        });
        return "RECV_ORD_ID_"+seq;
    }
    private void reconcile(IouPaymentAttempt initial) {
        if(!active(initial))return;
        Map<String,Object> response=gateway.query(initial);
        IouFeeOrder first=orders.selectById(initial.getFeeOrderId());
        tx.execute(status -> {
            IouNote note=note(first.getNoteId(),true);IouFeeOrder order=lockedOrder(first.getId());IouPaymentAttempt attempt=lockedAttempt(initial.getId());
            validateResponse(order,attempt,response,true);
            if("SUCCESS".equals(attempt.getStatus()))return null;
            String state=String.valueOf(response.get("trans_stat"));
            attempt.setCheckedAt(now());
            if("S".equals(state)) {
                if(!"UNPAID".equals(order.getStatus())&& !"PAID".equals(order.getStatus()))throw error(409,"PAYMENT_STATE_CONFLICT","支付状态需要核对");
                payableOrPaid(note);
                attempt.setStatus("SUCCESS");order.setStatus("PAID");
                if(order.getPaidAt()==null)order.setPaidAt(paymentTime(response.get("end_time")));
                if(!Integer.valueOf(2).equals(note.getServiceFeePayStatus())){
                    note.setServiceFeePayStatus(2);
                    if(Integer.valueOf(2).equals(note.getLenderSignStatus())){note.setStatus(2);note.setEffectiveAt(now());}
                    else note.setStatus(7);
                    note.setRowVersion(note.getRowVersion()+1);requireWrite(notes.updateById(note));
                }
                requireWrite(orders.updateById(order));
            }
            else if("F".equals(state)&&active(attempt))attempt.setStatus("FAILED");
            if(response.get("hf_seq_id")!=null)attempt.setHfSeqId(String.valueOf(response.get("hf_seq_id")));
            requireWrite(attempts.updateById(attempt));return null;
        });
        IouPaymentAttempt refreshed=attempts.selectById(initial.getId());
        if(active(refreshed)&&refreshed.getExpiresAt().getTime()<System.currentTimeMillis()) {
            Map<String,Object> close=gateway.close(refreshed);
            // Close's trans_stat describes the close operation, not the payment.
            if("S".equals(close.get("trans_stat"))&&"00000000".equals(close.get("resp_code"))
                    &&refreshed.getHuifuId().equals(close.get("huifu_id"))&&refreshed.getReqDate().equals(close.get("org_req_date"))
                    &&(refreshed.getReqSeqId().equals(close.get("org_req_seq_id"))||refreshed.getHfSeqId()!=null&&refreshed.getHfSeqId().equals(close.get("org_hf_seq_id")))) {
                tx.execute(status -> {
                    note(first.getNoteId(),true);lockedOrder(first.getId());IouPaymentAttempt attempt=lockedAttempt(initial.getId());
                    if(active(attempt)){attempt.setStatus("CLOSED");requireWrite(attempts.updateById(attempt));}return null;
                });
            }
        }
    }
    @Scheduled(fixedDelayString="${iou.payment.reconcile-delay-ms:60000}",initialDelay=60000)
    public void reconcilePending() {
        if(!options.ready())return;
        List<IouPaymentAttempt> pending=attempts.selectList(new QueryWrapper<IouPaymentAttempt>().in("status",Arrays.asList("SUBMITTING","PENDING","READY"))
            .and(q -> q.isNull("checked_at").or().lt("checked_at",new Timestamp(System.currentTimeMillis()-10000)))
            .orderByAsc("checked_at").last("LIMIT 20"));
        for(IouPaymentAttempt attempt:pending) {
            try{reconcile(attempt);}catch(Exception ignored){
                // Keep the same request identity; unknown upstream results are never converted into failures.
                attempts.update(null,new UpdateWrapper<IouPaymentAttempt>().eq("id",attempt.getId()).set("checked_at",now()));
            }
        }
    }
    private void validateResponse(IouFeeOrder order,IouPaymentAttempt attempt,Map<String,Object> data,boolean requireAmount) {
        if(!attempt.getHuifuId().equals(data.get("huifu_id"))||!attempt.getReqSeqId().equals(data.get("req_seq_id"))||!attempt.getReqDate().equals(data.get("req_date")))
            throw error(502,"PAYMENT_DATA_MISMATCH","支付订单信息不一致，需核对");
        Object amount=data.get("trans_amt");
        try{if(requireAmount&&amount==null||amount!=null&&order.getAmount().compareTo(new BigDecimal(amount.toString()))!=0)throw new IllegalArgumentException();}
        catch(Exception ignored){throw error(502,"PAYMENT_DATA_MISMATCH","支付金额不一致，需核对");}
        Object type=data.get("trade_type");if(type==null)type=data.get("trans_type");
        if(type!=null&&!"T_JSAPI".equals(type))throw error(502,"PAYMENT_DATA_MISMATCH","支付渠道不一致，需核对");
        if(attempt.getHfSeqId()!=null&&data.get("hf_seq_id")!=null&&!attempt.getHfSeqId().equals(data.get("hf_seq_id")))
            throw error(502,"PAYMENT_DATA_MISMATCH","支付流水不一致，需核对");
    }
    public Map<String,Object> payParameters(String raw) {
        Map<String,Object> data=gateway.decode(raw);Map<String,Object> result=new LinkedHashMap<>();
        for(String key:Arrays.asList("appId","timeStamp","nonceStr","package","signType","paySign")) {
            Object value=data.get(key);if(value==null&&"timeStamp".equals(key))value=data.get("timestamp");
            if(value==null||value.toString().isEmpty())throw error(502,"PAYMENT_PARAMS_INVALID","微信支付参数不完整");
            result.put(key,value.toString());
        }
        if(!options.getAppId().equals(result.get("appId"))||!String.valueOf(result.get("package")).startsWith("prepay_id="))
            throw error(502,"PAYMENT_PARAMS_INVALID","微信支付参数不匹配");
        return result;
    }
    private Map<String,Object> paymentView(IouFeeOrder order,IouPaymentAttempt attempt,IouUser user) {
        Map<String,Object> result=map("order",view(order,user),"action","QUERY");
        if("PAID".equals(order.getStatus()))result.put("action","PAID");
        else if(attempt!=null&&"READY".equals(attempt.getStatus())&&attempt.getExpiresAt().getTime()>System.currentTimeMillis()) {
            result.put("action","INVOKE");result.put("payParams",payParameters(attempt.getPayInfo()));
        }
        return result;
    }
    private Map<String,Object> view(IouFeeOrder order,IouUser user) {
        IouPaymentAttempt attempt=latest(order.getId());
        return map("orderNo",order.getOrderNo(),"noteId",order.getNoteId().toString(),"amount",order.getAmount().toPlainString(),
            "status",order.getStatus(),"paidAt",order.getPaidAt()==null?null:order.getPaidAt().toInstant().toString(),
            "canPay",order.getBorrowerUserId().equals(user.getId())&&"UNPAID".equals(order.getStatus())&&options.ready(),
            "authorized",!IouPaymentOptions.blank(order.getPayerOpenid()),"attemptStatus",attempt==null?null:attempt.getStatus());
    }
    private IouFeeOrder read(String orderNo,IouUser user) {
        if(orderNo==null||!orderNo.matches("SF[a-f0-9]{32}"))throw error(404,"FEE_ORDER_NOT_FOUND","订单不存在或无权查看");
        IouFeeOrder order=orders.selectOne(new QueryWrapper<IouFeeOrder>().eq("order_no",orderNo));
        if(order==null)throw error(404,"FEE_ORDER_NOT_FOUND","订单不存在或无权查看");
        IouNote note=note(order.getNoteId(),false);
        if(!user.getIdNoHash().equals(note.getBorrowerIdNoHash())&&!user.getIdNoHash().equals(note.getLenderIdNoHash()))throw error(404,"FEE_ORDER_NOT_FOUND","订单不存在或无权查看");
        return order;
    }
    private IouNote note(long id,boolean lock) {
        QueryWrapper<IouNote> query=new QueryWrapper<IouNote>().eq("id",id);if(lock)query.last("FOR UPDATE");
        IouNote note=notes.selectOne(query);if(note==null)throw error(404,"NOTE_NOT_FOUND","借条不存在");return note;
    }
    private IouFeeOrder lockedOrder(long id){return orders.selectOne(new QueryWrapper<IouFeeOrder>().eq("id",id).last("FOR UPDATE"));}
    private IouPaymentAttempt lockedAttempt(long id){return attempts.selectOne(new QueryWrapper<IouPaymentAttempt>().eq("id",id).last("FOR UPDATE"));}
    private IouPaymentAttempt latest(long id){return attempts.selectOne(new QueryWrapper<IouPaymentAttempt>().eq("fee_order_id",id).orderByDesc("id").last("LIMIT 1"));}
    private void borrower(IouNote note,IouUser user){if(!user.getIdNoHash().equals(note.getBorrowerIdNoHash()))throw error(403,"BORROWER_PAYMENT_REQUIRED","服务费由借款方支付");}
    private void payable(IouNote note){if(!Integer.valueOf(6).equals(note.getStatus()))throw error(409,"NOTE_NOT_PAYABLE","借条当前无需支付服务费");payableOrPaid(note);}
    private void payableOrPaid(IouNote note){
        boolean signedTogether=Integer.valueOf(2).equals(note.getLenderSignStatus());
        if(!Arrays.asList(6,7,2).contains(note.getStatus())||!Integer.valueOf(2).equals(note.getBorrowerSignStatus())
                ||signedTogether&&(note.getAgreementFile()==null||note.getAgreementHash()==null)
                ||!signedTogether&&(note.getBorrowerSignFile()==null||note.getBorrowerSignFileHash()==null)
                ||note.getServiceFee()==null||note.getServiceFee().signum()<=0)
            throw error(409,"NOTE_NOT_PAYABLE","借款方签署及签署文件尚未完成");
    }
    private static boolean active(IouPaymentAttempt attempt){return Arrays.asList("SUBMITTING","PENDING","READY").contains(attempt.getStatus());}
    private static Timestamp now(){return new Timestamp(System.currentTimeMillis());}
    private static String random(){return UUID.randomUUID().toString().replace("-","");}
    private static String hash(String value){return IouAgreementStore.hash(value);}
    private static String url(String value){try{return URLEncoder.encode(value,StandardCharsets.UTF_8.name());}catch(Exception ignored){throw new IllegalStateException("UTF-8 unavailable");}}
    private static Timestamp paymentTime(Object value){
        try{return Timestamp.from(LocalDateTime.parse(value.toString(),DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).atZone(ZoneId.of("Asia/Shanghai")).toInstant());}
        catch(Exception ignored){return now();}
    }
    private static void requireWrite(int count){if(count!=1)throw new IllegalStateException("支付状态保存失败");}
    private static IouBusinessException error(int status,String code,String message){return new IouBusinessException(status,code,message);}
}
