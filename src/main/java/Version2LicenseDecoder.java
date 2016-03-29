//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.atlassian.extras.common.LicenseException;
import com.atlassian.extras.common.org.springframework.util.DefaultPropertiesPersister;
import com.atlassian.extras.decoder.api.AbstractLicenseDecoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.commons.codec.binary.Base64;

public class Version2LicenseDecoder extends AbstractLicenseDecoder {
	public static final int VERSION_NUMBER_1 = 1;
	public static final int VERSION_NUMBER_2 = 2;
	public static final int VERSION_LENGTH = 3;
	public static final int ENCODED_LICENSE_LENGTH_BASE = 31;
	public static final byte[] LICENSE_PREFIX = new byte[] { (byte) 13, (byte) 14, (byte) 12, (byte) 10, (byte) 15 };
	public static final char SEPARATOR = 'X';
	private static final PublicKey PUBLIC_KEY;
	private static final int ENCODED_LICENSE_LINE_LENGTH = 76;

	public Version2LicenseDecoder() {
	}

	public boolean canDecode2(String licenseString) {
		licenseString = removeWhiteSpaces(licenseString);
		int pos = licenseString.lastIndexOf(88);
		if (pos != -1 && pos + 3 < licenseString.length()) {
			try {
				int version = Integer.parseInt(licenseString.substring(pos + 1, pos + 3));
				if (version != 1 && version != 2) {
					return false;
				} else {
					String lengthStr = licenseString.substring(pos + 3);
					int encodedLicenseLength = Integer.valueOf(lengthStr, 31).intValue();
					return pos == encodedLicenseLength;
				}
			} catch (NumberFormatException var6) {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean canDecode(String licenseString) {
		return true;
	}

	public Properties doDecode(String licenseString) {
		Properties result = null;
		String encodedLicenseTextAndHash = null;
		if (this.canDecode2(licenseString)) {
			encodedLicenseTextAndHash = this.getLicenseContent(removeWhiteSpaces(licenseString));
			byte[] proStrs = this.checkAndGetLicenseText(encodedLicenseTextAndHash);
			Reader property = this.unzipText(proStrs);
			result = this.loadLicenseConfiguration(property);
		} else {
			encodedLicenseTextAndHash = removeWhiteSpaces(licenseString);
			result = new Properties();
			if (encodedLicenseTextAndHash != null && encodedLicenseTextAndHash.length() > 0) {
				String[] var10 = encodedLicenseTextAndHash.split(",");
				if (var10 != null && var10.length > 0) {
					String[] var8 = var10;
					int var7 = var10.length;

					for (int var6 = 0; var6 < var7; ++var6) {
						String var11 = var8[var6];
						String[] proStr = var11.split("=");
						result.put(proStr[0], proStr[1]);
					}
				}
			}
		}

		return result;
	}

	protected int getLicenseVersion() {
		return 2;
	}

	private Reader unzipText(byte[] licenseText) {
		ByteArrayInputStream in = new ByteArrayInputStream(licenseText);
		in.skip((long) LICENSE_PREFIX.length);
		InflaterInputStream zipIn = new InflaterInputStream(in, new Inflater());

		try {
			return new InputStreamReader(zipIn, "UTF-8");
		} catch (UnsupportedEncodingException var5) {
			throw new LicenseException(var5);
		}
	}

	private String getLicenseContent(String licenseString) {
		String lengthStr = licenseString.substring(licenseString.lastIndexOf(88) + 3);

		try {
			int e = Integer.valueOf(lengthStr, 31).intValue();
			return licenseString.substring(0, e);
		} catch (NumberFormatException var4) {
			throw new LicenseException("Could NOT decode license length <" + lengthStr + ">", var4);
		}
	}

	private byte[] checkAndGetLicenseText(String licenseContent) {
		try {
			byte[] e = Base64.decodeBase64(licenseContent.getBytes());
			ByteArrayInputStream in = new ByteArrayInputStream(e);
			DataInputStream dIn = new DataInputStream(in);
			int textLength = dIn.readInt();
			byte[] licenseText = new byte[textLength];
			dIn.read(licenseText);
			byte[] hash = new byte[dIn.available()];
			dIn.read(hash);

			try {
				Signature e1 = Signature.getInstance("SHA1withDSA");
				e1.initVerify(PUBLIC_KEY);
				e1.update(licenseText);
				if (!e1.verify(hash)) {
					throw new LicenseException("Failed to verify the license.");
				} else {
					return licenseText;
				}
			} catch (InvalidKeyException var9) {
				throw new LicenseException(var9);
			} catch (SignatureException var10) {
				throw new LicenseException(var10);
			} catch (NoSuchAlgorithmException var11) {
				throw new LicenseException(var11);
			}
		} catch (IOException var12) {
			throw new LicenseException(var12);
		}
	}

	private Properties loadLicenseConfiguration(Reader text) {
		try {
			Properties e = new Properties();
			(new DefaultPropertiesPersister()).load(e, text);
			return e;
		} catch (IOException var3) {
			throw new LicenseException("Could NOT load properties from reader", var3);
		}
	}

	private static String removeWhiteSpaces(String licenseData) {
		if (licenseData != null && licenseData.length() != 0) {
			char[] chars = licenseData.toCharArray();
			StringBuffer buf = new StringBuffer(chars.length);

			for (int i = 0; i < chars.length; ++i) {
				if (!Character.isWhitespace(chars[i])) {
					buf.append(chars[i]);
				}
			}

			return buf.toString();
		} else {
			return licenseData;
		}
	}

	public static String packLicense(byte[] text, byte[] hash) throws LicenseException {
		try {
			ByteArrayOutputStream e = new ByteArrayOutputStream();
			DataOutputStream dOut = new DataOutputStream(e);
			dOut.writeInt(text.length);
			dOut.write(text);
			dOut.write(hash);
			byte[] allData = e.toByteArray();
			String result = (new String(Base64.encodeBase64(allData))).trim();
			result = result + 'X' + "0" + 2 + Integer.toString(result.length(), 31);
			result = split(result);
			return result;
		} catch (IOException var6) {
			throw new LicenseException(var6);
		}
	}

	public static String packLicense2(byte[] text, byte[] hash) throws LicenseException {
		try {
			ByteArrayOutputStream e = new ByteArrayOutputStream();
			DataOutputStream dOut = new DataOutputStream(e);
			dOut.writeInt(text.length);
			dOut.write(text);
			dOut.write(hash);
			byte[] allData = e.toByteArray();
			String result = (new String(Base64.encodeBase64(allData))).trim();
			result = result + 'X' + "0" + 2 + Integer.toString(result.length(), 31);
			result = split(result);
			return result;
		} catch (IOException var6) {
			throw new LicenseException(var6);
		}
	}

	private static String split(String licenseData) {
		if (licenseData != null && licenseData.length() != 0) {
			char[] chars = licenseData.toCharArray();
			StringBuffer buf = new StringBuffer(chars.length + chars.length / 76);

			for (int i = 0; i < chars.length; ++i) {
				buf.append(chars[i]);
				if (i > 0 && i % 76 == 0) {
					buf.append('\n');
				}
			}

			return buf.toString();
		} else {
			return licenseData;
		}
	}

	static {
		try {
			String e = "MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAIvfweZvmGo5otwawI3no7Udanxal3hX2haw962KL/nHQrnC4FG2PvUFf34OecSK1KtHDPQoSQ+DHrfdf6vKUJphw0Kn3gXm4LS8VK/LrY7on/wh2iUobS2XlhuIqEc5mLAUu9Hd+1qxsQkQ50d0lzKrnDqPsM0WA9htkdJJw2nS";
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			PUBLIC_KEY = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(
					"MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAIvfweZvmGo5otwawI3no7Udanxal3hX2haw962KL/nHQrnC4FG2PvUFf34OecSK1KtHDPQoSQ+DHrfdf6vKUJphw0Kn3gXm4LS8VK/LrY7on/wh2iUobS2XlhuIqEc5mLAUu9Hd+1qxsQkQ50d0lzKrnDqPsM0WA9htkdJJw2nS"
							.getBytes())));
		} catch (NoSuchAlgorithmException var2) {
			throw new Error(var2);
		} catch (InvalidKeySpecException var3) {
			throw new Error(var3);
		}
	}

	public static void main(String[] args) throws IOException, Exception {
		new Version2LicenseDecoder();
		String lll = "AAABDA0ODAoPeNptUEtPg0AQvu+vIPG8ZsEKlmQPFda6DVAENB68rHTUbdotmQVi/71QTHykh5lM5nvMl7m4Q+2sOuOwwGHz8PomnDFnmVaOx9wrEoOtUTetPhi+ksXiJXREr3adGjckQjgNsWqBj3zKfMoCstWoLhNdg7EgNvqkFlkliryQpSA/DrzFDv7Qq2MDmdoDj9ZpKopILpIJV3Wre5gEu4n7BGhHE4+kSpsWjDI1iM9G4/FXomBMtMZ3ZbSdjm4PWpnN0M1knXX7V8D126MdDDl1SQnYA8qY31b5A5WRLKgfP0d0du8uSSkyPhRNPJ/5njcn38kHeiLjc8j5SHmH9Yey8P95XxVof60wKwITfDIxHZPgo323OEKd2FJ4BXvU7wIUIbLvXQNrkIAf4AL2Aeu4ZBRbTOA=X02dl";
		String lls = "AAABckRlc2NyaXB0aW9uPUpJUkE6IENvbW1lcmNpYWwKQ3JlYXRpb25EYXRlPTIwMTMtMDYtMDcKamlyYS5MaWNlbnNlRWRpdGlvbj1FTlRFUlBSSVNFCkV2YWx1YXRpb249ZmFsc2UKamlyYS5MaWNlbnNlVHlwZU5hbWU9Q09NTUVSQ0lBTApqaXJhLmFjdGl2ZT10cnVlCmxpY2Vuc2VWZXJzaW9uPTIKTWFpbnRlbmFuY2VFeHBpcnlEYXRlPTIwOTktMDYtMDcKT3JnYW5pc2F0aW9uPWpvaWFuZGpvaW4KU0VOPVNFTi1MMjYwNjIyOQpTZXJ2ZXJJRD1CVFBRLUlDSVItNkRYQy00SDFHCmppcmEuTnVtYmVyT2ZVc2Vycz0tMQpMaWNlbnNlSUQ9TElEU0VOLUwyNjA2MjI5CkxpY2Vuc2VFeHBpcnlEYXRlPTIwOTktMDYtMDcKUHVyY2hhc2VEYXRlPTIwMTMtMDYtMDc=X02g4";
//		String sll = "Description=JIRA:Commercial\nCreationDate=2013-06-07\njira.LicenseEdition=ENTERPRISE\nEvaluation=false\njira.LicenseTypeName=COMMERCIAL\njira.active=true\nlicenseVersion=2\nMaintenanceExpiryDate=2099-06-07\nOrganisation=joiandjoin\nSEN=SEN-L2606229\nServerID=BTPQ-ICIR-6DXC-4H1G\njira.NumberOfUsers=-1\nLicenseID=LIDSEN-L2606229\nLicenseExpiryDate=2099-06-07\nPurchaseDate=2013-06-07";
//		String sll = "com.allenta.jira.plugins.gitlab.gitlab-listener.enterprise=true\nDescription=GitLab Listener\\: Evaluation\nNumberOfUsers=-1\nCreationDate=2015-12-29\nContactEMail=xwturing@gmail.com\nEvaluation=true\ncom.allenta.jira.plugins.gitlab.gitlab-listener.Starter=false\nlicenseVersion=2\nMaintenanceExpiryDate=2099-01-27\nOrganisation=Evaluation license\nSEN=SEN-L7030895\ncom.allenta.jira.plugins.gitlab.gitlab-listener.active=true\nLicenseExpiryDate=2099-01-27\nLicenseTypeName=COMMERCIAL\nPurchaseDate=2015-12-29\n";
//		String sll = "jira.product.jira-servicedesk.active=true\njira.product.jira-servicedesk.Starter=false\nNumberOfUsers=-1\nPurchaseDate=2016-01-05\ncom.atlassian.servicedesk.active=true\nLicenseTypeName=COMMERCIAL\nLicenseExpiryDate=2099-02-03\nContactEMail=xwturing@gmail.com\nServerID=BWGW-FKTG-N1UQ-PNHH\ncom.atlassian.servicedesk.LicenseTypeName=COMMERCIAL\njira.product.jira-servicedesk.NumberOfUsers=-1\nMaintenanceExpiryDate=2099-02-03\ncom.atlassian.servicedesk.enterprise=true\nLicenseID=LIDSEN-L7059162\nSEN=SEN-L7059162\nOrganisation=Evaluation license\nCreationDate=2016-01-05\ncom.atlassian.servicedesk.numRoleCount=-1\nlicenseVersion=2\nDescription=JIRA Service Desk (Server)\\: Evaluation\nEvaluation=true";
		String sll =
						"NumberOfUsers=-1\n" +
								"jira.product.jira-core.NumberOfUsers=-1\n" +
								"jira.NumberOfUsers=-1\n" +
								"PurchaseDate=2016-02-20\n" +
								"LicenseTypeName=COMMERCIAL\n" +
								"LicenseExpiryDate=2099-03-21\n" +
								"ContactEMail=xwturing@gmail.com\n" +
								"ServerID=BVDW-Q5Y0-CRXH-AI3I\n" +
								"jira.product.jira-core.Starter=false\n" +
								"jira.LicenseEdition=ENTERPRISE\n" +
								"jira.product.jira-core.active=true\n" +
								"MaintenanceExpiryDate=2099-03-21\n" +
								"LicenseID=LIDSEN-L7336401\n" +
								"SEN=SEN-L7336401\n" +
								"Organisation=Evaluation license\n" +
								"CreationDate=2016-02-20\n" +
								"licenseVersion=2\n" +
								"Description=JIRA Core (Server)\\: Evaluation\n" +
								"jira.active=true\n" +
								"jira.LicenseTypeName=COMMERCIAL\n" +
								"Evaluation=true";


		byte[] allData = sll.getBytes();
		Signature signature = Signature.getInstance("SHA1withDSA");
		signature.initVerify(PUBLIC_KEY);
		signature.update(allData);
		ByteArrayInputStream in = new ByteArrayInputStream(allData);
		DataInputStream dIn = new DataInputStream(in);
		int textLength = dIn.readInt();
		byte[] licenseText = new byte[textLength];
		dIn.read(licenseText);
		byte[] hash = new byte[dIn.available()];
		dIn.read(hash);
		String result = packLicense(allData, hash);
		System.out.println(result);
	}
}
今天在家想了一天,关于你的各种好,觉得还是应该再为自己争取一下机会的.我觉得婚后的幸福生活是建立在一定的物质基础之上的.我觉得我们家的物质条件不算差.有一辆出租车,在白水坝这边有两套房子,一套70一套150.然后还有四套回迁房.以后买房的话,你可以看哪边离你工作方便就在哪边买房,
然后我们卖掉两套回迁房应该差不多就够了.然后你说我的不成熟,我可以慢慢改,你也可以帮我一起改,我相信也不会需要多少时间的.我其实挺孝顺人的,我外公就和我们住在一起,然后我们可以一起孝顺叔叔阿姨.希望你能再给我一次机会~