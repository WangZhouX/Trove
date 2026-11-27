package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为「仓库工作台」的收货验收、授权铸造和履约任务保留入口。
 * Reserves entry points for intake, inspection, authorized minting and fulfillment tasks on the Warehouse Workbench.
 */
@RestController
@RequestMapping("/api/v1/warehouse")
@Tag(name = "仓库管理")
public class WarehouseController {
}
