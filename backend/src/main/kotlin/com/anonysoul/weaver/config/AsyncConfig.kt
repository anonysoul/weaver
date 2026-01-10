package com.anonysoul.weaver.config

import com.anonysoul.weaver.session.application.SessionExecutionProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    /**
     * 会话初始化专用线程池，避免占用默认异步执行器
     */
    @Bean(name = ["sessionInitializerExecutor"])
    fun sessionInitializerExecutor(properties: SessionExecutionProperties): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = properties.corePoolSize
            maxPoolSize = properties.maxPoolSize
            queueCapacity = properties.queueCapacity
            setThreadNamePrefix(properties.threadNamePrefix)
            initialize()
        }
}
