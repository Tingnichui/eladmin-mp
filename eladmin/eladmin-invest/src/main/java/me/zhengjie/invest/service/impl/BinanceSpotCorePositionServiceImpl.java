/*
*  Copyright 2019-2025 Zheng Jie
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

import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionAdjustRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionCandidate;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionLockRequest;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceSpotCorePositionService;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionQueryCriteria;
import me.zhengjie.invest.mapper.BinanceSpotCorePositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
* @description 服务实现
* @author genghui
* @date 2026-09-19
**/
@Service
@RequiredArgsConstructor
public class BinanceSpotCorePositionServiceImpl extends ServiceImpl<BinanceSpotCorePositionMapper, BinanceSpotCorePosition> implements BinanceSpotCorePositionService {

    private final BinanceSpotCorePositionMapper binanceSpotCorePositionMapper;
    private final BinanceSpotTradeMatchStateMapper binanceSpotTradeMatchStateMapper;

    @Override
    public PageResult<BinanceSpotCorePosition> queryAll(BinanceSpotCorePositionQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceSpotCorePositionMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceSpotCorePosition resources) {
        BinanceSpotCorePositionLockRequest request = new BinanceSpotCorePositionLockRequest();
        request.setUid(resources.getUid());
        request.setSymbol(resources.getSymbol());
        request.setTradeId(resources.getTradeId());
        request.setCoreQty(resources.getCoreQty());
        request.setRemark(resources.getRemark());
        lock(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceSpotCorePosition resources) {
        BinanceSpotCorePositionAdjustRequest request = new BinanceSpotCorePositionAdjustRequest();
        request.setCoreQty(resources.getCoreQty());
        request.setRemark(resources.getRemark());
        adjust(resources.getId(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        throw new BadRequestException("底仓记录需要解除，不能直接删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BinanceSpotCorePosition lock(BinanceSpotCorePositionLockRequest request) {
        validateScope(request.getUid(), request.getSymbol());
        validateQty(request.getCoreQty());
        if (request.getTradeId() == null) {
            throw new BadRequestException("成交 ID 不能为空");
        }
        binanceSpotTradeMatchStateMapper.initializeFromTrades(request.getUid(), request.getSymbol());
        BinanceSpotTradeMatchState state = requireBuyState(
                request.getUid(), request.getSymbol(), request.getTradeId());
        BinanceSpotCorePosition active = binanceSpotCorePositionMapper.findActiveByTradeForUpdate(
                request.getUid(), request.getSymbol(), request.getTradeId());
        if (active != null) {
            throw new BadRequestException("该买入成交已经设置底仓，请直接调整数量");
        }
        validateNotExceedRemaining(request.getCoreQty(), state.getRemainingQty());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        BinanceSpotCorePosition position = new BinanceSpotCorePosition();
        position.setUid(request.getUid());
        position.setSymbol(request.getSymbol());
        position.setTradeId(request.getTradeId());
        position.setCoreQty(request.getCoreQty());
        position.setLockedAt(now);
        position.setRemark(request.getRemark());
        position.setCreateTime(now);
        position.setUpdateTime(now);
        binanceSpotCorePositionMapper.insert(position);
        return position;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BinanceSpotCorePosition adjust(Long id, BinanceSpotCorePositionAdjustRequest request) {
        if (id == null) {
            throw new BadRequestException("底仓记录 ID 不能为空");
        }
        validateQty(request.getCoreQty());
        BinanceSpotCorePosition snapshot = binanceSpotCorePositionMapper.selectById(id);
        if (snapshot == null) {
            throw new BadRequestException("底仓记录不存在");
        }
        BinanceSpotTradeMatchState state = requireBuyState(
                snapshot.getUid(), snapshot.getSymbol(), snapshot.getTradeId());
        BinanceSpotCorePosition position = binanceSpotCorePositionMapper.findByIdForUpdate(id);
        requireActive(position);
        validateNotExceedRemaining(request.getCoreQty(), state.getRemainingQty());
        position.setCoreQty(request.getCoreQty());
        position.setRemark(request.getRemark());
        position.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        binanceSpotCorePositionMapper.updateById(position);
        return position;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BinanceSpotCorePosition release(Long id) {
        if (id == null) {
            throw new BadRequestException("底仓记录 ID 不能为空");
        }
        BinanceSpotCorePosition snapshot = binanceSpotCorePositionMapper.selectById(id);
        if (snapshot == null) {
            throw new BadRequestException("底仓记录不存在");
        }
        requireBuyState(snapshot.getUid(), snapshot.getSymbol(), snapshot.getTradeId());
        BinanceSpotCorePosition position = binanceSpotCorePositionMapper.findByIdForUpdate(id);
        if (position == null) {
            throw new BadRequestException("底仓记录不存在");
        }
        if (position.getReleasedAt() != null) {
            return position;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        position.setReleasedAt(now);
        position.setUpdateTime(now);
        binanceSpotCorePositionMapper.updateById(position);
        return position;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BinanceSpotCorePosition> releaseAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("请选择需要解除的底仓");
        }
        List<BinanceSpotCorePosition> positions = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            positions.add(release(id));
        }
        return positions;
    }

    @Override
    public List<BinanceSpotCorePositionCandidate> listCandidates(Integer uid, String symbol) {
        validateScope(uid, symbol);
        return binanceSpotCorePositionMapper.findCandidates(uid, symbol);
    }

    @Override
    public void download(BinanceSpotCorePositionQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("币安账户用户编号");
        headers.add("现货交易对，如 BTCUSDT");
        headers.add("原始现货买入成交 ID");
        headers.add("锁定为底仓的数量");
        headers.add("设为底仓时间");
        headers.add("解除底仓时间，空表示仍在锁定");
        headers.add("备注");
        headers.add("创建者");
        headers.add("更新者");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<BinanceSpotCorePosition> records = binanceSpotCorePositionMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (BinanceSpotCorePosition binanceSpotCorePosition : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("币安账户用户编号", binanceSpotCorePosition.getUid());
                    map.put("现货交易对，如 BTCUSDT", binanceSpotCorePosition.getSymbol());
                    map.put("原始现货买入成交 ID", getExportValue(binanceSpotCorePosition.getTradeId()));
                    map.put("锁定为底仓的数量", binanceSpotCorePosition.getCoreQty());
                    map.put("设为底仓时间", binanceSpotCorePosition.getLockedAt());
                    map.put("解除底仓时间，空表示仍在锁定", binanceSpotCorePosition.getReleasedAt());
                    map.put("备注", binanceSpotCorePosition.getRemark());
                    map.put("创建者", binanceSpotCorePosition.getCreateBy());
                    map.put("更新者", binanceSpotCorePosition.getUpdateBy());
                    map.put("创建时间", binanceSpotCorePosition.getCreateTime());
                    map.put("更新时间", binanceSpotCorePosition.getUpdateTime());
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }

    private BinanceSpotTradeMatchState requireBuyState(Integer uid, String symbol, Long tradeId) {
        BinanceSpotTradeMatchState state = binanceSpotTradeMatchStateMapper.findBuyStateForUpdate(uid, symbol, tradeId);
        if (state == null) {
            throw new BadRequestException("未找到可设置底仓的买入成交");
        }
        if (state.getRemainingQty() == null || state.getRemainingQty().signum() <= 0) {
            throw new BadRequestException("该买入成交已全部卖出");
        }
        return state;
    }

    private void requireActive(BinanceSpotCorePosition position) {
        if (position == null) {
            throw new BadRequestException("底仓记录不存在");
        }
        if (position.getReleasedAt() != null) {
            throw new BadRequestException("底仓已经解除，不能继续调整");
        }
    }

    private void validateNotExceedRemaining(BigDecimal coreQty, BigDecimal remainingQty) {
        if (remainingQty == null || coreQty.compareTo(remainingQty) > 0) {
            throw new BadRequestException("底仓数量不能超过当前剩余持仓数量");
        }
    }

    private void validateScope(Integer uid, String symbol) {
        if (uid == null) {
            throw new BadRequestException("账户不能为空");
        }
        if (!StringUtils.hasText(symbol)) {
            throw new BadRequestException("交易对不能为空");
        }
    }

    private void validateQty(BigDecimal coreQty) {
        if (coreQty == null || coreQty.signum() <= 0) {
            throw new BadRequestException("底仓数量必须大于 0");
        }
    }
}
