package avecsafoi.ktx.mts.io.data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class TDataOutputStream extends DataOutputStream {

    public TDataOutputStream(OutputStream out) {
        super(out);
    }

    public abstract void writeObject(Object o) throws IOException;
}
