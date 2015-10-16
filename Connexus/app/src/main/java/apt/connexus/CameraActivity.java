package apt.connexus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class CameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Button camera_back_to_streams_btn = (Button) findViewById(R.id.camera_back_to_streams_btn);
        camera_back_to_streams_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, ViewAllStreamActivity.class);
                startActivity(intent);
            }
        });


    }


}
