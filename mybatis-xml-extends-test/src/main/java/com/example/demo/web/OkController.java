package com.example.demo.web;

import com.example.demo.db.mapper.audit.AuditDOMapper;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ：张文博
 * @date ：Created in 2020/8/20 9:44 上午
 * @description：
 * @modified By：
 * @version:
 */
@RestController
public class OkController {

    @Resource
    private AuditDOMapper auditDOMapper;

    @RequestMapping(value = "/okA", method = RequestMethod.POST)
    public Map<String, String> okA(String age, String busiResponseCode) {
        System.out.println("okA is invoke");
        Map<String, String> map = new HashMap<>();
        map.put("busiResponseCode", busiResponseCode);
        map.put("age", age);
        return map;
    }

    @GetMapping("okB")
    public Map<String, String> okB(String age, String busiResponseCode) {
        System.out.println("okB is invoke");
        Map<String, String> map = new HashMap<>();
        map.put("busiResponseCode", busiResponseCode);
        map.put("age", age);
        return map;
    }

    @GetMapping("db")
    public Object db(){
//        AuditQuery auditQuery = new AuditQuery();
//        return auditDOMapper.selectForDecisionTool(auditQuery);
        return auditDOMapper.selectAll();
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                System.out.println("text = " + text);
                if ("".equals(text)) {
                    setValue(null);
                } else {
                    setValue(text);
                }
            }
        });
    }

    @GetMapping("person")
    public Person person() {
        Person p = new Children("1");
        p.setAge(10);
        p.setName("张三");
        return p;
    }

    public static class Person {
        private String name;

        private Integer age;

        private Date date = new Date();

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public Person() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }


    public static class Children extends Person {
        private String school;

        public Children(String name, Integer age, String school) {
            super(name, age);
            this.school = school;
        }

        public Children(String school) {
            this.school = school;
        }

        public String getSchool() {
            return school;
        }

        public void setSchool(String school) {
            this.school = school;
        }
    }
}
