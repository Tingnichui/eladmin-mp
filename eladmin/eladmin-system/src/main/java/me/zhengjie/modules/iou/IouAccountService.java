package me.zhengjie.modules.iou;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.sql.Timestamp;
import java.util.*;

@Service
public class IouAccountService {
    public static final long DEPT_ID = 920001L, JOB_ID = 920001L, ROLE_ID = 920001L;
    private final IouUserMapper iouUsers;
    private final UserService users;
    private final DeptMapper depts;
    private final JobMapper jobs;
    private final RoleMapper roles;
    private final IouIdentitySupport identity;
    private final YzhIdentityVerifier verifier;
    private final PasswordEncoder passwords;
    private final IouRequestGuard guard;
    private final TransactionTemplate transaction;
    private final String agreementVersion, userAgreementUrl, privacyPolicyUrl;

    public IouAccountService(IouUserMapper iouUsers, UserService users, DeptMapper depts, JobMapper jobs,
        RoleMapper roles, IouIdentitySupport identity, YzhIdentityVerifier verifier,
        PasswordEncoder passwords, IouRequestGuard guard, PlatformTransactionManager manager,
        @Value("${iou.registration.agreement-version:}") String agreementVersion,
        @Value("${iou.registration.user-agreement-url:}") String userAgreementUrl,
        @Value("${iou.registration.privacy-policy-url:}") String privacyPolicyUrl) {
        this.iouUsers=iouUsers; this.users=users; this.depts=depts; this.jobs=jobs; this.roles=roles;
        this.identity=identity; this.verifier=verifier; this.passwords=passwords;
        this.guard=guard; this.transaction=new TransactionTemplate(manager);
        this.agreementVersion=agreementVersion; this.userAgreementUrl=userAgreementUrl; this.privacyPolicyUrl=privacyPolicyUrl;
    }

    public Map<String,Object> options() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("enabled", ready()); result.put("agreementVersion", agreementVersion);
        result.put("userAgreementUrl", userAgreementUrl); result.put("privacyPolicyUrl", privacyPolicyUrl);
        return result;
    }

    private boolean ready() {
        return !agreementVersion.trim().isEmpty() && secureUrl(userAgreementUrl) && secureUrl(privacyPolicyUrl);
    }
    private boolean secureUrl(String url) {
        try {java.net.URI uri=java.net.URI.create(url); return "https".equals(uri.getScheme()) && uri.getHost()!=null && uri.getRawUserInfo()==null;}
        catch (IllegalArgumentException e) {return false;}
    }

    public void register(IouRegisterRequest request, String remoteAddress) {
        if (!ready()) throw new BadRequestException("注册暂未开放：用户协议与隐私政策尚未配置");
        if (!Boolean.TRUE.equals(request.getAgreementAccepted()) || !agreementVersion.equals(request.getAgreementVersion())) {
            throw new BadRequestException("请阅读并同意最新用户协议与隐私政策");
        }
        if (!Boolean.TRUE.equals(request.getIdentityConsentAccepted())) {
            throw new BadRequestException("请单独同意实名核验授权");
        }
        String name=request.getRealName()==null?"":request.getRealName().trim();
        if (name.isEmpty() || name.length()>64) throw new BadRequestException("请填写真实姓名");
        String idNo=identity.normalize(request.getIdNo());
        try {
            java.time.LocalDate birthday=java.time.LocalDate.parse(idNo.substring(6,14),
                java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(java.time.format.ResolverStyle.STRICT));
            if (birthday.isAfter(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusYears(18))) {
                throw new BadRequestException("仅限年满18周岁的成年人注册");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException("身份证出生日期不正确");
        }
        String hash=identity.hash(idNo);
        String password=decryptPassword(request.getPassword());
        guard.captcha(request.getUuid(), request.getCode());
        checkUnused(hash);
        Dept dept=depts.selectById(DEPT_ID); Job job=jobs.selectById(JOB_ID); Role role=roles.selectById(ROLE_ID);
        if (dept==null || !Boolean.TRUE.equals(dept.getEnabled()) || job==null || !Boolean.TRUE.equals(job.getEnabled())
            || role==null || !"借条用户".equals(role.getName()) || !"借条用户".equals(dept.getName()) || !"借条用户".equals(job.getName())) {
            throw new BadRequestException("注册账号基础配置不可用");
        }
        String encrypted=identity.encrypt(idNo);
        String passwordHash=passwords.encode(password);
        // 外部网络调用不占用数据库事务；三个关联表与业务身份在同一事务提交。
        if (!verifier.iDCardVerify(name, idNo)) throw new BadRequestException("姓名与身份证核验未通过");
        try {
            transaction.execute(status -> {
                checkUnused(hash);
                User account=new User();
                account.setUsername("iou_"+hash);
                account.setNickName(name);
                account.setUserType("IOU"); account.setEnabled(true); account.setPassword(passwordHash);
                account.setDept(dept); account.setJobs(Collections.singleton(job)); account.setRoles(Collections.singleton(role));
                users.create(account);
                if (account.getId()==null) throw new IllegalStateException("系统账号创建失败");
                IouUser person=new IouUser(); Timestamp now=new Timestamp(System.currentTimeMillis());
                person.setSysUserId(account.getId()); person.setRealName(name); person.setIdNoHash(hash); person.setIdNoEncrypted(encrypted);
                person.setVerifyStatus(0); person.setIdVerifyStatus(2); person.setIdVerifiedAt(now);
                person.setAgreementVersion(agreementVersion); person.setAgreementAcceptedAt(now);
                if (iouUsers.insert(person)!=1) throw new IllegalStateException("借条身份创建失败");
                return null;
            });
        } catch (org.springframework.dao.DuplicateKeyException | me.zhengjie.exception.EntityExistException e) {
            throw new BadRequestException("该身份已注册，请直接登录");
        }
    }

    static String decryptPassword(String encrypted) {
        String password;
        try {password=RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, encrypted);}
        catch (Exception e) {throw new BadRequestException("密码加密格式不正确");}
        IouPasswordPolicy.validate(password);
        return password;
    }

    private void checkUnused(String hash) {
        if (iouUsers.selectCount(new LambdaQueryWrapper<IouUser>().eq(IouUser::getIdNoHash,hash))>0
            || users.findByName("iou_"+hash)!=null) {
            throw new BadRequestException("该身份已注册，请直接登录");
        }
    }

    /** 当前只开放身份证登录；手机号须后续短信验证并唯一绑定后开放。 */
    public String resolveLogin(String idNo) {
        IouUser person=iouUsers.selectOne(new LambdaQueryWrapper<IouUser>().eq(IouUser::getIdNoHash,identity.hash(idNo))
            .eq(IouUser::getIdVerifyStatus,2));
        User account=person==null?null:users.getById(person.getSysUserId());
        if (account==null || !"IOU".equals(account.getUserType()) || !Boolean.TRUE.equals(account.getEnabled())) {
            throw new BadRequestException("账号或密码错误");
        }
        return account.getUsername();
    }

    public Map<String,Object> profile(long sysUserId) {
        IouUser person=iouUsers.selectOne(new LambdaQueryWrapper<IouUser>().eq(IouUser::getSysUserId,sysUserId));
        if (person==null) throw new BadRequestException("借条身份不存在");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id", person.getId().toString()); result.put("realName", person.getRealName());
        result.put("idVerifyStatus", person.getIdVerifyStatus()); result.put("verifyStatus", person.getVerifyStatus());
        result.put("mobileBound", person.getMobile()!=null); return result;
    }
}
