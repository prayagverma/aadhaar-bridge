package com.aadhaarconnect.bridge.sampler.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.aadhaarconnect.bridge.capture.model.auth.AuthCaptureData;
import com.aadhaarconnect.bridge.gateway.model.AuthResponse;
import com.aadhaarconnect.bridge.sampler.events.ABSEvent;
import com.aadhaarconnect.bridge.sampler.events.CompletionEvent;
import com.aadhaarconnect.bridge.sampler.events.StartingEvent;
import com.aadhaarconnect.bridge.sampler.util.GsonSerializerUtil;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;

public class AadhaarAuthAsyncTask extends AsyncTask<String, Void, AuthResponse> {
	protected static final String CONTENT_HEADER_ACCEPT = "Accept";
	protected static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	protected static final String CONTENT_HEADER_TYPE = "Content-Type";
	protected static final int CONNECTION_TIMEOUT = 10000;
	protected static final int SOCKET_TIMEOUT = 60000;
    protected static final String LOG_TAG = AadhaarAuthAsyncTask.class.getSimpleName();
	protected ProgressDialog mDialog;
	protected Context ctx;
    protected ABSEvent event;

	AuthCaptureData authData;

	public AadhaarAuthAsyncTask(Context ctx, ABSEvent event, AuthCaptureData authCaptureData) {
		this.authData = authCaptureData;
		this.ctx = ctx;
        this.event = event;
	}

	@Override
	protected void onPreExecute() {
		Log.d(LOG_TAG, "AadhaarDemo : pre execute async");
		mDialog = new ProgressDialog(ctx);
		mDialog.setCancelable(true);
		mDialog.setMessage("Please wait while communicating with the server");
	}

	@Override
	protected AuthResponse doInBackground(String... params) {
        EventBus.getDefault().postSticky(new StartingEvent().setEvent(event));

		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, SOCKET_TIMEOUT);

		HttpClient httpclient = getHttpClient(httpparams);
		HttpPost httppost = new HttpPost(params[0]);
		httppost.setHeader(CONTENT_HEADER_TYPE, CONTENT_TYPE_APPLICATION_JSON);
		httppost.setHeader(CONTENT_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);

		try {
			StringEntity entity = new StringEntity(GsonSerializerUtil.marshall(authData));
            Log.d(LOG_TAG, "REQUEST : " + GsonSerializerUtil.marshall(authData));
            Log.d(LOG_TAG, "URI : " + params[0]);
			entity.setContentType(CONTENT_TYPE_APPLICATION_JSON);
			httppost.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			Log.e(LOG_TAG, "DEMOAPP : Error while communicating with the server", e);

		}

		try {
			HttpResponse response = httpclient.execute(httppost);
			String responseContent = EntityUtils.toString(response.getEntity());
			if (response != null && response.getStatusLine().getStatusCode() == 200) {
                EventBus.getDefault().postSticky(
                        new CompletionEvent().setResponseString(responseContent).setEventASuccess().setEvent(event));
				Log.d(LOG_TAG,"RESPONSE : " +  responseContent);
				return GsonSerializerUtil.unmarshal(responseContent, AuthResponse.class);
			} else {
                Log.d(LOG_TAG, "RESPONSE CODE : " + response.getStatusLine().getStatusCode());
                EventBus.getDefault().postSticky(
                        new CompletionEvent().setResponseString(responseContent).
                                setCode(response.getStatusLine().getStatusCode()).setEventAFailure().setEvent(event));
				return buildErrorResponse();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "COMMUNICATION_ERROR : Error while communicating with the server. Check connectivity.", e);
            EventBus.getDefault().postSticky(
                    new CompletionEvent().setResponseString("COMMUNICATION_ERROR : Error while communicating with the server. Check connectivity.").
                            setCode(500).setEventAFailure().setEvent(event));
			return buildErrorResponse();
		}
	}

	private HttpClient getHttpClient(HttpParams params) {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new TrustAllCertsSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
			DefaultHttpClient client = new DefaultHttpClient(ccm, params);
			client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
			return client;
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

	private AuthResponse buildErrorResponse() {
		AuthResponse response = new AuthResponse();
		response.setSuccess(false);
		return response;
	}

	public static class TrustAllCertsSSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public TrustAllCertsSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
		        KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
		        UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}
}