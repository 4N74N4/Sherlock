package com.bgu.congeor.commons;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by clint on 1/5/14.
 */
public class SerializationDeserializationTest<T extends KryoSerializable> {

    protected Class<T> testClass;
    protected HashSet<String> ignoredFields;
    Random r;

    public SerializationDeserializationTest (){
        r = new Random();
        ignoredFields = new HashSet<String>();
    }

    @Test
    public void testSerializationDeSerialization() throws Exception {
        String className = "";
        T instance = testClass.newInstance();
        fillClass( instance );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Kryo kryo = new Kryo();
        Output output = new Output ( baos );
        kryo.writeObject(output, instance);
        output.flush();
        output.close();
        byte [] serialized = baos.toByteArray();
        Input input = new Input ( serialized );
        T generatedInstance = kryo.readObject(input, testClass);
        compareClasses(instance, generatedInstance);
    }

    private void compareClasses ( T instance1, T instance2 ) throws IllegalAccessException {
        Field[] fields = instance1.getClass().getDeclaredFields();
        for ( Field field:fields ){
            if ( !ignoredFields.contains(field.getName())){
                field.setAccessible(true);
                if ( field.getType().isArray())
                    Assert.assertEquals(field.getName(), Array.getLength(field.get(instance1)), Array.getLength(field.get(instance2)));
                else {
                    Assert.assertNotNull("Field " + field.getName() + " was not initialized", field.get(instance2));
                    Assert.assertEquals(field.getName(), field.get(instance1).toString(), field.get(instance2).toString());
                }
            }
        }
    }

    private void fillClass ( T instance ) throws IllegalAccessException, InstantiationException {
        Field[] fields = instance.getClass().getDeclaredFields();
        for ( Field field:fields )
            if ( !ignoredFields.contains(field.getName())){
                Object value = getValue(field.getType());
                field.setAccessible(true);
                field.set(instance, value);
            }
    }

    private Object getValue ( Class<?> fieldType ) throws IllegalAccessException, InstantiationException {
        Object value = null;
        if ( fieldType == Double.class || fieldType == long.class || fieldType == int.class || fieldType == Float.class || fieldType == byte.class){
            value = r.nextInt();
        }
        if ( fieldType == char.class ){
            value = 'C' + r.nextInt();
        }
        if ( fieldType == String.class ){
            value = "test" + r.nextInt();
        }
        if ( fieldType.isArray()){
            Object toFill = Array.newInstance(fieldType.getComponentType(), 5 + r.nextInt(10));
            value = toFill;
        }
        if ( value == null )
            throw new UnsupportedOperationException();
        return value;
    }
}
