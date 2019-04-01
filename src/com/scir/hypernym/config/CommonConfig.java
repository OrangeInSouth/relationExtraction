package com.scir.hypernym.config;

import java.util.HashMap;
import java.util.Map;

public class CommonConfig {
	private static Map<String, String> configData;

	public static void Initialize() throws Exception {
		configData = new HashMap<String, String>();
		CommonParam config = new CommonParam();

		configData.put("FRONT", config.getString("FRONT"));
		configData.put("FRONT2", config.getString("FRONT2"));
		configData.put("cws", config.getString("cws"));
		configData.put("pos", config.getString("pos"));
		configData.put("DIM", config.getString("DIM"));
		
		configData.put("W2V", config.getString("W2V"));
	}

	public static String getString(String key) {
		return configData.get(key);
	}
}
