package com.zhu.framework.servlet;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class PIGHandlerMapping {
        private Object controller;     //保存方法对应的Controller实例对象
        private Method method;          //保存映射的方法
        private Pattern pattern;        //保存URL

        public PIGHandlerMapping(Pattern pattern,Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
        }

        public Object getController() {
            return controller;
        }

        public void setController(Object controller) {
            this.controller = controller;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }
}
