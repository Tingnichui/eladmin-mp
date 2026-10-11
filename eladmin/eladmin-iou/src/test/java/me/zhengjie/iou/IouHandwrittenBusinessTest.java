package me.zhengjie.iou;

import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.domain.dto.IouNoteConditions;
import me.zhengjie.iou.mapper.*;
import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouHandwrittenBusinessTest {
    @TempDir Path dir;
    IouNoteOptions options;IouNoteMapper notes;IouUserMapper users;IouNotePlanMapper plans;IouNoteBusinessService service;IouNote stored;IouUser actor;java.util.List<IouNotePlan> plan;
    @BeforeEach void setup(){
        options=new IouNoteOptions();options.setSigningEnabled(true);options.setSealKey(String.join("",Collections.nCopies(40,"s")));options.setStoragePath(dir.toString());
        notes=mock(IouNoteMapper.class);users=mock(IouUserMapper.class);plans=mock(IouNotePlanMapper.class);actor=user(17,"a");plan=new ArrayList<>();
        when(users.selectOne(any())).thenAnswer(i->actor);when(notes.selectOne(any())).thenAnswer(i->stored);
        when(notes.insert(any())).thenAnswer(i->{stored=i.getArgument(0);stored.setId(91L);return 1;});when(notes.updateById(any())).thenReturn(1);
        when(plans.insert(any())).thenAnswer(i->{plan.add(i.getArgument(0));return 1;});when(plans.selectList(any())).thenAnswer(i->plan);
        IouNoteNumberService numbers=mock(IouNoteNumberService.class);when(numbers.next()).thenReturn("JT20261011-002020");
        service=new IouNoteBusinessService(notes,plans,users,new IouRepaymentCalculator(options),options,new IouAgreementStore(options),numbers);
    }
    IouUser user(long id,String h){IouUser u=new IouUser();u.setId(id);u.setSysUserId(id);u.setRealName("合成用户"+id);u.setIdVerifyStatus(2);u.setIdNoHash(String.join("",Collections.nCopies(64,h)));u.setIdNoEncrypted("synthetic-encrypted");return u;}
    String image() throws Exception{BufferedImage b=new BufferedImage(640,240,BufferedImage.TYPE_INT_ARGB);Graphics2D g=b.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,640,240);g.setColor(Color.BLACK);g.setStroke(new BasicStroke(4));g.drawLine(20,20,160,120);g.drawLine(160,120,250,40);g.dispose();ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(b,"png",out);return "data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray());}
    void draft(){IouNoteConditions c=IouRepaymentCalculatorTest.standard("principal");service.create(17,"create-operation-key-123",c);}
    Map<String,Object> sign(String key,String reference) throws Exception{Map<String,Object>p=reference==null?service.preview(actor.getSysUserId(),91):service.shared(actor.getSysUserId(),reference);return service.sign(actor.getSysUserId(),91,(String)p.get("version"),(String)p.get("previewHash"),key,reference,image(),true,"127.0.0.1","synthetic-test");}
    @Test void draftIdentityAndIdempotenceAreServerOwned(){draft();assertNull(stored.getBorrowerIdNoHash());assertEquals(actor.getIdNoHash(),stored.getLenderIdNoHash());service.create(17,"create-operation-key-123",IouRepaymentCalculatorTest.standard("principal"));verify(notes,times(1)).insert(any());IouNoteConditions changed=IouRepaymentCalculatorTest.standard("principal");changed.setPrincipalAmount("999");assertThrows(IouBusinessException.class,()->service.create(17,"create-operation-key-123",changed));}
    @Test void completeFlowKeepsOriginalAndBindsOnlySuccessfulCounterparty() throws Exception{
        draft();sign("initiator-sign-key-123",null);assertEquals(7,stored.getStatus());assertNull(stored.getBorrowerIdNoHash());String original=stored.getLenderSignFileHash();
        String reference=(String)service.share(17,91).get("reference");actor=user(18,"b");service.shared(18,reference);assertNull(stored.getBorrowerName());
        sign("counterparty-sign-key-123",reference);assertEquals(2,stored.getStatus());assertEquals(actor.getIdNoHash(),stored.getBorrowerIdNoHash());assertEquals(original,stored.getLenderSignFileHash());assertNotNull(stored.getEffectiveAt());assertTrue(new String(service.agreement(18,91),java.nio.charset.StandardCharsets.UTF_8).contains(original));
        Map<String,Object>evidence=new com.fasterxml.jackson.databind.ObjectMapper().readValue(stored.getActiveSignContext(),Map.class);Map<String,Object>own=(Map<String,Object>)evidence.get("borrower");
        service.sign(18,91,"0",(String)own.get("previewHash"),"counterparty-sign-key-123",reference,image(),true,"x","x");verify(notes,times(2)).updateById(any());
    }
    @Test void signedTermsCannotBeEditedAndShareDoesNotAuthorizeNormalDetail() throws Exception{draft();sign("initiator-sign-key-123",null);assertThrows(IouBusinessException.class,()->service.update(17,91,"1",IouRepaymentCalculatorTest.standard("principal")));actor=user(18,"b");assertThrows(IouBusinessException.class,()->service.detail(18,91));assertThrows(IouBusinessException.class,()->service.agreement(18,91));}
    @Test void selfSignAndSecondCounterpartyAreRejected() throws Exception{draft();sign("initiator-sign-key-123",null);String ref=(String)service.share(17,91).get("reference");assertThrows(IouBusinessException.class,()->sign("self-sign-other-key-123",ref));actor=user(18,"b");sign("first-counterparty-key-123",ref);actor=user(19,"c");Map<String,Object>s=service.shared(19,ref);assertFalse(s.containsKey("terms"));assertThrows(IouBusinessException.class,()->service.sign(19,91,"1","x","second-counterparty-key-123",ref,image(),true,"x","x"));}
    @Test void stalePreviewCancellationAndForgedReferenceAreRejected() throws Exception{draft();assertThrows(IouBusinessException.class,()->service.sign(17,91,"0","wrong","initiator-sign-key-123",null,image(),true,"x","x"));sign("initiator-sign-key-123",null);String ref=(String)service.share(17,91).get("reference");service.cancel(17,91,"1","取消测试");actor=user(18,"b");assertFalse(service.shared(18,ref).containsKey("terms"));assertThrows(IouBusinessException.class,()->service.shared(18,ref.substring(0,ref.length()-1)+"z"));assertThrows(IouBusinessException.class,()->service.sign(18,91,"2","x","cancelled-sign-key-123",ref,image(),true,"x","x"));}
    @Test void configurationAndBlankSignatureCannotFakeSuccess() throws Exception{draft();options.setSigningEnabled(false);assertThrows(IouBusinessException.class,()->sign("initiator-sign-key-123",null));options.setSigningEnabled(true);assertThrows(IouBusinessException.class,()->IouAgreementStore.signature("data:image/png;base64,AAAA"));assertEquals(0,stored.getStatus());}
    @Test void fileIntegrityAndPathTraversalAreRejected(){IouAgreementStore store=new IouAgreementStore(options);String key=store.write("sample.html","safe".getBytes(java.nio.charset.StandardCharsets.UTF_8));assertThrows(IouBusinessException.class,()->store.read(key,"bad"));assertThrows(IouBusinessException.class,()->store.write("../escape.html",new byte[0]));}
    @Test void wrongRoleUnknownFieldsAndStaleVersionCannotWrite(){IouNoteConditions c=IouRepaymentCalculatorTest.standard("principal");c.setCreatorRole("admin");assertThrows(IouBusinessException.class,()->service.create(17,"create-operation-key-123",c));assertThrows(IouBusinessException.class,()->c.rejectUnknown("borrowerIdNo","secret"));draft();assertThrows(IouBusinessException.class,()->service.cancel(17,91,"9","bad"));verify(notes,never()).updateById(any());}
    @Test void cancelledSignedNoteCanBeDeletedByCreatorAndOldInviteStopsWorking() throws Exception {
        draft();sign("initiator-sign-key-123",null);
        String reference=(String)service.share(17,91).get("reference");
        service.cancel(17,91,"1","取消测试");
        assertTrue(service.actions(stored,actor).contains("DELETE"));
        when(notes.deleteById(91L)).thenAnswer(i->{stored=null;return 1;});
        service.delete(17,91,"2");
        org.mockito.InOrder order=inOrder(plans,notes);
        order.verify(plans).delete(any());order.verify(notes).deleteById(91L);
        service.delete(17,91,"2");verify(notes,times(1)).deleteById(91L);
        assertThrows(IouBusinessException.class,()->service.shared(17,reference));
    }
    @Test void cancelledDeletionChecksCreatorAndCurrentVersionBeforeRemovingPlans() {
        draft();service.cancel(17,91,"0","取消测试");
        assertThrows(IouBusinessException.class,()->service.delete(17,91,"0"));
        actor=user(18,"b");
        assertFalse(service.actions(stored,actor).contains("DELETE"));
        assertThrows(IouBusinessException.class,()->service.delete(18,91,"1"));
        verify(plans,never()).delete(any());verify(notes,never()).deleteById(anyLong());
    }
    @Test void cancelledLegacyNoteCanBeDeletedButOtherNonDraftStatusesCannot() {
        draft();stored.setStatus(4);stored.setRepaymentMethod(null);
        assertTrue(service.actions(stored,actor).contains("DELETE"));
        service.delete(17,91,"0");verify(notes).deleteById(91L);
        clearInvocations(notes,plans);
        for(int status:new int[]{1,2,3,5,6,7}) {
            stored.setStatus(status);
            assertThrows(IouBusinessException.class,()->service.delete(17,91,"0"));
        }
        verify(plans,never()).delete(any());verify(notes,never()).deleteById(anyLong());
    }
}
