package hu.blint.ssldroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SSLDroidReadLogs extends Activity{

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.read_logs);
    try {
      Process process = Runtime.getRuntime().exec("logcat -d");
      BufferedReader bufferedReader = new BufferedReader(
      new InputStreamReader(process.getInputStream()));

      StringBuilder log=new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        log.append(line);
      }
      TextView tv = (TextView)findViewById(R.id.logTextView);
      tv.setText(log.toString());
    } catch (IOException e) {
    }
  }
}
