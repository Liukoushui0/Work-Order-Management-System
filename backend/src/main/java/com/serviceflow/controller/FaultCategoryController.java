package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.system.FaultCategoryQuery;
import com.serviceflow.dto.system.FaultCategoryRequest;
import com.serviceflow.service.FaultCategoryService;
import com.serviceflow.vo.FaultCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 故障分类管理接口。
 */
@Tag(name = "故障分类")
@RestController
@RequestMapping("/api/v1/fault-categories")
public class FaultCategoryController {

    private final FaultCategoryService faultCategoryService;

    public FaultCategoryController(FaultCategoryService faultCategoryService) {
        this.faultCategoryService = faultCategoryService;
    }

    @Operation(summary = "故障分类分页列表")
    @PreAuthorize("hasAuthority('fault-category:view')")
    @GetMapping
    public Result<PageResult<FaultCategoryVO>> page(@Valid FaultCategoryQuery query) {
        return Result.success(faultCategoryService.page(query));
    }

    @Operation(summary = "启用状态的故障分类", description = "供工单创建、筛选下拉使用")
    @PreAuthorize("hasAuthority('fault-category:view')")
    @GetMapping("/enabled")
    public Result<List<FaultCategoryVO>> enabled() {
        return Result.success(faultCategoryService.listEnabled());
    }

    @Operation(summary = "新增故障分类")
    @PreAuthorize("hasAuthority('fault-category:manage')")
    @OperationLogRecord(module = "fault-category", operation = "新增故障分类")
    @PostMapping
    public Result<FaultCategoryVO> create(@Valid @RequestBody FaultCategoryRequest request) {
        return Result.success(faultCategoryService.create(request));
    }

    @Operation(summary = "修改故障分类")
    @PreAuthorize("hasAuthority('fault-category:manage')")
    @OperationLogRecord(module = "fault-category", operation = "修改故障分类")
    @PutMapping("/{id}")
    public Result<FaultCategoryVO> update(@PathVariable Long id,
                                          @Valid @RequestBody FaultCategoryRequest request) {
        return Result.success(faultCategoryService.update(id, request));
    }

    @Operation(summary = "删除故障分类")
    @PreAuthorize("hasAuthority('fault-category:manage')")
    @OperationLogRecord(module = "fault-category", operation = "删除故障分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        faultCategoryService.delete(id);
        return Result.success();
    }
}
