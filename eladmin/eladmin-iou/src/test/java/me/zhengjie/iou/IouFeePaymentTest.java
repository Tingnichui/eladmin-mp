package me.zhengjie.iou;

import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.mapper.*;
import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouFeePaymentTest {
    IouNoteMapper notes;IouFeeOrderMapper orders;IouPaymentAttemptMapper attempts;IouNoteBusinessService business;
    IouPaymentOptions options;IouHuifuGateway gateway;IouFeePaymentService service;IouNote note;IouUser actor;
    IouFeeOrder order;IouPaymentAttempt attempt;
    @BeforeEach void setup() {
        notes=mock(IouNoteMapper.class);orders=mock(IouFeeOrderMapper.class);attempts=mock(IouPaymentAttemptMapper.class);
        business=mock(IouNoteBusinessService.class);options=mock(IouPaymentOptions.class);gateway=mock(IouHuifuGateway.class);
        when(options.ready()).thenReturn(true);when(options.getHuifuId()).thenReturn("merchant");when(options.getAppId()).thenReturn("wxsynthetic");when(options.getExpireMinutes()).thenReturn(10);
        when(options.getH5Origin()).thenReturn("https://example.test");when(options.getOauthCallbackUrl()).thenReturn("https://example.test/api/iou/payment/wechat/callback");
        PlatformTransactionManager manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        service=new IouFeePaymentService(notes,orders,attempts,business,options,gateway,manager);
        actor=new IouUser();actor.setId(18L);actor.setIdNoHash("borrower");when(business.person(anyLong(),eq(false))).thenAnswer(i->actor);
        note=new IouNote();note.setId(91L);note.setBorrowerIdNoHash("borrower");note.setLenderIdNoHash("lender");note.setStatus(6);note.setBorrowerSignStatus(2);note.setLenderSignStatus(2);
        note.setAgreementFile("final.html");note.setAgreementHash("hash");note.setServiceFee(new BigDecimal("10.00"));note.setRowVersion(2L);note.setTermsSnapshot("fee-snapshot");
        when(notes.selectOne(any())).thenAnswer(i->note);when(notes.updateById(any())).thenReturn(1);
        when(orders.selectOne(any())).thenAnswer(i->order);when(orders.selectById(any())).thenAnswer(i->order);
        when(orders.insert(any())).thenAnswer(i->{order=i.getArgument(0);order.setId(1L);return 1;});when(orders.updateById(any())).thenReturn(1);when(orders.update(any(),any())).thenReturn(1);
        when(attempts.selectOne(any())).thenAnswer(i->attempt);when(attempts.selectById(any())).thenAnswer(i->attempt);
        when(attempts.insert(any())).thenAnswer(i->{attempt=i.getArgument(0);attempt.setId(1L);return 1;});when(attempts.updateById(any())).thenReturn(1);when(attempts.update(any(),any())).thenReturn(1);
    }
    void create(){service.create(18,91);}
    Map<String,Object> response(String state){Map<String,Object> r=new HashMap<>();r.put("huifu_id","merchant");r.put("req_date",attempt.getReqDate());r.put("req_seq_id",attempt.getReqSeqId());r.put("trans_amt","10.00");r.put("trans_stat",state);r.put("trade_type","T_JSAPI");return r;}
    void pending(){create();order.setPayerOpenid("synthetic-openid");doAnswer(i->{throw new IouBusinessException(502,"UNKNOWN","unknown");}).when(gateway).create(any(),any());assertThrows(IouBusinessException.class,()->service.payment(18,order.getOrderNo()));}
    @Test void onlyBorrowerCanCreateOrPayAndUnrelatedUsersCannotRead() {
        actor.setIdNoHash("lender");assertThrows(IouBusinessException.class,()->create());verify(orders,never()).insert(any());
        actor.setIdNoHash("borrower");create();actor.setId(19L);actor.setIdNoHash("lender");assertFalse((Boolean)service.detail(19,order.getOrderNo()).get("canPay"));
        assertThrows(IouBusinessException.class,()->service.payment(19,order.getOrderNo()));
        actor.setIdNoHash("outsider");assertThrows(IouBusinessException.class,()->service.detail(20,order.getOrderNo()));
    }
    @Test void orderAmountIsServerOwnedAndRepeatedCreateReturnsSameOrder() {
        create();String number=order.getOrderNo();service.create(18,91);assertEquals(number,order.getOrderNo());verify(orders,times(1)).insert(any());assertEquals(new BigDecimal("10.00"),order.getAmount());
    }
    @Test void incompleteSigningCannotCreateOrder() {
        note.setLenderSignStatus(0);assertThrows(IouBusinessException.class,()->create());verify(orders,never()).insert(any());
    }
    @Test void paymentNeedsOauthBeforeProviderRequest() {
        create();assertEquals("AUTHORIZE",service.payment(18,order.getOrderNo()).get("action"));verifyNoInteractions(gateway);verify(attempts,never()).insert(any());
    }
    @Test void unknownSubmissionIsPersistedAndCannotCreateAnotherAttempt() {
        pending();assertEquals("SUBMITTING",attempt.getStatus());when(gateway.query(any())).thenReturn(response("P"));
        assertEquals("QUERY",service.payment(18,order.getOrderNo()).get("action"));verify(attempts,times(1)).insert(any());verify(gateway,times(1)).create(any(),any());assertEquals(6,note.getStatus());
    }
    @Test void verifiedQueryActivatesOnceAndDoesNotRewriteSignedFile() {
        pending();when(gateway.query(any())).thenReturn(response("S"));service.detail(18,order.getOrderNo());
        assertEquals("PAID",order.getStatus());assertEquals("SUCCESS",attempt.getStatus());assertEquals(2,note.getStatus());assertEquals("final.html",note.getAgreementFile());assertEquals(3L,note.getRowVersion());
        service.detail(18,order.getOrderNo());service.create(18,91);verify(notes,times(1)).updateById(any());
    }
    @Test void borrowerInitiatedNoteCanPayBeforeLenderSignsAndThenWaitsForLender() {
        note.setLenderSignStatus(0);note.setAgreementFile(null);note.setAgreementHash(null);
        note.setBorrowerSignFile("borrower-original.html");note.setBorrowerSignFileHash("original-hash");
        pending();when(gateway.query(any())).thenReturn(response("S"));service.detail(18,order.getOrderNo());
        assertEquals("PAID",order.getStatus());assertEquals(2,note.getServiceFeePayStatus());assertEquals(7,note.getStatus());
        assertNull(note.getEffectiveAt());assertNull(note.getAgreementFile());assertEquals("borrower-original.html",note.getBorrowerSignFile());
        service.detail(18,order.getOrderNo());verify(notes,times(1)).updateById(any());
    }
    @Test void mismatchedAmountMerchantAndRequestCannotActivate() {
        for(String field:Arrays.asList("trans_amt","huifu_id","req_seq_id","req_date","trade_type")) {
            setup();pending();Map<String,Object> forged=response("S");forged.put(field,"999");when(gateway.query(any())).thenReturn(forged);
            service.detail(18,order.getOrderNo());assertEquals("UNPAID",order.getStatus());assertEquals(6,note.getStatus());verify(notes,never()).updateById(any());
        }
    }
    @Test void expiryOnlyCreatesNewAttemptAfterConfirmedClose() {
        pending();attempt.setExpiresAt(new Timestamp(System.currentTimeMillis()-1000));when(gateway.query(any())).thenAnswer(i->response("P"));
        when(gateway.close(any())).thenReturn(Collections.singletonMap("trans_stat","P"));assertEquals("QUERY",service.payment(18,order.getOrderNo()).get("action"));verify(attempts,times(1)).insert(any());
        Map<String,Object> closed=new HashMap<>();closed.put("trans_stat","S");closed.put("resp_code","00000000");closed.put("huifu_id",attempt.getHuifuId());closed.put("org_req_date",attempt.getReqDate());closed.put("org_req_seq_id",attempt.getReqSeqId());when(gateway.close(any())).thenReturn(closed);
        assertThrows(IouBusinessException.class,()->service.payment(18,order.getOrderNo()));verify(attempts,times(2)).insert(any());
    }
    @Test void callbackTriggersDurableQueryAndDoesNotTrustNotificationAlone() {
        pending();Map<String,Object> data=response("S");when(gateway.notification("raw","signed")).thenReturn(data);
        assertEquals("RECV_ORD_ID_"+attempt.getReqSeqId(),service.notify("raw","signed"));assertEquals(6,note.getStatus());verify(attempts).update(isNull(),any());verify(gateway,never()).query(any());
    }
    @Test void oauthStateIsOneTimeAndCannotBeReplayed() {
        create();String state=String.join("",Collections.nCopies(32,"a"));order.setOauthStateHash(IouAgreementStore.hash(state));order.setOauthExpiresAt(new Timestamp(System.currentTimeMillis()+60000));
        when(gateway.oauthOpenid("code")).thenReturn("synthetic-openid");String redirect=service.oauthCallback("code",state);
        assertTrue(redirect.startsWith("https://example.test/#/notes/payment?noteId=91&orderNo=SF"));assertEquals("synthetic-openid",order.getPayerOpenid());
        assertThrows(IouBusinessException.class,()->service.oauthCallback("code",state));verify(gateway,times(1)).oauthOpenid(any());
    }
    @Test void disabledSchedulerDoesNotAccessPaymentTables() {
        when(options.ready()).thenReturn(false);service.reconcilePending();verify(attempts,never()).selectList(any());
    }
}
