package io.github.wenbozhang.spark.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author wenbo.zhangw
 * @date 2023/8/8 14:35
 */
@Getter
@AllArgsConstructor
public enum TianZuoInputTypeEnum {
    /**
     * HIVE 表
     */
    HIVE(1, "HIVE"),

    ZIP(2, "ZIP");

    private final Integer code;

    private final String description;

    public static TianZuoInputTypeEnum getByCode(Integer code){
       return Arrays.stream(TianZuoInputTypeEnum.values())
               .filter(e -> e.code.equals(code))
               .findFirst()
               .orElse(null);
    }
}
