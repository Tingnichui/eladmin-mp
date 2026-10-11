package me.zhengjie.iou;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.mapper.*;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.iou.service.impl.IouNoteServiceImpl;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class IouNoteInitiationTest {
    private IouNoteMapper notes;
    private IouUserMapper users;
    private IouNoteServiceImpl service;
    private IouUser creator;
    @BeforeEach void setup() {
        notes = mock(IouNoteMapper.class); users = mock(IouUserMapper.class);
        service = new IouNoteServiceImpl(notes, mock(RedisUtils.class), new IouIdentitySupport("test-encryption-password", "test-hmac-key-with-at-least-thirty-two-characters"), users);
        creator = new IouUser(); creator.setId(11L); creator.setRealName("合成本人"); creator.setIdVerifyStatus(2);
        creator.setIdNoHash(String.join("", Collections.nCopies(64, "a"))); creator.setIdNoEncrypted("synthetic-encrypted-identity");
        when(users.selectById(11L)).thenReturn(creator);
    }
    private IouNote draft(int role) {
        IouNote note = new IouNote(); note.setCreatorUserId(11L); note.setCreatorRole(role);
        note.setLoanType(1); note.setStatus(0); note.setPrincipalAmount(new BigDecimal("100.00"));
        note.setAnnualRate(BigDecimal.ZERO); note.setInterestAmount(BigDecimal.ZERO); note.setTotalAmount(new BigDecimal("100.00"));
        note.setServiceFee(BigDecimal.ZERO); note.setLoanDate(Timestamp.valueOf("2026-10-11 00:00:00")); note.setDueDate(Timestamp.valueOf("2027-10-11 00:00:00"));
        return note;
    }
    @Test void borrowerDraftCopiesOwnVerifiedIdentityAndAllowsUnknownLender() {
        IouNote note=draft(1);note.setLenderName(" ");service.create(note);
        assertEquals(creator.getIdNoHash(), note.getBorrowerIdNoHash());assertNull(note.getLenderIdNoHash());
        assertEquals(creator.getRealName(), note.getBorrowerName());assertNull(note.getLenderName());verify(notes).insert(note);
    }
    @Test void lenderDraftAndUpdateDoNotDereferenceMissingBorrower() {
        IouNote original=draft(2);service.create(original);original.setId(9L);original.setRowVersion(0L);
        when(notes.selectOne(any())).thenReturn(original);
        IouNote update=draft(2);update.setId(9L);service.update(update);
        assertNull(original.getBorrowerIdNoHash());assertEquals(1L,original.getRowVersion());verify(notes).updateById(original);
    }
    @Test void unverifiedCreatorAndPartialCounterpartyAreRejected() {
        creator.setIdVerifyStatus(0);assertThrows(BadRequestException.class,()->service.create(draft(1)));
        creator.setIdVerifyStatus(2);IouNote partial=draft(1);partial.setLenderName("合成对方");
        assertThrows(BadRequestException.class,()->service.create(partial));verify(notes,never()).insert(any());
    }
    @Test void callerCannotCreateAWaitingForSignatureOrFakeSignedRecord() {
        IouNote note=draft(1);note.setStatus(7);assertThrows(BadRequestException.class,()->service.create(note));
        note.setStatus(0);note.setBorrowerSignedAt(Timestamp.valueOf("2026-10-11 00:00:00"));assertThrows(BadRequestException.class,()->service.create(note));
        verify(notes,never()).insert(any());
    }
    @Test void adminCannotMoveAnUnsignedDraftIntoWaitingForSignature() {
        IouNote original=draft(1);original.setId(9L);service.create(originalWithoutId(original));original.setId(9L);
        when(notes.selectOne(any())).thenReturn(original);
        IouNote update=draft(1);update.setId(9L);update.setStatus(7);
        assertThrows(BadRequestException.class,()->service.update(update));verify(notes,never()).updateById(any());
    }
    private IouNote originalWithoutId(IouNote note) { note.setId(null); return note; }
    @Test void lockedNoteCannotBeModifiedOrDeletedThroughAdminCrud() {
        IouNote original=draft(1);original.setId(9L);original.setTermsLockedAt(Timestamp.valueOf("2026-10-11 00:00:00"));
        when(notes.selectOne(any())).thenReturn(original);when(notes.selectList(any())).thenReturn(Collections.singletonList(original));
        IouNote update=draft(1);update.setId(9L);
        assertThrows(BadRequestException.class,()->service.update(update));
        assertThrows(BadRequestException.class,()->service.deleteAll(Collections.singletonList(9L)));
        verify(notes,never()).updateById(any());verify(notes,never()).deleteBatchIds(any());
    }
    @Test void privateSigningFieldsAreNotReturnedAcceptedOrLogged() throws Exception {
        IouNote note=draft(1);note.setTermsSnapshot("secret-snapshot");note.setBorrowerSignRequestId("secret-request");
        note.setLenderSignEvidenceId("secret-evidence");note.setBorrowerSignFile("secret-file");
        assertFalse(JSON.toJSONString(note).contains("secret-"));assertFalse(new ObjectMapper().writeValueAsString(note).contains("secret-"));
        assertFalse(note.toString().contains("secret-"));
        String input="{\"termsSnapshot\":\"secret-snapshot\",\"borrowerSignStatus\":2,\"rowVersion\":99}";
        IouNote parsed=JSON.parseObject(input,IouNote.class);assertNull(parsed.getTermsSnapshot());assertNull(parsed.getBorrowerSignStatus());assertNull(parsed.getRowVersion());
        parsed=new ObjectMapper().readValue(input,IouNote.class);assertNull(parsed.getTermsSnapshot());assertNull(parsed.getBorrowerSignStatus());assertNull(parsed.getRowVersion());
    }
}
