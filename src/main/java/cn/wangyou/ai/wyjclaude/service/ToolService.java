package cn.wangyou.ai.wyjclaude.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @Author: wyj
 * @Date 2026/11/6 13:47
 * @Describe:
 */
@Component
public class ToolService {

    @Tool(description = "执行shell命令")
    public String commandTool(@ToolParam(description = "命令") String command) {
        System.out.println("执行命令: " + command);
        return "命令执行成功！";
    }
}
