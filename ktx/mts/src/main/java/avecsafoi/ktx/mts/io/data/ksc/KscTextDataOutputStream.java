package avecsafoi.ktx.mts.io.data.ksc;

import avecsafoi.ktx.mts.io.data.TA;
import avecsafoi.ktx.mts.io.data.TData;
import avecsafoi.ktx.mts.io.data.TDataOutputStream;
import avecsafoi.ktx.mts.util.StringU;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;

public class KscTextDataOutputStream extends TDataOutputStream {

    public static final int RW_SIZE = 5;

    protected final Charset charset;

    public KscTextDataOutputStream(OutputStream out, Charset charset) {
        super(out);
        this.charset = charset;
    }

    public void writeString(String s, int n) throws IOException {
        byte[] b = s == null ? null : s.getBytes(charset);
        int z = b == null ? 0 : b.length;
        if (z > 0) write(b);
        while (z++ < n) write(' ');
    }

    public void writeNumber(BigDecimal v, int[] z) throws IOException {
        String s = v.setScale(z[1], RoundingMode.HALF_EVEN).abs().toPlainString();
        int n = size(z);
        s = (v.signum() < 0 ? "-" : "+") + StringU.leftPad(s, n, '0');
        writeString(s, n + 1);
    }

    public void writeSize(int n) {
        StringU.leftPad(String.valueOf(n), RW_SIZE, '0');
    }

    @Override
    public void writeObject(Object o) throws IOException {
        writeObject(o.getClass(), o, null, null);
    }

    public void writeObject(Class<?> c, Object o, Field f, TA a) throws IOException {
        if (TData.class.isAssignableFrom(c)) {
            writeSize(1);
            writeObjectFields(c, o);
            return;
        }
        if (f == null) throw new IllegalArgumentException("No reference of field found for " + c.getName());
        if (a == null) throw new IllegalArgumentException("No TA annotation found");
        if (c.isPrimitive() || Number.class.isAssignableFrom(c)) {
            java.math.BigDecimal v = new BigDecimal(o.toString());
            writeNumber(v, a.z());
            return;
        }
        if (String.class.isAssignableFrom(c)) {
            writeString((String) o, size(a.z()));
            return;
        }
        if (List.class.isAssignableFrom(c)) {
            Class<?> s = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
            List<?> l = o == null ? null : (List<?>) o;
            int z = l == null ? 0 : l.size();
            for (int i = 0; i < z; i++) writeObject(s, l.get(i), f, a);
            return;
        }
        if (c.isArray()) {
            int z = o == null ? 0 : Array.getLength(o);
            Class<?> s = c.getComponentType();
            for (int i = 0; i < z; i++) writeObject(s, Array.get(o, i), f, a);
            return;
        }
        if (Temporal.class.isAssignableFrom(c)) {
            if (o == null) return;
            String t = a.f();
            if (t == null || t.isBlank()) throw new IllegalArgumentException("No annotation value of TA.f() found");
            DateTimeFormatter df = DateTimeFormatter.ofPattern(t);
            if (LocalDate.class.isAssignableFrom(c)) {
                writeString(((LocalDate) o).format(df), size(a.z()));
                return;
            }
            if (LocalDateTime.class.isAssignableFrom(c)) {
                writeString(((LocalDateTime) o).format(df), size(a.z()));
                return;
            }
            if (LocalTime.class.isAssignableFrom(c)) {
                writeString(((LocalTime) o).format(df), size(a.z()));
                return;
            }
            throw new IllegalArgumentException("Unsupported temporal type: " + c.getName());
        }
        throw new IllegalArgumentException("Unsupported type: " + c.getName());
    }

    public void writeObjectFields(Class<?> c, Object o) {
        Class<?> s = c.getSuperclass();
        if (s != null) writeObjectFields(s, o);
        final Object r = o;
        ReflectionUtils.doWithFields(c, f -> {
            try {
                if (Modifier.isStatic(f.getModifiers())) return;
                TA a = f.getAnnotation(TA.class);
                if (a == null) return;
                f.setAccessible(true);
                writeObject(f.getType(), f.get(r), f, a);
            } catch (IllegalAccessException | IOException e) {
                throw new RuntimeException("%s on field %s.%s".formatted(e.getMessage(), f.getDeclaringClass().getName(), f.getName()));
            }
        });
    }

    public int size(int[] z) {
        return z[0] + z[1] + (z[0] > 0 ? 1 : 0);
    }

}
