package com.tunan.flow.engine.component.gateway;



import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent Skill执行器
 * 将流程发布为通用 Agent 技能
 */

@Slf4j
@Component
public class AgentSkillExecutor implements ComponentExecutor {

    @Override
    public String getType() {
        return "agent-skill";
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "skill");
        context.setVariable("_skillName", config.get("skillName"));

        log.debug("Agent Skill执行器: skillName={}", config.get("skillName"));

        return input;
    }

    /**
     * 获取技能定义
     */
    public Map<String, Object> getSkillDefinition(Map<String, Object> config) {
        return Map.of(
                "name", config.get("skillName"),
                "description", config.get("skillDescription"),
                "inputSchema", config.getOrDefault("inputSchema", Map.of())
        );
    }
}
