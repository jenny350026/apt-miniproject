package apt.connexus;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class NearbyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);

        Button view_all_streams_btn = (Button) findViewById(R.id.view_all_streams_btn);
        view_all_streams_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


}
