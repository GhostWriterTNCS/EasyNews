package com.davide.vgn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
	private AppCompatActivity activity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		activity = this;

		final TextInputEditText urlsInput = (TextInputEditText) findViewById(R.id.urlsInput);
		urlsInput.setText(MainActivity.sp.getString(Strings.urls, ""));
		final TextInputEditText urlsInputJS = (TextInputEditText) findViewById(R.id.urlsInputJS);
		urlsInputJS.setText(MainActivity.sp.getString(Strings.urlsJS, ""));

		final EditText emailFrom = (EditText) findViewById(R.id.emailFrom);
		emailFrom.setText(MainActivity.sp.getString(Strings.emailFrom, ""));
		final EditText emailTo = (EditText) findViewById(R.id.emailTo);
		emailTo.setText(MainActivity.sp.getString(Strings.emailTo, ""));

		Button saveFeeds = (Button) findViewById(R.id.feedsSave);
		saveFeeds.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SharedPreferences.Editor editor = MainActivity.sp.edit();
				editor.putString(Strings.urls, urlsInput.getText().toString());
				editor.putString(Strings.urlsJS, urlsInputJS.getText().toString());
				editor.apply();
				Toast.makeText(activity, MainActivity.context.getString(R.string.done), Toast.LENGTH_SHORT).show();
			}
		});

		Button saveEmail = (Button) findViewById(R.id.emailSave);
		saveEmail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SharedPreferences.Editor editor = MainActivity.sp.edit();
				editor.putString(Strings.emailFrom, emailFrom.getText().toString());
				editor.putString(Strings.emailPassword, ((EditText) findViewById(R.id.emailPassword)).getText().toString());
				editor.putString(Strings.emailTo, emailTo.getText().toString());
				editor.apply();
				Toast.makeText(activity, MainActivity.context.getString(R.string.done), Toast.LENGTH_SHORT).show();
			}
		});

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}
