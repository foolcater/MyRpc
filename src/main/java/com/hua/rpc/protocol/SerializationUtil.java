package com.hua.rpc.protocol;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializationUtil {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

    private static Objenesis objenesis = new ObjenesisStd(true);

    private SerializationUtil(){

    }

    private static <T> Schema<T> getSchema(Class<T> cls){
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null){
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null){
                cachedSchema.put(cls, schema);
            }
        }
        return schema;
    }

    /**
     * 序列化（对象 -->  字节数组）
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> byte[] serialize(T obj){
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtobufIOUtil.toByteArray(obj, schema, buffer);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组 -》对象）
     * @param data
     * @param cls
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> cls){
        try {
            T message = (T) objenesis.newInstance(cls);
            Schema<T> schema = getSchema(cls);
            ProtobufIOUtil.mergeFrom(data, message, schema);
            return message;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
