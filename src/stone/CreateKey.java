package stone;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


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
