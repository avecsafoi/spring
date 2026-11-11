package avecsafoi.ktx.mts.util;

import java.lang.reflect.InvocationTargetException;

public class ClassU {

    public static <T> T createObject(Class<T> c, Class<T> t, Object a) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return c.getConstructor(t).newInstance(a);
    }

}
