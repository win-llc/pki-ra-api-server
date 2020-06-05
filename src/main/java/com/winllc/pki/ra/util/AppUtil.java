package com.winllc.pki.ra.util;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AppUtil {

    public static String generate20BitString(){
        return getAlphaNumericString(20);
    }

    public static String generate256BitString(){
        //todo
        return getAlphaNumericString(256);
    }

    static String getAlphaNumericString(int n)
    {

        // length is bounded by 256 Character
        byte[] array = new byte[256];
        new Random().nextBytes(array);

        String randomString
                = new String(array, Charset.forName("UTF-8"));

        // Create a StringBuffer to store the result
        StringBuffer r = new StringBuffer();

        // Append first 20 alphanumeric characters
        // from the generated random String into the result
        for (int k = 0; k < randomString.length(); k++) {

            char ch = randomString.charAt(k);

            if (((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9'))
                    && (n > 0)) {

                r.append(ch);
                n--;
            }
        }

        // return the resultant string
        return r.toString();
    }

    private static String generateRandomString(int length){
        try {
            Random random = ThreadLocalRandom.current();
            byte[] r = new byte[length]; //Means 2048 bit
            random.nextBytes(r);
            String s = new String(r);
            return s;
        }catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }
}
