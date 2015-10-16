package apt.connexus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class ViewAllStreamActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_stream);

        ImageButton stream1btn = (ImageButton) findViewById(R.id.stream1);
        Button nearby_btn = (Button) findViewById(R.id.nearby_btn);
        Button search_btn = (Button) findViewById(R.id.search_btn);

        stream1btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, ViewSingleActivity.class);
                startActivity(intent);
            }
        });

        nearby_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, NearbyActivity.class);
                startActivity(intent);
            }
        });

        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, SearchResultActivity.class);
                startActivity(intent);
            }
        });

    }

}
