package com.tunan.flow.dto;

import lombok.Data;

@Data
public class ApiParam {

    private String name;
    private String type;
    private String description;
    private Boolean required;
    private String example;
}
