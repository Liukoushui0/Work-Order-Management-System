package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.customer.CustomerCreateRequest;
import com.serviceflow.dto.customer.CustomerQuery;
import com.serviceflow.dto.customer.CustomerStatusRequest;
import com.serviceflow.dto.customer.CustomerUpdateRequest;
import com.serviceflow.service.CustomerService;
import com.serviceflow.vo.CustomerVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户资料管理接口。
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "客户分页列表", description = "客户角色只能看到自己的客户档案")
    @PreAuthorize("hasAuthority('customer:view')")
    @GetMapping
    public Result<PageResult<CustomerVO>> page(@Valid CustomerQuery query) {
        return Result.success(customerService.page(query));
    }

    @Operation(summary = "客户下拉选项")
    @PreAuthorize("hasAnyAuthority('customer:view', 'device:view', 'ticket:view')")
    @GetMapping("/options")
    public Result<List<CustomerVO>> options(@RequestParam(required = false) String keyword) {
        return Result.success(customerService.options(keyword));
    }

    @Operation(summary = "客户详情", description = "包含名下设备数量与关联工单数量")
    @PreAuthorize("hasAuthority('customer:view')")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.success(customerService.detail(id));
    }

    @Operation(summary = "新增客户")
    @PreAuthorize("hasAuthority('customer:manage')")
    @OperationLogRecord(module = "customer", operation = "新增客户")
    @PostMapping
    public Result<CustomerVO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return Result.success(customerService.create(request));
    }

    @Operation(summary = "修改客户")
    @PreAuthorize("hasAuthority('customer:manage')")
    @OperationLogRecord(module = "customer", operation = "修改客户")
    @PutMapping("/{id}")
    public Result<CustomerVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CustomerUpdateRequest request) {
        return Result.success(customerService.update(id, request));
    }

    @Operation(summary = "启用或停用客户", description = "存在未完成工单时不允许停用")
    @PreAuthorize("hasAuthority('customer:manage')")
    @OperationLogRecord(module = "customer", operation = "启用或停用客户")
    @PutMapping("/{id}/status")
    public Result<CustomerVO> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody CustomerStatusRequest request) {
        return Result.success(customerService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "删除客户", description = "名下存在设备或未完成工单时不允许删除")
    @PreAuthorize("hasAuthority('customer:manage')")
    @OperationLogRecord(module = "customer", operation = "删除客户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
