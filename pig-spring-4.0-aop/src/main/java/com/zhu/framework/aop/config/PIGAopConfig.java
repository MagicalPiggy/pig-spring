package com.zhu.framework.aop.config;

import lombok.Data;

@Data
public class PIGAopConfig {
    private String pointCut;
    private String aspectClass;
    private String aspectBefore;
    private String aspectAfter;
    private String aspectAfterThrow;
    private String aspectAfterThrowingName;
}
