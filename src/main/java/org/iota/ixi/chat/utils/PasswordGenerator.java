package org.iota.ixi.chat.utils;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom r = new SecureRandom();

    public static String randomString(int len){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( alphabet.charAt( r.nextInt(alphabet.length()) ) );
        return sb.toString();
    }

}
