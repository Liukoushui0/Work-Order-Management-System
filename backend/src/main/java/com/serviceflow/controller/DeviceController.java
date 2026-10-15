package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.device.DeviceCreateRequest;
import com.serviceflow.dto.device.DeviceQuery;
import com.serviceflow.dto.device.DeviceUpdateRequest;
import com.serviceflow.service.DeviceService;
import com.serviceflow.vo.DeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * 设备档案管理接口。
 */
@Tag(name = "设备档案")
@Validated
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "设备分页列表", description = "客户角色只能看到自己名下的设备")
    @PreAuthorize("hasAuthority('device:view')")
    @GetMapping
    public Result<PageResult<DeviceVO>> page(@Valid DeviceQuery query) {
        return Result.success(deviceService.page(query));
    }

    @Operation(summary = "设备详情", description = "包含所属客户与关联工单数量")
    @PreAuthorize("hasAuthority('device:view')")
    @GetMapping("/{id}")
    public Result<DeviceVO> detail(@PathVariable Long id) {
        return Result.success(deviceService.detail(id));
    }

    @Operation(summary = "指定客户的可用设备", description = "供工单创建设备下拉使用")
    @PreAuthorize("hasAuthority('device:view')")
    @GetMapping("/options")
    public Result<List<DeviceVO>> optionsByCustomer(@RequestParam @NotNull(message = "客户 ID 不能为空") Long customerId) {
        return Result.success(deviceService.optionsByCustomer(customerId));
    }

    @Operation(summary = "新建设备档案")
    @PreAuthorize("hasAuthority('device:manage')")
    @OperationLogRecord(module = "device", operation = "新建设备档案")
    @PostMapping
    public Result<DeviceVO> create(@Valid @RequestBody DeviceCreateRequest request) {
        return Result.success(deviceService.create(request));
    }

    @Operation(summary = "修改设备档案")
    @PreAuthorize("hasAuthority('device:manage')")
    @OperationLogRecord(module = "device", operation = "修改设备档案")
    @PutMapping("/{id}")
    public Result<DeviceVO> update(@PathVariable Long id,
                                   @Valid @RequestBody DeviceUpdateRequest request) {
        return Result.success(deviceService.update(id, request));
    }

    @Operation(summary = "删除设备档案", description = "存在工单记录时不允许删除")
    @PreAuthorize("hasAuthority('device:manage')")
    @OperationLogRecord(module = "device", operation = "删除设备档案")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.success();
    }
}
