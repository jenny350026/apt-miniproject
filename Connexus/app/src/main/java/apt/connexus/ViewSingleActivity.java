package apt.connexus;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ViewSingleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_single);

        Button back_to_streams = (Button) findViewById(R.id.back_to_streams);
        Button upload_img = (Button) findViewById(R.id.upload_img);

        back_to_streams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        upload_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewSingleActivity.this, UploadActivity.class);
                startActivity(intent);
            }
        });
    }

}
