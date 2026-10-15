package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.system.FaultCategoryQuery;
import com.serviceflow.dto.system.FaultCategoryRequest;
import com.serviceflow.entity.FaultCategory;
import com.serviceflow.mapper.FaultCategoryMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.vo.FaultCategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 故障分类管理。
 */
@Service
public class FaultCategoryService {

    private final FaultCategoryMapper faultCategoryMapper;

    public FaultCategoryService(FaultCategoryMapper faultCategoryMapper) {
        this.faultCategoryMapper = faultCategoryMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<FaultCategoryVO> page(FaultCategoryQuery query) {
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Page<FaultCategory> page = query.toPage("sort");
        Page<FaultCategory> result = faultCategoryMapper.selectPage(page,
                Wrappers.<FaultCategory>lambdaQuery()
                        .eq(FaultCategory::getDeleted, 0L)
                        .eq(query.getStatus() != null, FaultCategory::getStatus, query.getStatus())
                        .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                                .like(FaultCategory::getCategoryCode, query.getKeyword())
                                .or()
                                .like(FaultCategory::getCategoryName, query.getKeyword())));
        return PageResult.of(result, FaultCategoryVO::from);
    }

    /**
     * 查询启用状态的分类，供工单创建页下拉选择。
     */
    @Transactional(readOnly = true)
    public List<FaultCategoryVO> listEnabled() {
        return faultCategoryMapper.selectList(Wrappers.<FaultCategory>lambdaQuery()
                        .eq(FaultCategory::getDeleted, 0L)
                        .eq(FaultCategory::getStatus, 1)
                        .orderByAsc(FaultCategory::getSort))
                .stream().map(FaultCategoryVO::from).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FaultCategoryVO create(FaultCategoryRequest request) {
        Long duplicate = faultCategoryMapper.selectCount(Wrappers.<FaultCategory>lambdaQuery()
                .eq(FaultCategory::getCategoryCode, request.getCategoryCode())
                .eq(FaultCategory::getDeleted, 0L));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "分类编码已存在：" + request.getCategoryCode());
        }
        validateParent(request.getParentId(), null);
        FaultCategory entity = new FaultCategory();
        entity.setCategoryCode(request.getCategoryCode());
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        entity.setDescription(request.getDescription());
        entity.setSort(request.getSort() == null ? 0 : request.getSort());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setDeleted(0L);
        faultCategoryMapper.insert(entity);
        return FaultCategoryVO.from(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public FaultCategoryVO update(Long id, FaultCategoryRequest request) {
        FaultCategory entity = requireCategory(id);
        Long duplicate = faultCategoryMapper.selectCount(Wrappers.<FaultCategory>lambdaQuery()
                .eq(FaultCategory::getCategoryCode, request.getCategoryCode())
                .eq(FaultCategory::getDeleted, 0L)
                .ne(FaultCategory::getId, id));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "分类编码已存在：" + request.getCategoryCode());
        }
        validateParent(request.getParentId(), id);
        entity.setCategoryCode(request.getCategoryCode());
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        entity.setDescription(request.getDescription());
        entity.setSort(request.getSort() == null ? 0 : request.getSort());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        faultCategoryMapper.updateById(entity);
        return FaultCategoryVO.from(entity);
    }

    /**
     * 删除分类（逻辑删除：deleted 写入本行 id，便于同名分类重新创建）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FaultCategory entity = requireCategory(id);
        Long children = faultCategoryMapper.selectCount(Wrappers.<FaultCategory>lambdaQuery()
                .eq(FaultCategory::getParentId, id)
                .eq(FaultCategory::getDeleted, 0L));
        if (children != null && children > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "该分类下存在子分类，无法删除");
        }
        faultCategoryMapper.update(null, Wrappers.<FaultCategory>lambdaUpdate()
                .eq(FaultCategory::getId, entity.getId())
                .set(FaultCategory::getDeleted, entity.getId()));
    }

    private FaultCategory requireCategory(Long id) {
        FaultCategory entity = faultCategoryMapper.selectOne(Wrappers.<FaultCategory>lambdaQuery()
                .eq(FaultCategory::getId, id)
                .eq(FaultCategory::getDeleted, 0L));
        if (entity == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "故障分类不存在");
        }
        return entity;
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (selfId != null && selfId.equals(parentId)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "父级分类不能是自己");
        }
        Long exists = faultCategoryMapper.selectCount(Wrappers.<FaultCategory>lambdaQuery()
                .eq(FaultCategory::getId, parentId)
                .eq(FaultCategory::getDeleted, 0L));
        if (exists == null || exists == 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "父级分类不存在");
        }
    }
}
