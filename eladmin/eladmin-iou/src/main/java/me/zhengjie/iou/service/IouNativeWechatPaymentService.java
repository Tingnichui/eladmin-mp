package me.zhengjie.iou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.mapper.IouNoteMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static me.zhengjie.iou.service.IouNativeWechatGateway.map;

/** One current WeChat order on iou_note. No payment/order/attempt table is used. */
@Service
public class IouNativeWechatPaymentService {
    private static final DefaultRedisScript<String> CONSUME=new DefaultRedisScript<>("local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]); end; return v",String.class);
    private final IouNoteMapper notes;
    private final IouNoteBusinessService business;
    private final IouNativeWechatOptions options;
    private final IouNativeWechatGateway gateway;
    private final StringRedisTemplate redis;
    private final TransactionTemplate tx;
    public IouNativeWechatPaymentService(IouNoteMapper notes,IouNoteBusinessService business,IouNativeWechatOptions options,IouNativeWechatGateway gateway,StringRedisTemplate redis,PlatformTransactionManager manager){
        this.notes=notes;this.business=business;this.options=options;this.gateway=gateway;this.redis=redis;tx=new TransactionTemplate(manager);
    }
    public Map<String,Object> create(long sysId,long id){
        options.requireReady();IouUser user=business.person(sysId,false);
        return tx.execute(s->{IouNote n=note(id,true);borrower(n,user);if(paid(n))return view(n,user);payable(n);
            if(n.getPaymentOrderNo()==null||Integer.valueOf(3).equals(n.getServiceFeePayStatus())){
                Map<String,Object> context=context(n),pay=new LinkedHashMap<>();String order="WX"+IouNativeWechatGateway.random().substring(0,30);
                long now=System.currentTimeMillis();pay.put("orderNo",order);pay.put("appId",options.getAppId());pay.put("merchantId",options.getMerchantId());
                pay.put("createdAt",now);pay.put("expiresAt",now+options.getExpireMinutes()*60000L);pay.put("submitted",false);
                context.put("payment",pay);n.setActiveSignContext(gateway.encode(context));n.setPaymentOrderNo(order);n.setServiceFeePayStatus(0);save(n);
            }
            return view(n,user);
        });
    }
    public Map<String,Object> detail(long sysId,String number){
        IouUser user=business.person(sysId,false);IouNote n=read(number,user);Map<String,Object> pay=payment(n);
        if(!paid(n)&&options.ready()&&Boolean.TRUE.equals(pay.get("submitted"))&&longValue(pay.get("checkedAt"))<System.currentTimeMillis()-3000){
            try{reconcile(number);}catch(IouBusinessException ignored){/* Unknown results remain pending. */}n=read(number,user);
        }
        return view(n,user);
    }
    public Map<String,Object> authorize(long sysId,String number){
        options.requireReady();IouUser user=business.person(sysId,false);IouNote n=read(number,user);borrower(n,user);payable(n);currentMerchant(n);
        String state=IouNativeWechatGateway.random();
        cache("oauth:"+state,gateway.encode(map("noteId",n.getId(),"orderNo",number,"userId",user.getId())),600);
        return map("authorizationUrl","https://open.weixin.qq.com/connect/oauth2/authorize?appid="+options.getAppId()+"&redirect_uri="+url(options.getOauthCallbackUrl())+"&response_type=code&scope=snsapi_base&state="+state+"#wechat_redirect");
    }
    public String oauthCallback(String code,String state){
        options.requireReady();if(state==null||!state.matches("[a-f0-9]{32}")||code==null||code.length()>256)throw error(400,"WECHAT_STATE_INVALID","微信授权失效，请返回重新授权");
        String raw;
        try{raw=redis.execute(CONSUME,Collections.singletonList(key("oauth:"+state)));}catch(Exception ignored){throw cacheUnavailable();}
        if(raw==null)throw error(400,"WECHAT_STATE_INVALID","微信授权已使用或已过期");Map<String,Object> grant=gateway.decode(raw);
        String openid=gateway.oauthOpenid(code);
        tx.execute(s->{IouNote n=note(Long.parseLong(grant.get("noteId").toString()),true);payable(n);currentMerchant(n);
            if(!Objects.equals(n.getPaymentOrderNo(),grant.get("orderNo")))throw error(409,"PAYMENT_ORDER_CHANGED","支付订单已更新，请重新打开签署页面");
            Map<String,Object> pending=pending(n),pay=payment(n);
            if(!Objects.equals(pending.get("userId").toString(),grant.get("userId").toString()))throw error(403,"PAYMENT_OWNER_MISMATCH","授权人与待签署人不一致");
            if(Boolean.TRUE.equals(pay.get("submitted"))&&pay.get("openid")!=null&&!openid.equals(pay.get("openid")))throw error(409,"WECHAT_PAYER_CHANGED","请使用原微信账号继续支付");
            pay.put("openid",openid);persistPayment(n,pay);save(n);return null;
        });
        return options.getH5Origin().replaceAll("/+$","")+"/#/notes/payment?noteId="+grant.get("noteId")+"&orderNo="+grant.get("orderNo")+"&start=true";
    }
    public Map<String,Object> payment(long sysId,String number){
        options.requireReady();IouUser user=business.person(sysId,false);IouNote before=read(number,user);borrower(before,user);
        if(paid(before))return map("action","PAID","order",view(before,user));
        if(Boolean.TRUE.equals(payment(before).get("submitted")))reconcile(number);
        Map<String,Object> command=new HashMap<>();
        Map<String,Object> immediate=tx.execute(s->{
            IouNote n=note(before.getId(),true);borrower(n,user);if(!number.equals(n.getPaymentOrderNo()))throw error(409,"PAYMENT_ORDER_CHANGED","支付订单已更新");
            if(paid(n))return map("action","PAID","order",view(n,user));payable(n);currentMerchant(n);Map<String,Object> pay=payment(n);
            if(Integer.valueOf(3).equals(n.getServiceFeePayStatus()))return map("action","QUERY","order",view(n,user));
            if(pay.get("openid")==null)return map("action","AUTHORIZE","order",view(n,user));
            String prepay=cached("prepay:"+number);
            if(prepay!=null&&longValue(pay.get("expiresAt"))>System.currentTimeMillis())return map("action","INVOKE","order",view(n,user),"payParams",gateway.payParams(prepay));
            if(longValue(pay.get("expiresAt"))<=System.currentTimeMillis()||longValue(pay.get("submittingAt"))>System.currentTimeMillis()-15000)return map("action","QUERY","order",view(n,user));
            pay.put("submitted",true);pay.put("submittingAt",System.currentTimeMillis());n.setServiceFeePayStatus(1);persistPayment(n,pay);save(n);
            command.put("openid",pay.get("openid"));command.put("expiresAt",pay.get("expiresAt"));command.put("amount",cents(n.getServiceFee()));return null;
        });
        if(immediate!=null)return immediate;
        // The order number is committed before the remote call. All uncertain retries keep this number and parameters.
        String prepay;
        try{prepay=gateway.prepay(number,(Long)command.get("amount"),(String)command.get("openid"),Instant.ofEpochMilli(longValue(command.get("expiresAt"))).atOffset(ZoneOffset.ofHours(8)).toString());}
        catch(IouBusinessException e){if("ORDERPAID".equals(e.getCode())||"OUT_TRADE_NO_USED".equals(e.getCode())){reconcile(number);return map("action",paid(read(number,user))?"PAID":"QUERY","order",view(read(number,user),user));}throw e;}
        long seconds=Math.max(1,(longValue(command.get("expiresAt"))-System.currentTimeMillis())/1000);cache("prepay:"+number,prepay,Math.min(seconds,7000));
        return map("action","INVOKE","order",view(read(number,user),user),"payParams",gateway.payParams(prepay));
    }
    public void notify(HttpHeaders headers,String raw){Map<String,Object> result=gateway.notification(headers,raw);String number=String.valueOf(result.get("out_trade_no"));
        IouNote n=notes.selectOne(new QueryWrapper<IouNote>().eq("payment_order_no",number));
        if(n==null)return; // An old number can only have been replaced after trusted closure.
        applyResult(n.getId(),number,result);
    }
    private void reconcile(String number){
        IouNote n=notes.selectOne(new QueryWrapper<IouNote>().eq("payment_order_no",number));if(n==null||paid(n))return;currentMerchant(n);
        Map<String,Object> result;
        try{result=gateway.query(number);}catch(IouBusinessException e){if("ORDER_NOT_EXIST".equals(e.getCode()))return;throw e;}
        applyResult(n.getId(),number,result);
        IouNote latest=note(n.getId(),false);
        if(number.equals(latest.getPaymentOrderNo())&&!paid(latest)&&"NOTPAY".equals(result.get("trade_state"))&&longValue(payment(latest).get("expiresAt"))<System.currentTimeMillis()){
            gateway.close(number);applyResult(n.getId(),number,gateway.query(number)); // Require confirmed CLOSED before replacing the order.
        }
    }
    private void applyResult(long id,String number,Map<String,Object> result){
        tx.execute(s->{IouNote n=note(id,true);if(!number.equals(n.getPaymentOrderNo()))return null;Map<String,Object> pay=payment(n);validate(n,pay,result);
            if(paid(n))return null;String state=String.valueOf(result.get("trade_state"));pay.put("checkedAt",System.currentTimeMillis());
            if("SUCCESS".equals(state)){
                payable(n);n.setServiceFeePayStatus(2);pay.put("transactionId",result.get("transaction_id"));pay.put("successTime",result.get("success_time"));persistPayment(n,pay);
                business.completePaidBorrower(n);n.setRowVersion(n.getRowVersion()+1);save(n);
            }else{if("CLOSED".equals(state))n.setServiceFeePayStatus(3);persistPayment(n,pay);save(n);}return null;
        });
    }
    private void validate(IouNote n,Map<String,Object> pay,Map<String,Object> result){
        if(!Objects.equals(pay.get("merchantId"),result.get("mchid"))||!Objects.equals(pay.get("appId"),result.get("appid"))||!n.getPaymentOrderNo().equals(result.get("out_trade_no")))throw error(502,"PAYMENT_DATA_MISMATCH","微信订单信息不匹配");
        if("SUCCESS".equals(result.get("trade_state"))){
            try{Map<?,?> amount=(Map<?,?>)result.get("amount"),payer=(Map<?,?>)result.get("payer");
                if(!"CNY".equals(amount.get("currency"))||cents(n.getServiceFee())!=new BigDecimal(amount.get("total").toString()).longValueExact()
                    ||!Objects.equals(pay.get("openid"),payer.get("openid"))||result.get("transaction_id")==null)throw new IllegalArgumentException();
            }catch(Exception ignored){throw error(502,"PAYMENT_DATA_MISMATCH","微信付款金额或付款人不匹配");}
        }
    }
    @Scheduled(fixedDelayString="${iou.wechat-payment.reconcile-delay-ms:60000}",initialDelay=60000)
    public void reconcilePending(){
        if(!options.ready())return;
        List<IouNote> list=notes.selectList(new QueryWrapper<IouNote>().eq("service_fee_pay_status",1).isNotNull("payment_order_no").orderByAsc("update_time","id").last("LIMIT 20"));
        for(IouNote n:list){
            try{reconcile(n.getPaymentOrderNo());}catch(Exception ignored){/* Keep pending; provider errors must not mark the order failed. */}
            // Advance the scan even for unavailable/nonexistent provider orders, without changing financial state.
            notes.update(null,new UpdateWrapper<IouNote>().eq("id",n.getId()).eq("payment_order_no",n.getPaymentOrderNo()).eq("service_fee_pay_status",1).set("update_time",new Timestamp(System.currentTimeMillis())));
        }
    }
    private Map<String,Object> view(IouNote n,IouUser user){Map<String,Object> pay=payment(n);int state=n.getServiceFeePayStatus()==null?0:n.getServiceFeePayStatus();
        return map("orderNo",n.getPaymentOrderNo(),"noteId",n.getId().toString(),"amount",n.getServiceFee().toPlainString(),"status",state==2?"PAID":state==3?"CLOSED":"UNPAID",
            "payStatus",state,"canPay",!paid(n)&&Integer.valueOf(6).equals(n.getStatus())&&user.getIdNoHash().equals(n.getBorrowerIdNoHash())&&options.ready(),
            "authorized",pay.get("openid")!=null,"attemptStatus",state==2?"SUCCESS":state==3?"CLOSED":Boolean.TRUE.equals(pay.get("submitted"))?"PENDING":null,
            "resumePath",resume(n));
    }
    private String resume(IouNote n){Map<String,Object> pending=pending(n);Object ref=pending.get("shareReference");return ref==null?"/notes/create?draft="+n.getId():"/notes/join?reference="+url(ref.toString());}
    private IouNote read(String number,IouUser user){if(number==null||!number.matches("WX[a-f0-9]{30}"))throw error(404,"PAYMENT_NOT_FOUND","支付订单不存在");IouNote n=notes.selectOne(new QueryWrapper<IouNote>().eq("payment_order_no",number));
        if(n==null||!user.getIdNoHash().equals(n.getBorrowerIdNoHash())&&!user.getIdNoHash().equals(n.getLenderIdNoHash()))throw error(404,"PAYMENT_NOT_FOUND","支付订单不存在或无权查看");return n;}
    private IouNote note(long id,boolean lock){QueryWrapper<IouNote> q=new QueryWrapper<IouNote>().eq("id",id);if(lock)q.last("FOR UPDATE");IouNote n=notes.selectOne(q);if(n==null)throw error(404,"NOTE_NOT_FOUND","借条不存在");return n;}
    private Map<String,Object> context(IouNote n){return n.getActiveSignContext()==null?new LinkedHashMap<>():gateway.decode(n.getActiveSignContext());}
    private Map<String,Object> payment(IouNote n){Object p=context(n).get("payment");return p instanceof Map?(Map<String,Object>)p:new LinkedHashMap<>();}
    private Map<String,Object> pending(IouNote n){Object p=context(n).get("pendingBorrower");return p instanceof Map?(Map<String,Object>)p:new LinkedHashMap<>();}
    private void persistPayment(IouNote n,Map<String,Object> pay){Map<String,Object> c=context(n);c.put("payment",pay);n.setActiveSignContext(gateway.encode(c));}
    private void currentMerchant(IouNote n){Map<String,Object> p=payment(n);if(!Objects.equals(p.get("merchantId"),options.getMerchantId())||!Objects.equals(p.get("appId"),options.getAppId()))throw error(409,"PAYMENT_CONFIG_CHANGED","请使用原商户配置处理当前订单");}
    private void borrower(IouNote n,IouUser user){if(!user.getIdNoHash().equals(n.getBorrowerIdNoHash()))throw error(403,"BORROWER_PAYMENT_REQUIRED","仅借款方可以签署付款");}
    private boolean paid(IouNote n){return Integer.valueOf(2).equals(n.getServiceFeePayStatus());}
    private void payable(IouNote n){if(!Integer.valueOf(6).equals(n.getStatus())||!Integer.valueOf(1).equals(n.getBorrowerSignStatus())||!business.requiresPayment(n)||pending(n).isEmpty())throw error(409,"NOTE_NOT_PAYABLE","借条没有待付款签署");}
    private void save(IouNote n){n.setUpdateTime(null);if(notes.updateById(n)!=1)throw new IllegalStateException("签署付款状态保存失败");}
    private String key(String suffix){return "iou:wechat-payment:"+options.getMerchantId()+":"+suffix;}
    private String cached(String suffix){try{return redis.opsForValue().get(key(suffix));}catch(Exception ignored){throw cacheUnavailable();}}
    private void cache(String suffix,String value,long seconds){try{redis.opsForValue().set(key(suffix),value,seconds,TimeUnit.SECONDS);}catch(Exception ignored){throw cacheUnavailable();}}
    private static long longValue(Object value){return value==null?0:Long.parseLong(value.toString());}
    private static long cents(BigDecimal value){try{return value.movePointRight(2).longValueExact();}catch(Exception ignored){throw error(400,"INVALID_FEE","服务费金额无效");}}
    private static String url(String value){try{return URLEncoder.encode(value,StandardCharsets.UTF_8.name());}catch(Exception e){throw new IllegalStateException(e);}}
    private static IouBusinessException error(int status,String code,String message){return new IouBusinessException(status,code,message);}
    private static IouBusinessException cacheUnavailable(){return error(503,"PAYMENT_CACHE_UNAVAILABLE","微信支付会话暂不可用，请稍后重试");}
}
