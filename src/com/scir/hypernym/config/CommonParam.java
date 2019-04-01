package com.scir.hypernym.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class CommonParam {
	private String propertyFileName;
	private ResourceBundle resourceBundle;

	public CommonParam() throws Exception {
		propertyFileName = "./Resource/config.properties";
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
				propertyFileName), "UTF8"));
		resourceBundle = new PropertyResourceBundle(in);
	}

	public String getString(String key) {
		if (key == null || key.equals("") || key.equals("null")) {
			return "";
		}
		String result = "";
		try {
			result = resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			e.printStackTrace();
		}
		return result;
	}
}
