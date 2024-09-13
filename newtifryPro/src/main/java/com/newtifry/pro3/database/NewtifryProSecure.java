package com.newtifry.pro3.database;

//import java.security.SecureRandom;
//
//import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;
//
////Simple helper class to encrypt and decrypt strings using AES128. The result is Ascii-encoded (actually hex, no base64), 
////so no byte[] has to be stored. A SEED value is used as a shared secret ("Master-Password"). 
////Only with the same SEED the stored values can be decrypted.
//
///**
// * Usage:
// * <pre>
// * String crypto = Secure.encrypt(masterpassword, cleartext)
// * ...
// * String cleartext = Secure.decrypt(masterpassword, crypto)
// * &lt;/pre&gt;
// * @author ferenc.hechler
// */
//
//public class Secure {
//
//	public static String encrypt(String seed, String cleartext) throws Exception {
//		byte[] rawKey = getRawKey(seed.getBytes());
//		byte[] result = encrypt(rawKey, cleartext.getBytes());
//		return toHex(result);
//	}
//	
//	public static String decrypt(String seed, String encrypted) throws Exception {
//		byte[] rawKey = getRawKey(seed.getBytes());
//		byte[] enc = toByte(encrypted);
//		byte[] result = decrypt(rawKey, enc);
//		return new String(result);
//	}
//
//	private static byte[] getRawKey(byte[] seed) throws Exception {
//		KeyGenerator kgen = KeyGenerator.getInstance("AES");
//		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
//		sr.setSeed(seed);
//	    kgen.init(128, sr); // 192 and 256 bits may not be available
//	    SecretKey skey = kgen.generateKey();
//	    byte[] raw = skey.getEncoded();
//	    return raw;
//	}
//
//	
//	private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
//	    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
//		Cipher cipher = Cipher.getInstance("AES");
//	    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
//	    byte[] encrypted = cipher.doFinal(clear);
//		return encrypted;
//	}
//
//	private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
//	    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
//		Cipher cipher = Cipher.getInstance("AES");
//	    cipher.init(Cipher.DECRYPT_MODE, skeySpec);
//	    byte[] decrypted = cipher.doFinal(encrypted);
//		return decrypted;
//	}
//
//	public static String toHex(String txt) {
//		return toHex(txt.getBytes());
//	}
//	public static String fromHex(String hex) {
//		return new String(toByte(hex));
//	}
//	
//	public static byte[] toByte(String hexString) {
//		int len = hexString.length()/2;
//		byte[] result = new byte[len];
//		for (int i = 0; i < len; i++)
//			result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
//		return result;
//	}
//
//	public static String toHex(byte[] buf) {
//		if (buf == null)
//			return "";
//		StringBuffer result = new StringBuffer(2*buf.length);
//		for (int i = 0; i < buf.length; i++) {
//			appendHex(result, buf[i]);
//		}
//		return result.toString();
//	}
//	private final static String HEX = "0123456789ABCDEF";
//	private static void appendHex(StringBuffer sb, byte b) {
//		sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
//	}
//	
//}


import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//String res = new String( this.NewtifryProSecure.decrypt( text ), "UTF-8" );
//res = URLDecoder.decode(res,"UTF-8");


public class NewtifryProSecure {

//    private String iv              = "myuniqueivparam";
//    private String secretkey       = "mysecretkey";
    private static IvParameterSpec ivspec = null;
    private static SecretKeySpec keyspec = null;
    private static Cipher cipher = null;

//    public NewtifryProSecure()
//    {
//        ivspec = new IvParameterSpec(iv.getBytes());
//        keyspec = new SecretKeySpec(secretkey.getBytes(), "AES");
//
//        try {
//            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//        }
//    }

    public static byte[] decrypt(String code, String secretkey, String iv) throws Exception {
    	if (ivspec == null) {
    		ivspec = new IvParameterSpec(iv.getBytes());
            keyspec = new SecretKeySpec(secretkey.getBytes(), "AES");
            try {
            	cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            } catch (NoSuchAlgorithmException e) {
            	e.printStackTrace();
            } catch (NoSuchPaddingException e) {
            	e.printStackTrace();
            }
    	}
    	
        if(code == null || code.length() == 0) {
            throw new Exception("Empty string");
        }
        byte[] decrypted = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            decrypted = cipher.doFinal(hexToBytes(code));
        }
        catch (Exception e) {
            throw new Exception("[decrypt] " + e.getMessage());
        }
        return decrypted;
    }

    
//    public byte[] encrypt(String text) throws Exception
//    {
//        if(text == null || text.length() == 0) {
//            throw new Exception("Empty string");
//        }
//        byte[] encrypted = null;
//        try {
//            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
//            encrypted = cipher.doFinal(text.getBytes("UTF-8"));
//        }
//        catch (Exception e) {
//            throw new Exception("[encrypt] " + e.getMessage());
//        }
//        return encrypted;
//    }


    public static String bytesToHex(byte[] data)
    {
        if (data==null) {
            return null;
        }
        int len = data.length;
        String str = "";
        for (int i=0; i<len; i++) {
            if ((data[i]&0xFF)<16) {
                str = str + "0" + java.lang.Integer.toHexString(data[i]&0xFF);
            }
            else {
                str = str + java.lang.Integer.toHexString(data[i]&0xFF);
            }
        }
        return str;
    }

    public static byte[] hexToBytes(String str) {
        if (str==null) {
            return null;
        }
        else if (str.length() < 2) {
            return null;
        }
        else {
            int len = str.length() / 2;
            byte[] buffer = new byte[len];
            for (int i=0; i<len; i++) {
                buffer[i] = (byte) Integer.parseInt(str.substring(i*2,i*2+2),16);
            }
            return buffer;
        }
    }

}
