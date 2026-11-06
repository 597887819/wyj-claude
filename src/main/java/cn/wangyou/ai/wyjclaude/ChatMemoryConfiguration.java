package cn.wangyou.ai.wyjclaude;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: wyj
 * @Date 2026/11/5 23:22
 * @Describe:
 */
@Configuration
public class ChatMemoryConfiguration {

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor() {
        SessionService sessionService = DefaultSessionService.builder().sessionRepository(InMemorySessionRepository.builder().build()).build();
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId("wyj")
                // 触发器 保留会话的轮次 一问一答为一轮
                .compactionTrigger(new TurnCountTrigger(1))
                // 压缩策略 滑动窗口
                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(1).build())
                .build();
    }

    @Bean
    SessionMemoryAdvisor sessionMemorySummarizationAdvisor(DeepSeekChatModel deepSeekChatModel) {
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();
        SessionService sessionService = DefaultSessionService.builder().sessionRepository(InMemorySessionRepository.builder().build()).build();
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId("wyj")
                .compactionTrigger(new TurnCountTrigger(1))
                // 使用递归总结策略
                .compactionStrategy(RecursiveSummarizationCompactionStrategy.builder(chatClient)
                        // 保留事件的条数 一问一答为两条事件
                        .maxEventsToKeep(10)
                        // 重叠的事件条数 表示除了要给到大模型总结的事件条数，还需要额外拿多少条事件，这样可以让大模型总结得更全面
                        .overlapSize(0)
                        .build())
                .build();
    }
}
