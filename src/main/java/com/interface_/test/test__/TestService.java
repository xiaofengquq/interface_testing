package com.interface_.test.test__;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interface_.test.data.DataBuild;
import com.interface_.test.data.TestData;
import com.interface_.test.util.SignatureType;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.fail;


public class TestService {
    private final Logger logger = LoggerFactory.getLogger(TestService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final DataBuild dataBuild = new DataBuild();


    public void test(TestData data, SignatureType type) {
        if ("skip".equalsIgnoreCase(data.getStatus())) {
            logger.info(data.getId() + " is skipped");
            return;

        }
        try (CloseableHttpClient client = HttpClients.custom()
                .setSSLHostnameVerifier((hostname, session) -> true)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true).build()))
                .build()) {
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
            RestTemplate template = new RestTemplate(factory);
            /**
            for (HttpMessageConverter<?> c : template.getMessageConverters()) {
                if (c instanceof StringHttpMessageConverter) {
                    ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8);
                }
            }
             */
            template.getMessageConverters()
                    .stream()
                    .filter(StringHttpMessageConverter.class::isInstance)
                    .map(StringHttpMessageConverter.class::cast)
                    .forEach(converter -> converter.setDefaultCharset(StandardCharsets.UTF_8));
            /**
            TreeMap<String, Object> maps = new TreeMap<>();
            for (Map.Entry<String, String> r : data.getParameters().entrySet()) {
                if ("appKey".equalsIgnoreCase(r.getKey()) ||
                data.getSignKey().toLowerCase().equalsIgnoreCase(r.getKey())) {
                    continue;
                }
                maps.put(r.getKey(), buildVal(r.getKey(), r.getValue(), data, type));
            }
            */
            SortedMap<String, Object> maps = data.getParameters()
                    .entrySet()
                    .stream()
                    .filter(e -> !("appKey".equalsIgnoreCase(e.getKey())))
                    .filter(e -> !(data.getSignKey().toLowerCase().equalsIgnoreCase(e.getKey())))
                    .collect(TreeMap::new, (map, item) -> map.put(item.getKey(),
                            buildVal(item.getKey(), item.getValue(), data, type)), TreeMap::putAll);
            String sign = data.getParameters().get(data.getSignKey());
            if ("$AUTO".equalsIgnoreCase(sign)) {
                maps.put(data.getSignKey(), new SHA1WithRSASignature(new HashSet<>(
                        Arrays.asList("url", data.getSignKey().toLowerCase())))
                        .sign(maps, type));
            }
            String response = sendRequest(maps, data, template);

            assert response != null;
            logger.info("response:" + response);
            logger.info("expect: " + data.getExpect());
            if (!response.contains(data.getExpect())) {
                fail("not expect value - " + data.getExpect());
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            } else {
                throw new IllegalArgumentException(e.getLocalizedMessage(), e);
            }
        }
    }

    private String sendRequest(Map<String, Object> maps, TestData data, RestTemplate template) {
        try {
            logger.info("send request:" + mapper.writeValueAsString(maps));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpMethod httpMethod = HttpMethod.resolve(data.getHttpMethod().toUpperCase());
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        RequestEntity<?> request;
        if (httpMethod == HttpMethod.GET) {
            maps.forEach((l, v) -> {
                try {
                    if (v instanceof String) {
                        map.add(l, URLEncoder.encode((String) v, StandardCharsets.UTF_8.toString()));
                    } else {
                        map.add(l, URLEncoder.encode(String.valueOf(v), StandardCharsets.UTF_8.toString()));
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e.getLocalizedMessage(), e);
                }
            });
            request = RequestEntity.get(UriComponentsBuilder.fromHttpUrl(data.getUrl())
                            .queryParams(map)
                            .build(true).toUri())
                    .build();
        } else {
            maps.forEach((l, v) -> {
                if (v instanceof String) {
                    map.add(l, (String) v);
                } else {
                    map.add(l, String.valueOf(v));
                }
            });
            request = RequestEntity.post(UriComponentsBuilder.fromHttpUrl(data.getUrl())
                            .build().toUri())
                    .body(map);
        }
        return template.exchange(request, String.class).getBody();
    }

    private Object buildVal(String l, String val, TestData data,
                            SignatureType type) {
        try {
//            if (data.getSignKey().equalsIgnoreCase(l)) {
//                if ("$AUTO".equalsIgnoreCase(val)) {
//                    return new SHA1WithRSASignature(new HashSet<>(Arrays.asList("url", data.getSignKey().toLowerCase())))
//                            .sign(data.getParameters(), type);
//                }
//            }
            if ("$null".equalsIgnoreCase(val)) {
                return null;
            }
            return dataBuild.parseVal(val);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new IllegalArgumentException(e.getLocalizedMessage(), e);
        }
    }
}
