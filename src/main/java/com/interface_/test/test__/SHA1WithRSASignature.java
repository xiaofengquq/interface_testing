package com.interface_.test.test__;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interface_.test.util.Config;
import com.interface_.test.util.SignatureType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
@RequiredArgsConstructor
public class SHA1WithRSASignature {
    private final Logger logger = LoggerFactory.getLogger(SHA1WithRSASignature.class);
    private final ObjectMapper MAPPER = new ObjectMapper();


    private final Set<String> excludes;

    public String sign(SortedMap<String, Object> maps, SignatureType type) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, SignatureException {
        String signContent = maps.entrySet()
                .stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> !(excludes.contains(e.getKey().toLowerCase())))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        logger.info("signContent: " + signContent);
        return SHA1WithRSA(signContent, type == SignatureType.SERVER ?
                Optional.ofNullable(maps.get("appKey"))
                        .map(String.class::cast).orElse(Config.SERVER) :
                Config.CLIENT);
    }

    public String SHA1WithRSA(String content, String appKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(getPrivateKey(appKey));
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private PrivateKey getPrivateKey(String key) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static String SHA1WithRSA1(String content, String appKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(getPrivateKey1(appKey));
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static PrivateKey getPrivateKey1(String key) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String signContent = "appExt=channelExt&appId=1000&cid=a1_c_1&countryCode=1&deviceId=454544354&deviceType=1&timestamp=1703151557041&vercode=1";
        System.out.println(SHA1WithRSASignature.SHA1WithRSA1(signContent, Config.SERVER));
    }
}
