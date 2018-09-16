package com.davide.vgn;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class CustomIO {
	public static boolean WriteFile(String localPath, String filename, String content) {
		File directory = new File(Environment.getExternalStorageDirectory(), localPath);
		if (!directory.exists()) {
			if (directory.mkdirs()) {
				Log.d("WriteFile", "Successfully created dir:" + directory.getAbsolutePath());
			} else {
				Log.d("WriteFile", "Failed to create dir:" + directory.getAbsolutePath());
				return false;
			}
		}

		File file = new File(directory, filename);
		try {
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			Log.d("WriteFile", "Successfully created file: " + file.getAbsolutePath());
		} catch (Exception ex) {
			Log.d("WriteFile", "Failed created file: " + file.getAbsolutePath());
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public static String ReadFile(String localPath, String filename) {
		File file = new File(Environment.getExternalStorageDirectory() + "/" + localPath, filename);
		if (file.exists()) {
			try {
				FileInputStream fin = new FileInputStream(file);
				BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
				reader.close();
				String s = sb.toString();
				Log.d("ReadFile", "Successfully readed file: " + file.getAbsolutePath());
				return s;
			} catch (Exception ex) {
				Log.d("ReadFile", "Failed readed file: " + file.getAbsolutePath());
				ex.printStackTrace();
			}
		} else {
			Log.d("ReadFile", "File not found: " + file.getAbsolutePath());
		}
		return "";
	}
}
