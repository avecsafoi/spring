package avecsafoi.ktx.mts.io.data.ksc;

import avecsafoi.ktx.mts.io.data.TA;
import avecsafoi.ktx.mts.io.data.TData;
import avecsafoi.ktx.mts.io.data.TDataInputStream;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KscTextDataInputStream extends TDataInputStream {

    public static final int RW_SIZE = 5;

    protected final Charset charset;

    public KscTextDataInputStream(InputStream in, Charset charset) {
        super(in);
        this.charset = charset;
    }

    public String readString(int n) throws IOException {
        byte[] b = new byte[n];
        readFully(b);
        return new String(b, charset);
    }

    public int readSize() throws IOException {
        return Integer.parseInt(readString(RW_SIZE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> c) throws IOException {
        return (T) readObject(c, null, null);
    }

    public Object readObject(Class<?> c, Field f, TA a) throws IOException {
        if (TData.class.isAssignableFrom(c)) {
            skip(RW_SIZE);
            Object o = createObject(c, null, null);
            try {
                readObjectFields(c, o);
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            return o;
        }
        if (f == null) throw new IOException("No reference of field found for " + c.getName());
        if (a == null) throw new IOException("No annotation TA found");
        if (c.isPrimitive()) {
            String s = readString(size(a.z()) + 1).trim();
            if (int.class.isAssignableFrom(c)) return Integer.parseInt(s);
            if (long.class.isAssignableFrom(c)) return Long.parseLong(s);
            if (double.class.isAssignableFrom(c)) return Double.parseDouble(s);
            if (float.class.isAssignableFrom(c)) return Float.parseFloat(s);
            throw new IOException("Unsupported primitive type: " + c.getName());
        }
        if (Number.class.isAssignableFrom(c)) {
            String s = readString(size(a.z()) + 1).trim();
            if (BigDecimal.class.isAssignableFrom(c)) return new BigDecimal(s);
            if (BigInteger.class.isAssignableFrom(c)) return new BigInteger(s);
            if (Integer.class.isAssignableFrom(c)) return Integer.parseInt(s);
            if (Long.class.isAssignableFrom(c)) return Long.parseLong(s);
            if (Double.class.isAssignableFrom(c)) return Double.parseDouble(s);
            if (Float.class.isAssignableFrom(c)) return Float.parseFloat(s);
            throw new IOException("Unsupported Number type: " + c.getName());
        }
        if (String.class.isAssignableFrom(c)) {
            return readString(size(a.z())).trim();
        }
        if (List.class.isAssignableFrom(c)) {
            Class<?> s = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
            int z = readSize();
            List<Object> l = new ArrayList<>(z);
            for (int i = 0; i < z; i++) l.add(readObject(s, f, a));
            return l;
        }
        if (c.isArray()) {
            int z = readSize();
            Class<?> s = c.getComponentType();
            Object o = Array.newInstance(c.getComponentType(), z);
            for (int i = 0; i < z; i++) Array.set(a, i, readObject(s, f, a));
            return o;
        }
        if (Temporal.class.isAssignableFrom(c)) {
            String s = readString(size(a.z())).trim();
            if (s.isEmpty()) return null;
            String t = a.f();
            if (t == null || t.isBlank()) throw new IOException("No annotation value of TA.f() found");
            DateTimeFormatter df = DateTimeFormatter.ofPattern(t);
            if (LocalDate.class.isAssignableFrom(c)) return LocalDate.parse(s, df);
            if (LocalDateTime.class.isAssignableFrom(c)) return LocalDateTime.parse(s, df);
            if (LocalTime.class.isAssignableFrom(c)) return LocalTime.parse(s, df);
            throw new IOException("Unsupported temporal type: " + c.getName());
        }
        throw new IOException("Unsupported type: " + c.getName());
    }

    public void readObjectFields(Class<?> c, Object o) {
        Class<?> s = c.getSuperclass();
        if (s != null) readObjectFields(s, o);
        final Object r = o;
        ReflectionUtils.doWithFields(c, f -> {
            try {
                if (Modifier.isStatic(f.getModifiers())) return;
                TA a = f.getAnnotation(TA.class);
                if (a == null) return;
                f.setAccessible(true);
                f.set(r, readObject(f.getType(), f, a));
            } catch (IllegalAccessException | IOException e) {
                throw new RuntimeException("%s on field %s.%s".formatted(e.getMessage(), f.getDeclaringClass().getName(), f.getName()));
            }
        });
    }

    public void skip(int n) throws IOException {
        int i = super.skipBytes(n);
        if (i != n) throw new IOException("Could not skip " + (n - i) + " of " + n + " bytes");
    }

    public int size(int[] z) {
        return z[0] + z[1] + (z[0] > 0 ? 1 : 0);
    }

    public Object createObject(Class<?> c, Class<?>[] t, Object[] a) throws IOException {
        try {
            return c.getConstructor(t).newInstance(a);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new IOException("Could not create object of type " + c.getName() + " with arguments " + Arrays.toString(a));
        }
    }

}
