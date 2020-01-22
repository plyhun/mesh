package com.gentics.mesh.core.ssl;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public class SSLTestClient {

	private static final Logger log = LoggerFactory.getLogger(SSLTestClient.class);

	public static final String CLIENT_CERT_PEM = "src/test/resources/client-ssl/alice.pem";
	public static final String CLIENT_KEY_PEM = "src/test/resources/client-ssl/alice.key";
	public static final String CA_CERT = "src/test/resources/client-ssl/server.pem";
	public static final String FMT_TEST_URL = "https://localhost:%s/api/v1";

	/**
	 * Invoke call to /api/v1
	 * 
	 * @param httpsPort
	 * @param sendClientAuth
	 * @param trustAll
	 * @throws IOException
	 */
	public static void call(int httpsPort, boolean sendClientAuth, boolean trustAll) throws IOException {
		OkHttpClient client = client(sendClientAuth, trustAll);
		Request request = new Request.Builder().url(String.format(FMT_TEST_URL, httpsPort)).build();

		log.info("Performing request: " + request);
		Response response = client.newCall(request).execute();
		log.info("Received response: " + response);
	}

	public static OkHttpClient client(boolean sendClientAuth, boolean trustAll) {
		KeyManager[] keyManagers = null;
		TrustManager[] trustManagers = null;

		if (sendClientAuth) {
			try {
				log.info("Loading private key from " + CLIENT_KEY_PEM);
				keyManagers = getKeyManagersPem();
			} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | IOException | CertificateException e) {
				rethrow(e, "Could not create key managers");
			}
		} else {
			log.info("Not sending client certificate");
		}

		if (trustAll) {
			trustManagers = getDummyTrustManager();
		} else {
			try {
				trustManagers = getTrustManagers();
			} catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
				rethrow(e, "Could not create trust managers");
			}
		}
		SSLContext sslCtx = null;

		try {
			sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(keyManagers, trustManagers, null);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			rethrow(e, "Could not create SSL context");
		}

		Builder builder = new OkHttpClient.Builder();
		builder.hostnameVerifier((hostName, sslSession) -> true);
		return builder.sslSocketFactory(sslCtx.getSocketFactory(), (X509TrustManager) trustManagers[0])
			.build();
	}

	private static KeyManager[] getKeyManagersPem()
		throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException {
		char[] randomKeyStorePass = UUIDUtil.randomUUID().toCharArray();
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate clientCert = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(CLIENT_CERT_PEM));
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		String principal = clientCert.getSubjectX500Principal().getName();

		try (PEMParser pemParser = new PEMParser(new FileReader(CLIENT_KEY_PEM))) {
			PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemParser.readObject());
			keyStore.load(null);
			keyStore.setCertificateEntry(principal + "Cert", clientCert);
			keyStore.setKeyEntry(principal + "Key", privateKey, randomKeyStorePass, new Certificate[] { clientCert });
			keyManagerFactory.init(keyStore, randomKeyStorePass);
		}

		return keyManagerFactory.getKeyManagers();
	}

	private static TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(CA_CERT));

		keyStore.load(null);
		keyStore.setCertificateEntry(caCert.getSubjectX500Principal().getName(), caCert);
		trustManagerFactory.init(keyStore);

		return trustManagerFactory.getTrustManagers();
	}

	private static TrustManager[] getDummyTrustManager() {
		return new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			}
		};
	}

	private static void rethrow(Throwable e, String msg) {
		log.error(msg + ": " + e.getMessage());
		throw new RuntimeException(e);
	}

}
