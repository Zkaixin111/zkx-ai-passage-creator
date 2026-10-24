package com.zkx.aipassagecreator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置
 * <p>
 * 为文章智能体链路提供独立线程池，避免阻塞 HTTP 请求线程。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 智能体执行线程池
     */
    @Bean(name = "articleTaskExecutor")
    public Executor articleTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("article-agent-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 配图并行抓取线程池
     * <p>
     * ImageFetchAgent 内多张配图通过 CompletableFuture 并行下载/生成/上传 COS，
     * 将串行耗时压缩到单图耗时级别。
     */
    @Bean(name = "imageFetchExecutor")
    public Executor imageFetchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：常驻线程，即使空闲也不会被回收，适合并发抓取 4 张图片
        executor.setCorePoolSize(4);
        // 最大线程数：当队列满时可扩容到的上限，应对突发多图请求
        executor.setMaxPoolSize(8);
        // 工作队列容量：超出核心线程后，任务先进入此队列等待，最多缓存 50 个任务
        executor.setQueueCapacity(50);
        // 线程名前缀：便于在日志和线程堆栈中快速识别该线程池的任务
        executor.setThreadNamePrefix("image-fetch-");
        // 拒绝策略：当队列和最大线程都满时，由调用线程（CompletableFuture 所在线程）直接执行，起到背压作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池，触发内部 ThreadPoolExecutor 的创建
        executor.initialize();
        return executor;
    }
}
