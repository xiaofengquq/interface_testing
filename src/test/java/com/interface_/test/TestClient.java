package com.interface_.test;

import ch.qos.logback.classic.Level;
import com.alibaba.excel.EasyExcel;
import com.interface_.test.data.ReadExcelData;
import com.interface_.test.data.TestData;
import com.interface_.test.test__.TestService;
import com.interface_.test.util.SignatureType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TestClient {

    private final TestData data;


    private static final AtomicBoolean t = new AtomicBoolean(false);

    public TestClient(TestData data) {
        this.data = data;
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        logger.setLevel(Level.INFO);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> prepareData() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ReadExcelData excelData = new ReadExcelData(latch);
        EasyExcel.read(new File("D:\\IdeaProjects\\test\\data\\test.xls"), excelData).sheet("changeChannelAppext")
                        .headRowNumber(-1).doRead();
        latch.await();
        return excelData.getData().stream().map(data -> new Object[]{data})
                        .collect(Collectors.toList());
    }

    @Test
    public void testSend() {
        new TestService().test(data, SignatureType.SERVER);
    }
}
