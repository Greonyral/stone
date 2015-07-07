package stone.updater;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.DESKeyGenerator;

import com.jcraft.jsch.jce.AES256CBC;

import stone.modules.Module;
import stone.util.Path;


/**
 * @author Nelphindal
 */
public class CreateKey {

	public static void main(final String[] args) {
		try {
			KeyGenerator kg;
			kg = KeyGenerator.getInstance("AES");
			kg.init(256);
			SecretKey sk = kg.generateKey();
			final byte[] key = sk.getEncoded();
			for (int i = 0; i < key.length; ++i) {
				System.out.printf("%02x", 0xff & key[i]);
			}
			System.out.printf("\n");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
