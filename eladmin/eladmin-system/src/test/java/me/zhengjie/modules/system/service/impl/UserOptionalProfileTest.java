package me.zhengjie.modules.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.zhengjie.config.properties.FileProperties;
import me.zhengjie.exception.EntityExistException;
import me.zhengjie.modules.security.service.OnlineUserService;
import me.zhengjie.modules.security.service.UserCacheManager;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.UserJobMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.mapper.UserRoleMapper;
import me.zhengjie.utils.RedisUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserOptionalProfileTest {
    private UserMapper mapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), User.class);
        mapper = mock(UserMapper.class);
        service = spy(new UserServiceImpl(mapper, mock(UserJobMapper.class), mock(UserRoleMapper.class),
            mock(FileProperties.class), mock(RedisUtils.class), mock(UserCacheManager.class), mock(OnlineUserService.class)));
        doReturn(true).when(service).save(any(User.class));
        doReturn(true).when(service).update(any(User.class), any(Wrapper.class));
    }

    @Test
    void optionalFieldsAreNotRequiredButFormatsStillApply() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            User user = user();
            assertTrue(factory.getValidator().validate(user).isEmpty());
            user.setPhone("");
            user.setEmail("");
            assertTrue(factory.getValidator().validate(user).isEmpty());
            user.setPhone("invalid");
            user.setEmail("invalid");
            assertEquals(2, factory.getValidator().validate(user).size());
            assertEquals(2, factory.getValidator().validate(user, User.Update.class).size());
        }
    }

    @Test
    void createNormalizesEmptyFieldsAndSkipsContactLookups() {
        User user = user();
        user.setNickName("  ");
        user.setPhone("");
        user.setEmail("  ");
        user.setIsAdmin(true);
        service.create(user);
        assertNull(user.getNickName());
        assertNull(user.getPhone());
        assertNull(user.getEmail());
        assertFalse(user.getIsAdmin());
        assertEquals("unchanged-hash", user.getPassword());
        verify(mapper, never()).findByPhone(any());
        verify(mapper, never()).findByEmail(any());
        verify(service).save(user);
    }

    @Test
    void suppliedContactsAreTrimmedAndChecked() {
        User user = user();
        user.setPhone(" 13800138000 ");
        user.setEmail(" test@example.test ");
        user.setNickName(" 测试昵称 ");
        service.create(user);
        verify(mapper).findByPhone("13800138000");
        verify(mapper).findByEmail("test@example.test");
        assertEquals("测试昵称", user.getNickName());
    }

    @Test
    void duplicateEmailAndPhoneStillBlockCreate() {
        User user = user();
        user.setEmail("test@example.test");
        when(mapper.findByEmail(user.getEmail())).thenReturn(user());
        assertThrows(EntityExistException.class, () -> service.create(user));
        reset(mapper);
        user.setEmail(null);
        user.setPhone("13800138000");
        when(mapper.findByPhone(user.getPhone())).thenReturn(user());
        assertThrows(EntityExistException.class, () -> service.create(user));
        verify(service, never()).save(any(User.class));
    }

    @Test
    void adminUpdateExplicitlyClearsOptionalColumns() throws Exception {
        User stored = user();
        stored.setEmail("old@example.test");
        stored.setPhone("13800138000");
        stored.setNickName("旧昵称");
        doReturn(stored).when(service).getById(1L);
        service.update(user());
        LambdaUpdateWrapper<User> wrapper = capturedUpdate();
        assertTrue(wrapper.getSqlSet().contains("email="));
        assertTrue(wrapper.getSqlSet().contains("phone="));
        assertTrue(wrapper.getSqlSet().contains("nick_name="));
        assertNull(stored.getEmail());
        assertNull(stored.getPhone());
        assertNull(stored.getNickName());
        verify(mapper, never()).findByPhone(any());
        verify(mapper, never()).findByEmail(any());
    }

    @Test
    void centerCanClearPhoneAndNicknameWithoutClearingEmail() {
        User stored = user();
        stored.setEmail("keep@example.test");
        stored.setPhone("13800138000");
        stored.setNickName("旧昵称");
        doReturn(stored).when(service).getById(1L);
        service.updateCenter(user());
        LambdaUpdateWrapper<User> wrapper = capturedUpdate();
        assertTrue(wrapper.getSqlSet().contains("phone="));
        assertTrue(wrapper.getSqlSet().contains("nick_name="));
        assertFalse(wrapper.getSqlSet().contains("email="));
        assertEquals("keep@example.test", stored.getEmail());
        verify(mapper, never()).findByPhone(any());
    }

    @Test
    void duplicateContactStillBlocksEditButOwnContactDoesNot() {
        User stored = user();
        User resources = user();
        resources.setPhone("13800138000");
        doReturn(stored).when(service).getById(1L);
        User other = user();
        other.setId(2L);
        when(mapper.findByPhone(resources.getPhone())).thenReturn(other);
        assertThrows(EntityExistException.class, () -> service.updateCenter(resources));
        when(mapper.findByPhone(resources.getPhone())).thenReturn(stored);
        assertDoesNotThrow(() -> service.updateCenter(resources));
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<User> capturedUpdate() {
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).update(any(User.class), captor.capture());
        return (LambdaUpdateWrapper<User>) captor.getValue();
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test-user");
        user.setEnabled(true);
        user.setPassword("unchanged-hash");
        Dept dept = new Dept();
        dept.setId(1L);
        user.setDept(dept);
        user.setRoles(Collections.emptySet());
        user.setJobs(Collections.emptySet());
        return user;
    }
}
