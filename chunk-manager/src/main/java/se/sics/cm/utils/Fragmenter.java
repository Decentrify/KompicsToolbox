package se.sics.cm.utils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by alidar on 10/22/14.
 */
public class Fragmenter {

    static public ArrayList<byte[]> getFragmentedByteArray(byte[] bytes, int fragmentSizeInBytes) {

        ArrayList<byte[]> fragmentList = new ArrayList<byte[]>();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        int len;
        while(true)
        {
            byte[] buf = new byte[fragmentSizeInBytes];
            len = in.read(buf, 0, buf.length);
            if(len < 0)
                break;

            if(len < fragmentSizeInBytes) { //last chunk
                buf = new byte[len];
                Arrays.copyOfRange(buf, 0, len);
            }

            fragmentList.add(buf);
        }

        return fragmentList;
    }

}
