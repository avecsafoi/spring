package avecsafoi.ktx.mts.io.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class TDataInputStream extends DataInputStream {

    public TDataInputStream(InputStream in) {
        super(in);
    }

    public abstract <T> T readObject(Class<T> c) throws IOException;

}
