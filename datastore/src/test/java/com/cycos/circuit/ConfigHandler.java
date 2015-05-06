package com.cycos.circuit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigHandler {
	private Properties properties;
	
	public ConfigHandler() {
		properties = new Properties();
	}
	
	public void set(String key, String value) {
		properties.setProperty(key, value);
		save();
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public void load() {
		try {
			properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		String file = getClass().getClassLoader().getResource("config.properties").getFile();
		System.out.println("File: " + file);
		OutputStream os;
		try {
			os = new FileOutputStream(getClass().getClassLoader().getResource("config.properties").getFile());
			properties.store(os, "");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
