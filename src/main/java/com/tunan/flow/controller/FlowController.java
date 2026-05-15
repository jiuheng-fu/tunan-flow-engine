package com.tunan.flow.controller;

import com.tunan.flow.common.Result;
import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.engine.FlowExecutor;
import com.tunan.flow.entity.FlowDefinition;
import com.tunan.flow.service.FlowPublishService;
import com.tunan.flow.service.FlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/flow")
public class FlowController {

    @Autowired
    private FlowService flowService;

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private FlowPublishService flowPublishService;



    @GetMapping("/{flowId}")
    public Result<FlowDefinitionDTO> loadFlow(@PathVariable String flowId) {
        FlowDefinitionDTO flowDef = flowService.loadFlowDefinition(flowId);
        return Result.success(flowDef);
    }


    @GetMapping("/list")
    @ResponseBody
    public Result<List<FlowDefinition>> loadFlow() {
        List<FlowDefinition> flowList = flowService.loadFlow();
        return Result.success(flowList);
    }

    /**
     * 保存流程定义
     */
    @PostMapping
    public Result<FlowDefinition> saveFlow(@RequestBody FlowDefinitionDTO flowDef) {
        log.info("保存流程定义: {}", flowDef);
        // 1. 保存到数据库
        FlowDefinition flow = flowService.create(flowDef);

        // 2. 返回带ID的流程
        return Result.success(flow);
    }

    /**
     * 设计流程定义
     */
    @PostMapping("/design/{flowId}")
    public Result<FlowDefinition> designFlow(@PathVariable String flowId,@RequestBody FlowDefinitionDTO flowDef) {

        log.info("设计流程定义: {}", flowDef);
        // 1. 保存到数据库
        FlowDefinition flow = flowService.designFlow(flowId,flowDef);
        // 2. 返回带ID的流程
        return Result.success(flow);
    }
    /**
     * 修改流程定义
     */
    @PutMapping("/{flowId}")
    public Result<FlowDefinition> updateFlow(@PathVariable String flowId, @RequestBody FlowDefinitionDTO flowDef) {
        log.info("修改流程定义: {}", flowDef);
        FlowDefinition flow = flowService.updateFlow(flowId, flowDef);
        return Result.success(flow);
    }

    /**
     * 删除流程定义
     */
    @DeleteMapping("/{flowId}")
    public Result<String> deleteFlow(@PathVariable String flowId) {
        // 1. 删除数据库
        flowService.deleteFlow(flowId);
        return Result.success("删除成功");
    }
     /**
     * 发布流程
     */
    @PostMapping("/{flowId}/publish")
    public Result<String> publishFlow(@PathVariable String flowId) {
        flowPublishService.publish(flowId);
        return Result.success("发布成功，接口地址: /api/flow/" + flowId);
    }


}
