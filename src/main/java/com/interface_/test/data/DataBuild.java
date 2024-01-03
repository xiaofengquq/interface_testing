package com.interface_.test.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.interface_.test.util.Util;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBuild {

    private final ReentrantLock lock = new ReentrantLock();
    private final Pattern pattern = Pattern.compile("^(.+)\\.(.+)$");
    private final Pattern methodPattern = Pattern.compile("^(.+)\\(([^()]*)\\)$");



    private final Logger logger = LoggerFactory.getLogger(DataBuild.class);

    private final ObjectMapper mapper = new ObjectMapper();

    UrlData parseUrlAndSignRow(Map<Integer, Object> data) {
        UrlData urlData = new UrlData();
        urlData.setUrlName(data.get(0).toString());
        urlData.setUrlValue(data.get(1).toString());
        if (Objects.nonNull(data.get(2))) {
            urlData.setSignName(data.get(2).toString());
        }
        if (Objects.nonNull(data.get(3))) {
            urlData.setSignValue(data.get(3).toString());
        }
        return urlData;
    }

    String[] parseProperties(Map<Integer, Object> data, UrlData urlData) {
        String[] properties = new String[data.size()];

        data.forEach((l, v) -> {
            properties[l] = v.toString().startsWith("#") ? v.toString().substring(1).trim() : v.toString().trim();
        });
        return properties;
    }

    TestData buildData(Map<Integer, Object> data, String[] properties, UrlData urlData, int index) {
        TestData td = new TestData();
        data.forEach((l, v) -> {
            if (properties[l].startsWith("$")) {
                td.getParameters().put(properties[l].substring(1), Objects.isNull(v) ? "" : v.toString());
            }
            String content = parseContent(Objects.isNull(v) ? "" : v.toString().trim(), () -> null);
            setValToTestData(properties[l], content, td);
        });
        td.setUrl(urlData.getUrlValue());
        if (Objects.nonNull(urlData.getSignValue())) {
            td.setSignKey(urlData.getSignValue());
        }
        String urlVal = urlData.getUrlValue().substring(urlData.getUrlValue().lastIndexOf("/") + 1);

        td.setId(urlVal + "0" + index + "_" + urlVal);
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(td));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return td;
    }

    private void setValToTestData(String property, String content, TestData data) {
        switch (property) {
            case "URL":
                data.setUrl(content);
                break;
            case "SignKey":
                data.setSignKey(content);
                break;
            case "Expect":
                data.setExpect(content);
                break;
            case "Assert":
                data.setAssert_(content);
                break;
            case "Setup":
                data.setSetup(content);
                break;
            case "TearDown":
                data.setTearDown(content);
                break;
            case "HttpMethod":
                data.setHttpMethod(content);
                break;
            case "Status":
                data.setStatus(content);
                break;
            case "Description":
                data.setDescription(content);
                break;
            default:
                break;
        }
    }

    public String parseContent(String content, Supplier<Void> elseContent) {
        String autoContent = content;
        switch (content) {
            case "$EMPTY":
               autoContent = "";
               break;
            case "$NULL":
                autoContent = null;
                break;
            case "$TIMESTAMP":
                try {
                    lock.lock();
                    autoContent = Long.toString(System.currentTimeMillis());
                } finally {
                    lock.unlock();
                }
                break;
            case "$TIME":
                try {
                    lock.lock();
                    autoContent = Long.toString(System.currentTimeMillis() / 1000);
                } finally {
                    lock.unlock();
                }
                break;
            case "$RANDOM":
                autoContent = Util.randomString(8);
                break;
            default:
                elseContent.get();
                break;
        }
        return autoContent;
    }

    public Object parseVal(String content) {
        if (content.startsWith("$java:")) {
            return parseObjectVal(content.substring(6));
        } else if (content.startsWith("$")) {
            return parseContent(content, () -> {throw new IllegalArgumentException("val is not valid");});
        }
        return content;
    }

    public String[] parseArgumentList(String[] arguments) {
        try {
            String[] return_ = new String[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                String arg = arguments[i];
                Object val = arg;
                if (arg.startsWith("$")) {
                    val = arg.startsWith("$java:") ?
                            parseObjectVal(arg.substring(6)) : parseContent(arg, () -> {
                        throw new IllegalArgumentException("argument is not valid");
                    });
                }
                return_[i] = mapper.writeValueAsString(val);
            }
            return return_;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
    }

    public Object parseObjectVal(String classString) {
        try {
            Matcher matcher = pattern.matcher(classString);
            if (matcher.find()) {
                String class_ = matcher.group(1);
                String property_ = matcher.group(2);
                Class<?> class__ = Class.forName(class_);
                Object object = class__.newInstance();
                if (property_.contains("(")) {
                    Matcher methodMather = methodPattern.matcher(property_);
                    if (!(methodMather.find())) {
                        throw new IllegalArgumentException("method string is not valid!");
                    }
                    String method = methodMather.group(1);
                    String[] argumentList = parseArgumentList(methodMather.group(2).split(","));
                    Method method_ = class__.getDeclaredMethod(method, Object[].class);
                    Class<?>[] type = method_.getParameterTypes();
                    Object[] sourceArgument = new Object[argumentList.length];
                    for (int i = 0; i < argumentList.length; i++) {
                        sourceArgument[i] = mapper.readValue(argumentList[i], type[i]);
                    }
                    method_.setAccessible(true);
                    Object return_ = method_.invoke(object, sourceArgument);
                    method_.setAccessible(false);
                    return return_;
                }
                Field field = class__.getDeclaredField(property_);
                field.setAccessible(true);
                Object return_ = field.get(object);
                field.setAccessible(false);
                return return_;
            }
            throw new IllegalArgumentException("class string is not valid!");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
    }
    TestData parseData(Map<Integer, Object> data, String[] properties) {
        TestData r = new TestData();
        data.forEach((l, v) -> {
            if (properties[l].startsWith("$")) {
                r.getParameters().put(properties[l].substring(1), v.toString());
            } else {

            }
        });
        return r;
    }



    @Getter @Setter
    public static class UrlData {
        private String urlName;
        private String urlValue;

        private String signName;

        private String signValue;
    }
}
