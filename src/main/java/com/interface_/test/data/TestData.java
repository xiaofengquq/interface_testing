package com.interface_.test.data;

import lombok.Getter;
import lombok.Setter;

import java.util.SortedMap;
import java.util.TreeMap;

@Getter @Setter
public class TestData {
    private String id;
    private String url;
    private String signKey = "sign";
    private String expect;

    private String assert_;

    private String setup;

    private String tearDown;

    private String httpMethod;

    private String status = "run";

    private String description;

    private Object result;


    private SortedMap<String, String> parameters = new TreeMap<>();


}
