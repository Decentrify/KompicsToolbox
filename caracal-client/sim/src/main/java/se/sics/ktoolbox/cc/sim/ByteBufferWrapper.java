package se.sics.ktoolbox.cc.sim;

import java.util.Arrays;

/**
 * Wrapper over the byte array with customized comparison
 * and equality.
 *
 * Created by babbarshaer on 2015-08-18.
 */
public class ByteBufferWrapper {

    private byte[] bytes;

    public ByteBufferWrapper(byte[] bytes){
        this.bytes = bytes;
    }

    public byte[] getBytes(){
        return this.bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ByteBufferWrapper that = (ByteBufferWrapper) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return bytes != null ? Arrays.hashCode(bytes) : 0;
    }

    @Override
    public String toString() {
        return "ByteBufferWrapper{" +
                "bytes=" + Arrays.toString(bytes) +
                '}';
    }
}
