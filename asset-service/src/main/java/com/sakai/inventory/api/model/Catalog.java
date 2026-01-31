package com.sakai.inventory.api.model;

//import lombok.Getter;
//import lombok.Setter;
//import lombok.ToString;

import java.io.Serializable;

//@Getter
//@Setter
//@ToString
public class Catalog implements Serializable {
    private String id;
    private String name;
    private String description;
    private String color;
    private Long productCount;
    private String createdAt;
}
