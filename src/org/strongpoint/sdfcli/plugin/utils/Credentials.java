package org.strongpoint.sdfcli.plugin.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Credentials {
	
	private static final String userHomePath = System.getProperty("user.home");
	
	public static final JSONObject getCredentialsFromFile() {
		StringBuilder contents = new StringBuilder();
		String str;
		File file = new File(userHomePath + "/sdfcli/credentials.json");
		JSONObject credentials = null;
		try {
			if(file.exists() && !file.isDirectory()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				while((str = reader.readLine())  != null) {
					contents.append(str);
				}
//				System.out.println("FILE Contents: " +contents.toString());
				credentials = (JSONObject) new JSONParser().parse(contents.toString());	
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return credentials;
	}
	
	public static final boolean isCredentialsFileExists() {
		File file = new File(userHomePath + "/sdfcli/credentials.json");
		if(file.exists() && !file.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

}
