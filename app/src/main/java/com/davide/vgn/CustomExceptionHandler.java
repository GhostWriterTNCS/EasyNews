package com.davide.vgn;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

	private Thread.UncaughtExceptionHandler defaultUEH;
	private String localPath;

	public CustomExceptionHandler(String localPath) {
		this.localPath = localPath;
		this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	}

	public void uncaughtException(Thread t, Throwable e) {
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
		String timestamp = s.format(new Date());

		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String stacktrace = result.toString();
		printWriter.close();

		CustomIO.WriteFile(localPath, "log " + timestamp + ".txt", stacktrace);
		defaultUEH.uncaughtException(t, e);
	}
}
