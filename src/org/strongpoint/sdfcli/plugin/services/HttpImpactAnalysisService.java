package org.strongpoint.sdfcli.plugin.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.strongpoint.sdfcli.plugin.dialogs.AuthenticationDialog;

public class HttpImpactAnalysisService {
	
	public static HttpImpactAnalysisService newInstance() {
		return new HttpImpactAnalysisService();
	}
	
	public JSONObject getImpactAnalysis(String changeRequestId, Shell shell, List<String> getScripIds) {
		AuthenticationDialog authenticationDialog = new AuthenticationDialog(shell);
		authenticationDialog.open();
		JSONObject results = new JSONObject();
		ArrayList<String> list = new ArrayList<String>(getScripIds);
		String removeWhitespaces = list.toString().substring(1,list.toString().length()-1).replace(" ", "");
		String strongpointURL = "https://rest.netsuite.com/app/site/hosting/restlet.nl?script=customscript_flo_impact_analysis_restlet&deploy=customdeploy_flo_impact_analysis_restlet&crId=" + changeRequestId + "&scriptIds=" + removeWhitespaces;
		System.out.println(strongpointURL);
		HttpGet httpGet = null;
		int statusCode;
		String responseBodyStr;
		CloseableHttpResponse response = null;
		try {
        	CloseableHttpClient client = HttpClients.createDefault();
            httpGet = new HttpGet(strongpointURL);
            httpGet.addHeader("Authorization", "NLAuth nlauth_account=" + authenticationDialog.getAccountIdStr() + ", nlauth_email=" + authenticationDialog.getEmailStr() + ", nlauth_signature=" + authenticationDialog.getPasswordStr() + ", nlauth_role=3");
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            statusCode = response.getStatusLine().getStatusCode();
            responseBodyStr = EntityUtils.toString(entity);
			
			if(statusCode >= 400) {
				results = new JSONObject();
				results.put("error", statusCode);
				throw new RuntimeException("HTTP Request returns a " +statusCode);
			}
			results = (JSONObject) JSONValue.parse(responseBodyStr);
		} catch (Exception exception) {
//			System.out.println("Request Deployment call error: " +exception.getMessage());
			results = new JSONObject();
			results.put("error", exception.getMessage());
//			throw new RuntimeException("Request Deployment call error: " +exception.getMessage());
		} finally {
			if (httpGet != null) {
				httpGet.reset();
			}
		}
		
		return results;
	}
	
}
