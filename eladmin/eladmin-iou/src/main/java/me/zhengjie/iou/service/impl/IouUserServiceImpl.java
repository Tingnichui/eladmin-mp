package me.zhengjie.iou.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.domain.dto.IouUserQueryCriteria;
import me.zhengjie.iou.mapper.IouUserMapper;
import me.zhengjie.iou.service.IouUserService;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.utils.*;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/** 业务身份由注册/认证流程维护；密码、启停账号仍由 sys_user 管理。 */
@Service
@RequiredArgsConstructor
public class IouUserServiceImpl extends ServiceImpl<IouUserMapper, IouUser> implements IouUserService {
    private final IouUserMapper iouUserMapper;
    private final RedisUtils redisUtils;
    private final IouIdentitySupport identitySupport;
    @Override
    public PageResult<IouUser> queryAll(IouUserQueryCriteria criteria, Page<Object> page) {
        PageResult<IouUser> result = PageUtil.toPage(iouUserMapper.findAll(criteria,page));
        result.getContent().forEach(user -> user.setMaskedIdNo(identitySupport.mask(user.getIdNoEncrypted())));
        return result;
    }
    @Override
    public String revealIdentity(Long id) {
        IouUser user = iouUserMapper.selectById(id);
        if (user == null) {
            throw new BadRequestException("借条用户不存在");
        }
        return identitySupport.decrypt(user.getIdNoEncrypted());
    }
    @Override
    public void create(IouUser resources) {throw new BadRequestException("请通过注册接口创建借条账号");}
    @Override
    public void update(IouUser resources) {throw new BadRequestException("身份资料与认证状态由认证流程维护；账号管理请使用系统用户页面");}
    @Override
    public void deleteAll(List<Long> ids) {throw new BadRequestException("借条身份不能直接删除；请在系统用户页面停用账号");}
    @Override
    public void download(IouUserQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers=Arrays.asList("系统用户ID", "手机号", "真实姓名", "二要素认证状态", "二要素认证时间", "三要素认证状态", "创建时间");
        FileUtil.downloadExcel(headers,response,writer -> {
            long current=1, size=10000;
            while(true) {
                List<IouUser> records=iouUserMapper.findAll(criteria,new Page<Object>(current,size,false)).getRecords();
                if(records.isEmpty()) break;
                List<Map<String,Object>> rows=new ArrayList<>();
                for(IouUser user:records) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    row.put("系统用户ID",String.valueOf(user.getSysUserId())); row.put("手机号",user.getMobile()); row.put("真实姓名",user.getRealName());
                    row.put("二要素认证状态",Integer.valueOf(2).equals(user.getIdVerifyStatus())?"已通过":"未通过"); row.put("二要素认证时间",user.getIdVerifiedAt());
                    row.put("三要素认证状态",redisUtils.getDictLabel("iou_user_verify_status",user.getVerifyStatus())); row.put("创建时间",user.getCreateTime());
                    rows.add(row);
                }
                writer.write(rows); if(records.size()<size)break; current++;
            }
        });
    }
}
