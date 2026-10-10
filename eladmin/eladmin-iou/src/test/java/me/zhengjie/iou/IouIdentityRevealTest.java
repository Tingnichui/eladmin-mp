package me.zhengjie.iou;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.domain.dto.IouUserQueryCriteria;
import me.zhengjie.iou.mapper.IouUserMapper;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.iou.service.impl.IouUserServiceImpl;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IouIdentityRevealTest {
    private final IouIdentitySupport identity = new IouIdentitySupport(
        "unit-test-encryption-password", "unit-test-hmac-key-with-at-least-32-characters");
    private final IouUserMapper mapper = mock(IouUserMapper.class);
    private final IouUserServiceImpl service = new IouUserServiceImpl(mapper, mock(RedisUtils.class), identity);
    private final String syntheticId = "00000020000101000X";

    @Test
    void listOnlySerializesMaskWhileDedicatedLookupReturnsPlaintext() throws Exception {
        IouUser user = new IouUser();
        user.setId(1L);
        user.setIdNoEncrypted(identity.encrypt(syntheticId));
        user.setIdNoHash(identity.hash(syntheticId));
        Page<IouUser> page = new Page<>();
        page.setRecords(Collections.singletonList(user));
        when(mapper.findAll(any(), any())).thenReturn(page);
        when(mapper.selectById(1L)).thenReturn(user);
        String json = new ObjectMapper().writeValueAsString(service.queryAll(new IouUserQueryCriteria(), new Page<>()));
        assertTrue(json.contains("000***********000X"));
        assertFalse(json.contains(syntheticId));
        assertFalse(json.contains(user.getIdNoEncrypted()));
        assertFalse(json.contains(user.getIdNoHash()));
        assertEquals(syntheticId, service.revealIdentity(1L));
    }

    @Test
    void invalidCiphertextNeverLeaksIntoErrorOrBreaksMasking() {
        assertNull(identity.mask("invalid-ciphertext"));
        assertNull(identity.mask(null));
        assertEquals("身份证信息暂不可查看", assertThrows(BadRequestException.class,
            () -> identity.decrypt("invalid-ciphertext")).getMessage());
        assertNull(assertThrows(BadRequestException.class,
            () -> identity.decrypt("invalid-ciphertext")).getCause());
    }

    @Test
    void missingUserAndMissingIdentityAreHandled() {
        assertThrows(BadRequestException.class, () -> service.revealIdentity(1L));
        when(mapper.selectById(1L)).thenReturn(new IouUser());
        assertThrows(BadRequestException.class, () -> service.revealIdentity(1L));
    }
}
