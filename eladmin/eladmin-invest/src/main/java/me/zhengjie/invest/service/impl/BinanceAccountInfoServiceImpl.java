/*
 *  Copyright 2019-2023 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.vo.BinanceAccountInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceAccountInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-08-02
 **/
@Service
@RequiredArgsConstructor
public class BinanceAccountInfoServiceImpl extends ServiceImpl<BinanceAccountInfoMapper, BinanceAccountInfo> implements BinanceAccountInfoService {

    private final BinanceAccountInfoMapper binanceAccountInfoMapper;
    private final DataSecurityUtil dataSecurityUtil;
    private final BinanceSpotUtil binanceSpotUtil;

    @Override
    public PageResult<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria, Page<Object> page) {
        return PageUtil.toPage(binanceAccountInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria) {
        return binanceAccountInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceAccountInfo resources) {
        this.checkApiValid(resources);
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceAccountInfo resources) {
        BinanceAccountInfo binanceAccountInfo = getById(resources.getId());
        binanceAccountInfo.copy(resources);
        this.checkApiValid(binanceAccountInfo);
        saveOrUpdate(binanceAccountInfo);
    }

    private void checkApiValid(BinanceAccountInfo accountInfo) {
        String apiKey = accountInfo.getApiKey();
        String apiSecret = accountInfo.getApiSecret();
        if (StringUtils.isNoneBlank(apiKey, apiSecret)) {
            try {
                BinanceAccountInfo tempInfo = new BinanceAccountInfo();
                tempInfo.setApiKey(dataSecurityUtil.decrypt(apiKey));
                tempInfo.setApiSecret(dataSecurityUtil.decrypt(apiSecret));

                BinanceAccountContextHolder.runWith(tempInfo, () -> {
                    JSONObject account = binanceSpotUtil.account();
                    Integer uid = account.getInteger("uid");
                    accountInfo.setUid(uid);
                    accountInfo.setApiValidFlag(1);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceAccountInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceAccountInfo binanceAccountInfo : all) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("身份证姓名", binanceAccountInfo.getIdCardName());
            map.put("用户编号", binanceAccountInfo.getUid());
            map.put("手机号，加密", binanceAccountInfo.getPhoneNumber());
            map.put("邮箱，加密", binanceAccountInfo.getEmail());
            map.put(" totalInvestment", binanceAccountInfo.getTotalInvestment());
            map.put("apiKey，加密", binanceAccountInfo.getApiKey());
            map.put("apiSecret，加密", binanceAccountInfo.getApiSecret());
            map.put("备注", binanceAccountInfo.getRemark());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<BinanceAccountInfo> listUseApiAccount() {
        // 查询所有API正常的账号
        List<BinanceAccountInfo> accountInfoList = this.list(
                Wrappers.lambdaQuery(BinanceAccountInfo.class)
                        .eq(BinanceAccountInfo::getApiValidFlag, 1)
        );
        return filterApiValidAccount(accountInfoList);
    }

    @Override
    public BinanceAccountInfo getAccountByIdCardName(String idCardName) {
        BinanceAccountInfo accountInfo = this.getOne(Wrappers.lambdaQuery(BinanceAccountInfo.class).eq(BinanceAccountInfo::getIdCardName, "耿辉"));
        return this.decryptApiInfo(accountInfo);
    }

    private List<BinanceAccountInfo> filterApiValidAccount(List<BinanceAccountInfo> accountInfoList) {
        return accountInfoList.stream()
                .filter(v -> StringUtils.isNoneBlank(v.getApiKey(), v.getApiSecret()))
                .map(this::decryptApiInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BinanceAccountInfo decryptApiInfo(BinanceAccountInfo accountInfo) {
        if (null == accountInfo) {
            return null;
        }
        try {
            accountInfo.setApiKey(dataSecurityUtil.decrypt(accountInfo.getApiKey()));
            accountInfo.setApiSecret(dataSecurityUtil.decrypt(accountInfo.getApiSecret()));
            return accountInfo;
        } catch (Exception e) {
            log.error(accountInfo.getIdCardName() + "，解密出现异常", e);
            return null;
        }
    }

    @Override
    public List<BinanceAccountInfo> listAutoTradeAccount() {
        // 查询所有自动交易且API正常的账号
        List<BinanceAccountInfo> accountInfoList = this.list(
                Wrappers.lambdaQuery(BinanceAccountInfo.class)
                        .eq(BinanceAccountInfo::getAutoTradeFlag, 1)
                        .eq(BinanceAccountInfo::getApiValidFlag, 1)
        );

        return filterApiValidAccount(accountInfoList);
    }

    @Override
    public void changeAutoTradeFlag(Integer id) {
        BinanceAccountInfo accountInfo = this.getById(id);
        if (null == accountInfo) {
            throw new BadRequestException("账户信息不存在");
        }
        if (accountInfo.getApiValidFlag() != 1) {
            throw new BadRequestException("账户API不可用");
        }

        this.update(
                Wrappers.lambdaUpdate(BinanceAccountInfo.class)
                        .eq(BinanceAccountInfo::getId, id)
                        .set(BinanceAccountInfo::getAutoTradeFlag, 1 ^ accountInfo.getAutoTradeFlag())
        );

    }

    @Override
    public BinanceAccountInfo getAccountByUid(Integer uid) {
        BinanceAccountInfo accountInfo = this.getOne(
                Wrappers.lambdaQuery(BinanceAccountInfo.class)
                        .eq(BinanceAccountInfo::getUid, uid)
        );

        return this.decryptApiInfo(accountInfo);
    }
}