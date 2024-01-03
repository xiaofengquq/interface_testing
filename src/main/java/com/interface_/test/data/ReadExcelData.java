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

@RequiredArgsConstructor
public class ReadExcelData extends AnalysisEventListener<Map<Integer, Object>> {

    private final Deque<TestData> list = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final CountDownLatch latch;



    private final AtomicBoolean isAfter = new AtomicBoolean(false);
    private final DataBuild dataBuild = new DataBuild();

    private int line = 0;

    private DataBuild.UrlData urlData;
    private String[] properties;

    private boolean isEmpty = false;

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

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        isAfter.compareAndSet(false, true);
        latch.countDown();
    }

    public Collection<TestData> getData() {
        return list;
    }
}
