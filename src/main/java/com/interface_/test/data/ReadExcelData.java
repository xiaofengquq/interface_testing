package com.interface_.test.data;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 读取Excel数据监听器
 */
@RequiredArgsConstructor
public class ReadExcelData extends AnalysisEventListener<Map<Integer, Object>> {

    /**
     * list队列，存储解析后的数据
     */
    private final Deque<TestData> list = new ArrayDeque<>();

    /**
     * 可重入锁，用于同步对数据的操作
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 用于线程间等待的倒计时闩锁
     */
    private final CountDownLatch latch;

    /**
     * 标记是否已经执行过doAfterAllAnalysed方法
     */
    private final AtomicBoolean isAfter = new AtomicBoolean(false);

    /**
     * 数据构建器，用于构建TestData对象
     */
    private final DataBuild dataBuild = new DataBuild();

    /**
     * 当前处理的行号
     */
    private int line = 0;

    /**
     * 存储解析出的URL和签名行数据
     */
    private DataBuild.UrlData urlData;

    /**
     * 存储解析出的属性名数组
     */
    private String[] properties;

    /**
     * 标记当前行是否为空行
     */
    private boolean isEmpty = false;

    /**
     * 每读取一行数据时调用此方法
     *
     * @param data    当前行的数据
     * @param context 分析上下文
     */
    @Override
    public void invoke(Map<Integer, Object> data, AnalysisContext context) {
        try {
            lock.lock();
            if (line == 0) {
                urlData = dataBuild.parseUrlAndSignRow(data);
            } else if (line == 1) {
                properties = dataBuild.parseProperties(data, urlData);
            } else {
                if (!(isEmpty)) {
                    TestData td = dataBuild.buildData(data, properties, urlData, line);
                    list.add(td);
                    if ("".equals(td.getExpect())) {
                        isEmpty = true;
                    }
                }
            }
            line++;
        } finally {
            lock.unlock();
        }
        if (isAfter.compareAndSet(true, false)) {
            latch.countDown();
        }
    }

    /**
     * 所有数据解析完成后调用此方法
     *
     * @param context 分析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        isAfter.compareAndSet(false, true);
        latch.countDown();
    }

    /**
     * 获取解析后的数据
     *
     * @return 解析后的数据集合
     */
    public Collection<TestData> getData() {
        return list;
    }
}
