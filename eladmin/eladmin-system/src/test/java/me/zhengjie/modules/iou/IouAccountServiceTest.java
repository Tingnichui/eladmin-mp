package me.zhengjie.modules.iou;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import me.zhengjie.config.properties.RsaProperties;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.mapper.IouUserMapper;
import me.zhengjie.iou.plugins.YzhIdentityVerifier;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.modules.system.domain.*;
import me.zhengjie.modules.system.mapper.*;
import me.zhengjie.modules.system.service.UserService;
import me.zhengjie.utils.RsaUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouAccountServiceTest {
    private IouUserMapper persons;
    private UserService users;
    private DeptMapper depts;
    private JobMapper jobs;
    private RoleMapper roles;
    private IouIdentitySupport identity;
    private YzhIdentityVerifier verifier;
    private IouRequestGuard guard;
    private PlatformTransactionManager transactions;
    private PasswordEncoder passwords;
    private IouAccountService service;
    private RsaUtils.RsaKeyPair keyPair;
    private String originalKey;

    @BeforeEach
    void setUp() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),IouUser.class);
        persons=mock(IouUserMapper.class); users=mock(UserService.class); depts=mock(DeptMapper.class);
        jobs=mock(JobMapper.class); roles=mock(RoleMapper.class); identity=mock(IouIdentitySupport.class);
        verifier=mock(YzhIdentityVerifier.class); guard=mock(IouRequestGuard.class);
        transactions=mock(PlatformTransactionManager.class); passwords=mock(PasswordEncoder.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        keyPair=RsaUtils.generateKeyPair(); originalKey=RsaProperties.privateKey; RsaProperties.privateKey=keyPair.getPrivateKey();
        when(identity.normalize(any())).thenReturn("110101199001010010");
        when(identity.hash(any())).thenReturn(String.join("", Collections.nCopies(64, "a"))); when(identity.encrypt(any())).thenReturn("test-encrypted");
        when(passwords.encode(any())).thenReturn("test-bcrypt");
        Dept dept=new Dept(); dept.setId(IouAccountService.DEPT_ID); dept.setName("借条用户"); dept.setEnabled(true);
        Job job=new Job(); job.setId(IouAccountService.JOB_ID); job.setName("借条用户"); job.setEnabled(true);
        Role role=new Role(); role.setId(IouAccountService.ROLE_ID); role.setName("借条用户");
        when(depts.selectById(IouAccountService.DEPT_ID)).thenReturn(dept);
        when(jobs.selectById(IouAccountService.JOB_ID)).thenReturn(job);
        when(roles.selectById(IouAccountService.ROLE_ID)).thenReturn(role);
        when(verifier.iDCardVerify(any(),any())).thenReturn(true);
        doAnswer(invocation -> {((User)invocation.getArgument(0)).setId(123L);return null;}).when(users).create(any());
        when(persons.insert(any())).thenReturn(1);
        service=service("v1");
    }
    @AfterEach
    void restoreKey() {RsaProperties.privateKey=originalKey;}

    private IouAccountService service(String version) {
        return new IouAccountService(persons,users,depts,jobs,roles,identity,verifier,passwords,guard,transactions,
            version,"https://example.com/agreement","https://example.com/privacy");
    }
    private IouRegisterRequest request() throws Exception {
        IouRegisterRequest r=new IouRegisterRequest(); r.setRealName("测试姓名");r.setIdNo("110101199001010010");
        r.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"111111"));r.setCode("2");r.setUuid("test-uuid");
        r.setAgreementAccepted(true);r.setIdentityConsentAccepted(true);r.setAgreementVersion("v1");return r;
    }

    @Test void createsSystemUserWithFixedAssociationsAndOnlyTwoFactorState() throws Exception {
        service.register(request(),"127.0.0.1");
        ArgumentCaptor<User> account=ArgumentCaptor.forClass(User.class);verify(users).create(account.capture());
        assertEquals("IOU",account.getValue().getUserType());
        assertEquals("iou_"+identity.hash("110101199001010010"),account.getValue().getUsername());
        assertTrue(account.getValue().getUsername().matches("iou_[a-f0-9]{64}"));
        assertEquals(IouAccountService.DEPT_ID,account.getValue().getDept().getId());
        assertEquals(IouAccountService.ROLE_ID,account.getValue().getRoles().iterator().next().getId());
        assertEquals(IouAccountService.JOB_ID,account.getValue().getJobs().iterator().next().getId());
        assertEquals("test-bcrypt",account.getValue().getPassword());assertEquals("测试姓名",account.getValue().getNickName());
        ArgumentCaptor<IouUser> person=ArgumentCaptor.forClass(IouUser.class);verify(persons).insert(person.capture());
        assertEquals(123L,person.getValue().getSysUserId());assertEquals(2,person.getValue().getIdVerifyStatus());
        assertEquals(0,person.getValue().getVerifyStatus());assertNull(person.getValue().getVerifiedAt());
        assertEquals("test-encrypted",person.getValue().getIdNoEncrypted()); assertEquals("v1",person.getValue().getAgreementVersion());
        assertNull(person.getValue().getPassword());assertNull(person.getValue().getMobile());
        org.mockito.InOrder order=inOrder(verifier,transactions,users,persons);
        order.verify(verifier).iDCardVerify(any(),any());order.verify(transactions).getTransaction(any());
        order.verify(users).create(any());order.verify(persons).insert(any());order.verify(transactions).commit(any());
    }
    @Test void duplicateIdentityNeverCallsPaidProvider() throws Exception {
        when(persons.selectCount(any())).thenReturn(1L);
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        verifyNoInteractions(verifier,transactions);
    }
    @Test void existingDeterministicUsernameNeverCallsPaidProviderOrOverwritesAccount() throws Exception {
        when(users.findByName("iou_"+identity.hash("110101199001010010"))).thenReturn(new User());
        assertEquals("该身份已注册，请直接登录", assertThrows(BadRequestException.class,
            () -> service.register(request(),"127.0.0.1")).getMessage());
        verifyNoInteractions(verifier,transactions);
        verify(users,never()).create(any());
    }
    @Test void repeatedRegistrationPreservesOriginalAccountAndTrimmedNickname() throws Exception {
        when(persons.selectCount(any())).thenReturn(0L,0L,1L);
        IouRegisterRequest first=request();first.setRealName(" 测试姓名 ");
        service.register(first,"127.0.0.1");
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        ArgumentCaptor<User> accounts=ArgumentCaptor.forClass(User.class);
        verify(users).create(accounts.capture());
        assertEquals("iou_"+identity.hash(first.getIdNo()),accounts.getValue().getUsername());
        assertEquals("测试姓名",accounts.getValue().getNickName());
        verify(verifier).iDCardVerify(any(),any());
    }
    @Test void usernameUniqueConstraintRaceRollsBackAccountCreation() throws Exception {
        doThrow(new org.springframework.dao.DuplicateKeyException("username collision")).when(users).create(any());
        assertEquals("该身份已注册，请直接登录",assertThrows(BadRequestException.class,
            () -> service.register(request(),"127.0.0.1")).getMessage());
        verify(transactions).rollback(any());verify(transactions,never()).commit(any());
        verify(persons,never()).insert(any());
    }
    @Test void usernamePrecheckRaceReturnsDuplicateMessage() throws Exception {
        doThrow(new me.zhengjie.exception.EntityExistException(User.class,"username","synthetic-name"))
            .when(users).create(any());
        assertEquals("该身份已注册，请直接登录",assertThrows(BadRequestException.class,
            () -> service.register(request(),"127.0.0.1")).getMessage());
        verify(transactions).rollback(any());verify(persons,never()).insert(any());
    }
    @Test void invalidCaptchaNeverCallsPaidProvider() throws Exception {
        doThrow(new BadRequestException("验证码错误")).when(guard).captcha(any(),any());
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));verifyNoInteractions(verifier,transactions);
    }
    @Test void missingSeedNeverCallsPaidProvider() throws Exception {
        when(depts.selectById(IouAccountService.DEPT_ID)).thenReturn(null);
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));verifyNoInteractions(verifier);
    }
    @Test void agreementMustBeReadyAndCurrent() throws Exception {
        assertFalse((Boolean)service("").options().get("enabled"));
        assertThrows(BadRequestException.class,()->service("").register(request(),"127.0.0.1"));
        IouRegisterRequest r=request();r.setAgreementVersion("v0");
        assertThrows(BadRequestException.class,()->service.register(r,"127.0.0.1"));verifyNoInteractions(verifier,guard);
    }
    @Test void failedVerificationCanBeRetriedImmediatelyByUser() throws Exception {
        when(verifier.iDCardVerify(any(),any())).thenThrow(new BadRequestException("核验失败"))
            .thenReturn(true);
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        assertDoesNotThrow(()->service.register(request(),"127.0.0.1"));
        verify(verifier,times(2)).iDCardVerify(any(),any());
        verify(users).create(any());
    }
    @Test void separateIdentityConsentIsRequiredBeforeAnyProcessing() throws Exception {
        IouRegisterRequest r=request();r.setIdentityConsentAccepted(null);
        assertThrows(BadRequestException.class,()->service.register(r,"127.0.0.1"));
        r.setIdentityConsentAccepted(false);
        assertThrows(BadRequestException.class,()->service.register(r,"127.0.0.1"));
        verifyNoInteractions(identity,verifier,guard,transactions);
    }
    @Test void minorsAndInvalidBirthDatesAreRejectedBeforeFee() throws Exception {
        when(identity.normalize(any())).thenReturn("110101201501010010");
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        when(identity.normalize(any())).thenReturn("110101199002300010");
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        verifyNoInteractions(verifier,guard,transactions);
    }
    @Test void providerFailureDoesNotCreateAnyAccount() throws Exception {
        when(verifier.iDCardVerify(any(),any())).thenThrow(new BadRequestException("核验失败"));
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        verify(users,never()).create(any());verifyNoInteractions(transactions);
    }
    @Test void businessInsertFailureRollsBackSystemAccount() throws Exception {
        when(persons.insert(any())).thenThrow(new IllegalStateException("storage failure"));
        assertThrows(IllegalStateException.class,()->service.register(request(),"127.0.0.1"));
        verify(transactions).rollback(any()); verify(transactions,never()).commit(any());
    }
    @Test void concurrentDuplicateAtPersistenceIsRolledBack() throws Exception {
        when(persons.selectCount(any())).thenReturn(0L,1L);
        assertThrows(BadRequestException.class,()->service.register(request(),"127.0.0.1"));
        verify(users,never()).create(any());verify(transactions).rollback(any());
    }
    @Test void rejectsPlaintextAndTooShortPasswordBeforeFee() throws Exception {
        IouRegisterRequest r=request();r.setPassword("plaintext");assertThrows(BadRequestException.class,()->service.register(r,"127.0.0.1"));
        r.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"123"));
        assertThrows(BadRequestException.class,()->service.register(r,"127.0.0.1"));verifyNoInteractions(verifier,guard);
    }
    @Test void rejectsInvalidNumericPasswordBeforeCaptchaOrFee() throws Exception {
        for (String password : Arrays.asList("1234567", "abc123", "12 345", "123456\n", "１２３４５６")) {
            IouRegisterRequest r=request();
            r.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),password));
            assertEquals("密码必须为 6 位数字", assertThrows(BadRequestException.class,
                () -> service.register(r,"127.0.0.1")).getMessage());
        }
        verifyNoInteractions(verifier,guard,passwords,transactions);
        verify(users,never()).create(any());
    }
    @Test void leadingZeroIsPreservedWhenEncodingRegisteredPassword() throws Exception {
        IouRegisterRequest r=request();
        r.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"012345"));
        service.register(r,"127.0.0.1");
        verify(passwords).encode("012345");
    }
    @Test void loginResolvesOnlyEnabledIouAccount() {
        IouUser person=new IouUser();person.setSysUserId(123L);when(persons.selectOne(any())).thenReturn(person);
        User account=new User();account.setId(123L);account.setUsername("iou_internal");account.setUserType("IOU");account.setEnabled(true);
        when(users.getById(123L)).thenReturn(account);assertEquals("iou_internal",service.resolveLogin("110101199001010010"));
        account.setEnabled(false);assertThrows(BadRequestException.class,()->service.resolveLogin("110101199001010010"));
        account.setEnabled(true);account.setUserType("BACKEND");assertThrows(BadRequestException.class,()->service.resolveLogin("110101199001010010"));
    }
    @Test void profileNeverReturnsIdentityCipherOrPassword() {
        IouUser person=new IouUser();person.setId(1L);person.setSysUserId(123L);person.setIdNoEncrypted("secret");person.setIdNoHash("secret-hmac");
        when(persons.selectOne(any())).thenReturn(person);
        Map<String,Object> result=service.profile(123L);assertFalse(result.containsKey("idNoEncrypted"));assertFalse(result.containsKey("idNoHash"));
        assertFalse(result.containsKey("password")); assertEquals("1",result.get("id"));
        ArgumentCaptor<Wrapper> query=ArgumentCaptor.forClass(Wrapper.class);verify(persons).selectOne(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("sys_user_id"));
    }
    @Test void iouCannotAccessBackendEvenIfRolesAreExpanded() {
        IouAccessPolicy policy=new IouAccessPolicy(users);User account=new User();account.setEnabled(true);account.setUserType("IOU");
        when(users.getLoginData("iou_internal")).thenReturn(account);
        UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("iou_internal",null,Collections.emptyList());
        assertTrue(policy.canAccessH5(auth));assertFalse(policy.canAccessBackend(auth));
        account.setEnabled(false);assertFalse(policy.canAccessH5(auth));account.setEnabled(true);account.setUserType("BACKEND");
        assertTrue(policy.canAccessBackend(auth));assertFalse(policy.canAccessH5(auth));assertFalse(policy.canAccessBackend(null));
    }
}
