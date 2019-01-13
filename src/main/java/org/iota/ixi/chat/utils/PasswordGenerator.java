package org.iota.ixi.chat.utils;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom r = new SecureRandom();
    private static final int length = 20;

    public static String random(){
        StringBuilder sb = new StringBuilder(length);
        for( int i = 0; i < length; i++ )
            sb.append( alphabet.charAt( r.nextInt(alphabet.length()) ) );
        return sb.toString();
    }

}
