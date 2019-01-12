package org.iota.ixi.chat.utils.aes_key_length;

import javax.crypto.Cipher;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class AESKeyLengthFix {

    // https://github.com/Delthas/JavaSkype/commit/c329dc06c28b0b2a6f2b6c05cc08bad1e0cdf9c8

    static {

        // ugly piece of code to bypass Oracle JRE stupid restriction on key lengths
        // Skype requires a 256-bit key AES cipher, but Oracle will only allow a key length <= 128-bit due to US export laws

        // the normal ways to fix this are:
        // a) to stop using Oracle JRE
        // b) to replace two files in the Oracle JRE folder (see http://stackoverflow.com/a/3864276)
        // c) to use a simple 128-bit key instead of a 256-bit one
        // d) to use an external Cipher implementation (like BouncyCastle)

        // however, none of these ways are practical, or lightweight enough
        // so we have to manually override the permissions on key lengths using reflection

        // ugly reflection hack start (we have to override a private static final field from a package-private class...)

        String errorString = "Failed manually overriding key-length permissions. "
                + "Please open an issue at https://github.com/Delthas/JavaSkype/issues/ if you see this message. "
                + "Try doing this to fix the problem: http://stackoverflow.com/a/3864276";

        int newMaxKeyLength;
        try {
            if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
                Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                Constructor con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissionCollection = con.newInstance();
                Field f = c.getDeclaredField("all_allowed");
                f.setAccessible(true);
                f.setBoolean(allPermissionCollection, true);

                c = Class.forName("javax.crypto.CryptoPermissions");
                con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissions = con.newInstance();
                f = c.getDeclaredField("perms");
                f.setAccessible(true);
                ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

                c = Class.forName("javax.crypto.JceSecurityManager");
                f = c.getDeclaredField("defaultPolicy");
                f.setAccessible(true);
                Field mf = Field.class.getDeclaredField("modifiers");
                mf.setAccessible(true);
                mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                // override a final field
                // this field won't be optimized out by the compiler because it is set at run-time
                f.set(null, allPermissions);

                newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            }
        } catch (Exception e) {
            throw new RuntimeException(errorString, e);
        }
        if (newMaxKeyLength < 256) {
            // hack failed
            throw new RuntimeException(errorString);
        }

        // ugly reflection hack end

    }

}
