package cn.wangyou.ai.wyjclaude;

import cn.wangyou.ai.wyjclaude.service.ToolService;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;

import java.util.Scanner;

@SpringBootApplication
public class WyjClaudeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WyjClaudeApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel, Environment environment,
                                        ChatMemory chatMemory, SessionMemoryAdvisor sessionMemorySummarizationAdvisor,
                                        ToolService toolService, ToolCallbackProvider toolCallbackProvider) {
        assertApiKeyResolved(environment);
        // 创建而非直接注入 ChatClient 的原因是避免接入多个模型，导致ChatClient无法决定使用哪个模型
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                // 添加内存 Advisor 为默认的 Advisors，简单理解将每次请求响应放到内存，之后会自动将内存中的会话内容添加到请求中
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(sessionMemorySummarizationAdvisor, SimpleLoggerAdvisor.builder().build())
//                .defaultTools(toolService)
                // 添加文件系统 和 shell 工具
                .defaultTools(FileSystemTools.builder().build(), ShellTools.builder().build())
                // 添加工具回调提供者（MCP)
                .defaultTools(toolCallbackProvider)
                .build();
        return args -> {
            System.out.println("\n我是wyj claude:.\n");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n> 用户: ");
                    String userInput = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(userInput)) {
                        break;
                    }
                    System.out.println("\n> AI: ");
//                    String content = chatClient.prompt().user(userInput).call().content();
//                    System.out.println("\n> AI: " + content);
                    Flux<String> content = chatClient.prompt().user(userInput)
                            // 添加会话 ID 为 "wyj"
//                            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "wyj"))
                            .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "wyj"))
                            .stream().content();
                    content.doOnNext(System.out::print)
                            // 阻塞等待流结束
                            .blockLast();
//                    System.out.println("我不能生成或提供任何非法、有害或危险的内容。请始终确保您的行为符合法律规定，并尊重他人的权利和尊严。");
                }
            }
        };
    }

    /**
     * ${DASHSCOPE_API_KEY} 解析不出来时,Spring AI 不会启动失败,而是把无效值当 api-key 发出去,
     * 最终表现为让人误解的 401 invalid_api_key。这里提前失败并说明原因。
     */
    private static void assertApiKeyResolved(Environment environment) {
        String apiKey = environment.getProperty("spring.ai.deepseek.api-key", "");
        if (apiKey.isBlank() || apiKey.contains("${")) {
            throw new IllegalStateException("未解析到有效的 api-key(binder 实际得到的值是: \"" + apiKey + "\")。\n"
                    + "最常见原因:环境变量 DASHSCOPE_API_KEY 没有传给 IDE 进程 —— macOS 从 Dock 启动的 GUI 应用不加载 ~/.zshrc。\n"
                    + "解决办法(任选一种):\n"
                    + "  1) Run/Debug Configurations -> Environment variables 添加 DASHSCOPE_API_KEY=sk-xxx;\n"
                    + "  2) 用 local profile,在 application-local.yaml 里直接写 spring.ai.deepseek.api-key;\n"
                    + "  3) 在 IDEA 终端执行 ./mvnw spring-boot:run。");
        }
    }
}
