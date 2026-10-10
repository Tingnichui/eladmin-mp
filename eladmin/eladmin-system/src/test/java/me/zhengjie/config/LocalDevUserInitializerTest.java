package me.zhengjie.config;

import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.DeptService;
import me.zhengjie.modules.system.service.JobService;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDevUserInitializerTest {

    @Test
    void createsMissingUserWithEncodedPasswordAndReferences() {
        UserService userService = mock(UserService.class);
        RoleService roleService = mock(RoleService.class);
        JobService jobService = mock(JobService.class);
        DeptService deptService = mock(DeptService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        LocalDevUserInitializer initializer = new LocalDevUserInitializer(
                userService, roleService, jobService, deptService, passwordEncoder);
        configure(initializer);

        Role role = new Role();
        role.setId(1L);
        Job job = new Job();
        job.setId(11L);
        Dept dept = new Dept();
        dept.setId(2L);
        when(roleService.getById(1L)).thenReturn(role);
        when(jobService.getById(11L)).thenReturn(job);
        when(deptService.getById(2L)).thenReturn(dept);
        when(passwordEncoder.encode("local-password")).thenReturn("encoded-password");

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).create(captor.capture());
        User created = captor.getValue();
        assertEquals("chunhui", created.getUsername());
        assertEquals("encoded-password", created.getPassword());
        assertEquals("chunhui@local.invalid", created.getEmail());
        assertTrue(created.getRoles().contains(role));
        assertTrue(created.getJobs().contains(job));
        assertEquals(dept, created.getDept());
    }

    @Test
    void leavesExistingUserUnchanged() {
        UserService userService = mock(UserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        LocalDevUserInitializer initializer = new LocalDevUserInitializer(
                userService, mock(RoleService.class), mock(JobService.class),
                mock(DeptService.class), passwordEncoder);
        configure(initializer);
        when(userService.findByName("chunhui")).thenReturn(new User());

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(userService, never()).create(org.mockito.ArgumentMatchers.any(User.class));
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    private void configure(LocalDevUserInitializer initializer) {
        ReflectionTestUtils.setField(initializer, "username", "chunhui");
        ReflectionTestUtils.setField(initializer, "password", "local-password");
        ReflectionTestUtils.setField(initializer, "roleId", 1L);
        ReflectionTestUtils.setField(initializer, "jobId", 11L);
        ReflectionTestUtils.setField(initializer, "deptId", 2L);
        ReflectionTestUtils.setField(initializer, "email", "");
        ReflectionTestUtils.setField(initializer, "phone", "19900000000");
    }
}
