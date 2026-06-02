package com.th.ipqcmbiz.config.serializer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.th.ipqcmbiz.entity.enums.ToolRecognitionTypeEnum;

import java.lang.reflect.Type;

/**
 * 工具编号列表序列化器
 * 将 "wrench,pliers" 序列化为 "扳手,钳子"
 */
public class ToolCodeListSerializer implements ObjectWriter {

    public static final ToolCodeListSerializer INSTANCE = new ToolCodeListSerializer();

    @Override
    public void write(JSONWriter writer, Object value, Object fieldName, Type fieldType, long features) {
        if (value == null) {
            writer.writeNull();
            return;
        }
        String translated = ToolRecognitionTypeEnum.translateToolCodeList((String) value);
        writer.writeString(translated);
    }
}