package me.zhengjie.iou;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.mapper.*;
import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouNativeWechatPaymentTest {
    IouNoteMapper notes;IouNoteBusinessService business;IouNativeWechatOptions options;IouNativeWechatGateway gateway;IouNativeWechatPaymentService service;
    StringRedisTemplate redis;ValueOperations<String,String> values;IouNote note;IouUser actor;Map<String,String> cache;ObjectMapper json=new ObjectMapper();
    @BeforeEach void setup() throws Exception {
        notes=mock(IouNoteMapper.class);business=mock(IouNoteBusinessService.class);options=mock(IouNativeWechatOptions.class);gateway=mock(IouNativeWechatGateway.class);redis=mock(StringRedisTemplate.class);values=mock(ValueOperations.class);
        when(options.ready()).thenReturn(true);when(options.getMerchantId()).thenReturn("1234567890");when(options.getAppId()).thenReturn("wxsynthetic");when(options.getExpireMinutes()).thenReturn(30);
        when(options.getH5Origin()).thenReturn("https://example.test");when(options.getOauthCallbackUrl()).thenReturn("https://example.test/api/iou/payment/wechat/callback");
        when(gateway.encode(any())).thenAnswer(i->json.writeValueAsString(i.getArgument(0)));when(gateway.decode(anyString())).thenAnswer(i->json.readValue((String)i.getArgument(0),Map.class));
        when(gateway.prepay(anyString(),anyLong(),anyString(),anyString())).thenReturn("wxsynthetic-prepay");when(gateway.payParams(anyString())).thenReturn(Collections.singletonMap("package","prepay_id=synthetic"));
        cache=new HashMap<>();when(redis.opsForValue()).thenReturn(values);when(values.get(anyString())).thenAnswer(i->cache.get(i.getArgument(0)));
        doAnswer(i->{cache.put(i.getArgument(0),i.getArgument(1));return null;}).when(values).set(anyString(),anyString(),anyLong(),eq(TimeUnit.SECONDS));
        actor=new IouUser();actor.setId(18L);actor.setIdNoHash("borrower");when(business.person(anyLong(),eq(false))).thenAnswer(i->actor);when(business.requiresPayment(any())).thenReturn(true);
        note=new IouNote();note.setId(91L);note.setCreatorUserId(18L);note.setStatus(6);note.setServiceFee(new BigDecimal("250.00"));note.setServiceFeePayStatus(0);note.setBorrowerSignStatus(1);note.setLenderSignStatus(0);note.setBorrowerIdNoHash("borrower");note.setLenderIdNoHash("lender");note.setRowVersion(1L);
        note.setActiveSignContext("{\"pendingBorrower\":{\"userId\":\"18\",\"identityHash\":\"borrower\"}}");when(notes.selectOne(any())).thenAnswer(i->note);when(notes.updateById(any())).thenReturn(1);
        doAnswer(i->{IouNote n=i.getArgument(0);n.setBorrowerSignStatus(2);n.setStatus(Integer.valueOf(2).equals(n.getLenderSignStatus())?2:7);return null;}).when(business).completePaidBorrower(any());
        PlatformTransactionManager manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        service=new IouNativeWechatPaymentService(notes,business,options,gateway,redis,manager);
    }
    void create(){service.create(18,91);}
    Map<String,Object> pay() throws Exception {return (Map<String,Object>)json.readValue(note.getActiveSignContext(),Map.class).get("payment");}
    void authorizeInContext() throws Exception {Map<String,Object> c=json.readValue(note.getActiveSignContext(),Map.class);((Map)c.get("payment")).put("openid","synthetic-openid");note.setActiveSignContext(json.writeValueAsString(c));}
    Map<String,Object> result(String state){Map<String,Object> r=new HashMap<>();r.put("appid","wxsynthetic");r.put("mchid","1234567890");r.put("out_trade_no",note.getPaymentOrderNo());r.put("trade_state",state);r.put("transaction_id","synthetic-wechat-id");Map<String,Object> amount=new HashMap<>();amount.put("total",25000);amount.put("currency","CNY");r.put("amount",amount);r.put("payer",Collections.singletonMap("openid","synthetic-openid"));return r;}
    void start() throws Exception {create();authorizeInContext();assertEquals("INVOKE",service.payment(18,note.getPaymentOrderNo()).get("action"));}
    @Test void orderIsOnNoteAndDoubleClickReusesIt() throws Exception {start();String number=note.getPaymentOrderNo();when(gateway.query(number)).thenReturn(result("NOTPAY"));assertEquals("INVOKE",service.payment(18,number).get("action"));service.create(18,91);assertEquals(number,note.getPaymentOrderNo());assertEquals(32,number.length());verify(gateway,times(1)).prepay(eq(number),eq(25000L),anyString(),anyString());assertEquals(1,note.getBorrowerSignStatus());}
    @Test void onlyBorrowerCanPayAndOnlyParticipantsCanRead(){create();actor.setIdNoHash("lender");assertFalse((Boolean)service.detail(19,note.getPaymentOrderNo()).get("canPay"));assertThrows(IouBusinessException.class,()->service.payment(19,note.getPaymentOrderNo()));actor.setIdNoHash("outsider");assertThrows(IouBusinessException.class,()->service.detail(20,note.getPaymentOrderNo()));}
    @Test void oauthIsRequiredBeforeProviderSubmission(){create();assertEquals("AUTHORIZE",service.payment(18,note.getPaymentOrderNo()).get("action"));assertEquals(0,note.getServiceFeePayStatus());verify(gateway,never()).prepay(anyString(),anyLong(),anyString(),anyString());}
    @Test void paidQueryCompletesSignatureOnceEvenWithoutBrowser() throws Exception {start();String number=note.getPaymentOrderNo();when(gateway.query(number)).thenReturn(result("SUCCESS"));service.detail(18,number);assertEquals(2,note.getServiceFeePayStatus());assertEquals(2,note.getBorrowerSignStatus());assertEquals(7,note.getStatus());service.detail(18,number);verify(business,times(1)).completePaidBorrower(any());}
    @Test void lenderInitiatedNoteOnlyCompletesAfterBorrowerPayment() throws Exception {note.setLenderSignStatus(2);start();when(gateway.query(anyString())).thenReturn(result("SUCCESS"));service.detail(18,note.getPaymentOrderNo());assertEquals(2,note.getStatus());}
    @Test void notificationCompletesAndDuplicateNotificationIsIdempotent() throws Exception {start();Map<String,Object> r=result("SUCCESS");when(gateway.notification(any(),eq("signed"))).thenReturn(r);service.notify(new HttpHeaders(),"signed");service.notify(new HttpHeaders(),"signed");verify(business,times(1)).completePaidBorrower(any());assertEquals(2,note.getServiceFeePayStatus());}
    @Test void mismatchedAmountMerchantPayerCannotSign() throws Exception {for(String field:Arrays.asList("amount","mchid","payer")){setup();start();Map<String,Object> r=result("SUCCESS");r.put(field,field.equals("mchid")?"other":Collections.singletonMap(field.equals("amount")?"total":"openid","0"));when(gateway.query(anyString())).thenReturn(r);service.detail(18,note.getPaymentOrderNo());assertEquals(1,note.getServiceFeePayStatus());assertEquals(1,note.getBorrowerSignStatus());verify(business,never()).completePaidBorrower(any());}}
    @Test void uncertainSubmissionNeverCreatesANewOrder() throws Exception {create();authorizeInContext();String number=note.getPaymentOrderNo();when(gateway.prepay(anyString(),anyLong(),anyString(),anyString())).thenThrow(new IouBusinessException(502,"UNKNOWN","unknown"));assertThrows(IouBusinessException.class,()->service.payment(18,number));when(gateway.query(number)).thenReturn(result("NOTPAY"));assertEquals("QUERY",service.payment(18,number).get("action"));service.create(18,91);assertEquals(number,note.getPaymentOrderNo());verify(gateway,times(1)).prepay(anyString(),anyLong(),anyString(),anyString());}
    @Test void onlyConfirmedClosureAllowsReplacement() throws Exception {start();String old=note.getPaymentOrderNo();Map<String,Object> c=json.readValue(note.getActiveSignContext(),Map.class);((Map)c.get("payment")).put("expiresAt",1);note.setActiveSignContext(json.writeValueAsString(c));when(gateway.query(old)).thenReturn(result("NOTPAY"),result("CLOSED"));service.payment(18,old);assertEquals(3,note.getServiceFeePayStatus());service.create(18,91);assertNotEquals(old,note.getPaymentOrderNo());assertEquals(new BigDecimal("250.00"),note.getServiceFee());}
    @Test void replacingMerchantConfigCannotRouteExistingOrderToNewMerchant(){create();when(options.getMerchantId()).thenReturn("9876543210");assertThrows(IouBusinessException.class,()->service.authorize(18,note.getPaymentOrderNo()));}
    @Test void disabledCompensationDoesNotReadDatabase(){when(options.ready()).thenReturn(false);service.reconcilePending();verify(notes,never()).selectList(any());}
    @Test void authorizationStateIsConsumedOnceAndBoundToPendingSigner() throws Exception {create();String number=note.getPaymentOrderNo(),state=String.join("",Collections.nCopies(32,"a"));when(redis.execute(any(DefaultRedisScript.class),anyList())).thenReturn("{\"noteId\":91,\"orderNo\":\""+number+"\",\"userId\":18}",null);when(gateway.oauthOpenid("code")).thenReturn("synthetic-openid");assertTrue(service.oauthCallback("code",state).contains("start=true"));assertEquals("synthetic-openid",pay().get("openid"));assertThrows(IouBusinessException.class,()->service.oauthCallback("code",state));verify(gateway,times(1)).oauthOpenid(anyString());}
}
