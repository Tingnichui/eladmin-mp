package me.zhengjie.modules.iou;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.mapper.IouNoteMapper;
import me.zhengjie.iou.mapper.IouUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouAppQueryServiceTest {
    private IouUserMapper users;
    private IouNoteMapper notes;
    private IouAppQueryService service;
    private IouUser user;
    @BeforeEach void setup() {
        users = mock(IouUserMapper.class);
        notes = mock(IouNoteMapper.class);
        service = new IouAppQueryService(users, notes);
        user = new IouUser();
        user.setId(17L); user.setSysUserId(123L); user.setIdVerifyStatus(2);
        user.setIdNoHash(String.join("", Collections.nCopies(64, "a")));
        when(users.selectOne(any())).thenReturn(user);
    }
    private IouNote note() {
        IouNote note = new IouNote();
        note.setId(9007199254740993L); note.setCreatorUserId(17L); note.setStatus(2);
        note.setBorrowerIdNoHash(user.getIdNoHash()); note.setLenderIdNoHash("other-secret-hash");
        note.setBorrowerName("合成甲"); note.setLenderName("合成乙");
        note.setBorrowerIdNoEncrypted("secret-ciphertext"); note.setAgreementFile("private-file-path");
        note.setPrincipalAmount(new BigDecimal("9007199254740993.12"));
        note.setAnnualRate(new BigDecimal("0.150000"));
        note.setDueDate(Timestamp.valueOf("2027-01-10 00:00:00"));
        return note;
    }
    @Test void visibilityRequiresParticipantAndKeepsDraftsPrivate() {
        QueryWrapper<IouNote> query = service.visible(user);
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("(borrower_id_no_hash ="));
        assertTrue(sql.contains("OR lender_id_no_hash ="));
        assertTrue(sql.contains("AND (status <>"));
        assertTrue(sql.contains("OR creator_user_id ="));
        assertTrue(query.getParamNameValuePairs().containsValue(user.getIdNoHash()));
        assertTrue(query.getParamNameValuePairs().containsValue(user.getId()));
        assertTrue(query.getParamNameValuePairs().containsValue(0));
    }
    @Test void pendingOnlyIncludesOtherPartyInvitationWithoutOwnSignature() {
        QueryWrapper<IouNote> query = service.filtered(user, "all", null, true);
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("creator_user_id <>"));
        assertTrue(sql.contains("borrower_signed_at IS NULL"));
        assertTrue(sql.contains("lender_signed_at IS NULL"));
        assertTrue(query.getParamNameValuePairs().containsValue(1));
    }
    @Test void invalidInputsDoNotRunNoteQueries() {
        assertThrows(BadRequestException.class, () -> service.list(123, "all", null, false, 0, 20));
        assertThrows(BadRequestException.class, () -> service.list(123, "all", null, false, 1, 51));
        assertThrows(BadRequestException.class, () -> service.list(123, "injected", null, false, 1, 20));
        assertThrows(BadRequestException.class, () -> service.list(123, "all", 7, false, 1, 20));
        verifyNoInteractions(notes);
    }
    @Test void unverifiedOrMissingIdentityCannotReadNotes() {
        user.setIdVerifyStatus(0);
        assertThrows(BadRequestException.class, () -> service.summary(123));
        user.setIdVerifyStatus(2); user.setIdNoHash(null);
        assertThrows(BadRequestException.class, () -> service.detail(123, 9));
        when(users.selectOne(any())).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.list(123, "all", null, false, 1, 20));
        verifyNoInteractions(notes);
    }
    @Test void detailScopesIdentifierInDatabaseAndDoesNotRevealExistence() {
        assertEquals("借条不存在或无权查看", assertThrows(BadRequestException.class,
            () -> service.detail(123, 99)).getMessage());
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notes).selectOne(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("borrower_id_no_hash"));
        assertTrue(sql.contains("creator_user_id"));
        assertTrue(sql.contains("AND id ="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(99L));
        assertFalse(captor.getValue().getSqlSelect().contains("encrypted"));
        assertFalse(captor.getValue().getSqlSelect().contains("agreement_file"));
    }
    @Test void responseKeepsMoneyAndIdentifiersExactAndExcludesSecrets() {
        Map<String, Object> view = service.view(note(), user, true);
        assertEquals("9007199254740993", view.get("id"));
        assertEquals("9007199254740993.12", view.get("principalAmount"));
        assertEquals("0.150000", view.get("annualRate"));
        assertEquals("2027-01-10", view.get("dueDate"));
        assertEquals("borrower", view.get("role"));
        assertEquals("合成乙", view.get("counterpartyName"));
        String json = com.alibaba.fastjson2.JSON.toJSONString(view);
        assertFalse(json.contains(user.getIdNoHash()));
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("private-file-path"));
        assertFalse(json.contains("creatorUserId"));
    }
    @Test void lenderSeesBorrowerAsCounterparty() {
        IouNote note = note(); note.setBorrowerIdNoHash("other"); note.setLenderIdNoHash(user.getIdNoHash());
        Map<String, Object> result = service.view(note, user, false);
        assertEquals("lender", result.get("role"));
        assertEquals("合成甲", result.get("counterpartyName"));
        assertFalse(result.containsKey("remark"));
    }
    @Test void summaryUsesActivePrincipalOnlyAndIncludesZeroValues() {
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("noteCount", 0L); aggregate.put("principal", BigDecimal.ZERO);
        when(notes.selectMaps(any())).thenReturn(Collections.singletonList(aggregate));
        when(notes.selectCount(any())).thenReturn(0L);
        Map<String, Object> result = service.summary(123);
        assertEquals("0.00", ((Map<?, ?>) result.get("borrowed")).get("principal"));
        assertEquals(0L, result.get("pendingCount"));
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(notes, times(2)).selectMaps(captor.capture());
        for (QueryWrapper query : captor.getAllValues()) {
            assertTrue(query.getSqlSelect().contains("SUM(principal_amount)"));
            query.getSqlSegment();
            assertTrue(query.getParamNameValuePairs().containsValue(2));
        }
    }
    @Test void listPagesInStableOrderAndUsesSafeProjection() {
        when(notes.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            Page<IouNote> page = call.getArgument(0);
            page.setRecords(Collections.singletonList(note())); page.setTotal(21);
            assertEquals(2, page.getCurrent()); assertEquals(20, page.getSize());
            QueryWrapper<IouNote> query = call.getArgument(1);
            assertTrue(query.getSqlSegment().endsWith("ORDER BY create_time DESC,id DESC"));
            assertFalse(query.getSqlSelect().contains("encrypted"));
            return page;
        });
        Map<String, Object> result = service.list(123, "borrower", 2, false, 2, 20);
        assertEquals(21L, result.get("totalElements"));
        assertEquals(1, ((List<?>) result.get("content")).size());
    }
    @Test void controllerRequiresH5AccountAndExposesOnlyGetOperations() {
        PreAuthorize permission = IouAppQueryController.class.getAnnotation(PreAuthorize.class);
        assertTrue(permission.value().contains("canAccessH5"));
        Arrays.stream(IouAppQueryController.class.getDeclaredMethods()).filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
            .forEach(m -> assertNotNull(m.getAnnotation(GetMapping.class)));
    }
}
