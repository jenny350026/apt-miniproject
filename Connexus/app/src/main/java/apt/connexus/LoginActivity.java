package apt.connexus;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;


import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.cookie.Cookie;

public class LoginActivity extends Activity implements
        LoginDialog.dialogOnClick {


    private final static String TAG = "LOGIN_ACTIVITY";
    protected AccountManager accountManager;
    private Account[] accounts;
    private Context context = this;
    public static boolean signedIn = false;
    private TextView status_textView;
    public static String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        status_textView = (TextView) findViewById(R.id.status_textView);
        checkSignedIn();
        accountManager = AccountManager.get(getApplicationContext());
        accounts = accountManager.getAccountsByType("com.google");
    }

    private void checkSignedIn() {
        PersistentCookieStore myCookieStore = new PersistentCookieStore(getApplicationContext());
        Log.v(TAG, "my cookie store: " + myCookieStore.toString());
        ArrayList<Cookie> list = new ArrayList<>(myCookieStore.getCookies());
        for(Cookie cookie: list) {
            if("apt-miniproject-1078.appspot.com".equals(cookie.getDomain())) {
                status_textView.setText("You have already signed in!");
                signedIn = true;
                return;
            }
        }
        signedIn = false;
        status_textView.setText("You have not signed in");
    }


    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /************************
     * Buttons onClick
     * *********************/

    public void onViewStream(View view){
        Intent intent = new Intent(this, ViewAllStreamActivity.class);
        startActivity(intent);
    }

    public void login_with_existing_account(View view) {
        if(isOnline()){
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setAccounts(accounts, context);
            loginDialog.show(getFragmentManager(), "NoticeDialogFragment");
        }
        else {
            Toast.makeText(this, "You are offline.", Toast.LENGTH_SHORT).show();
        }
    }


    public void sign_out(View view) {
        PersistentCookieStore myCookieStore = new PersistentCookieStore(getApplicationContext());
        ArrayList<Cookie> list = new ArrayList<>(myCookieStore.getCookies());
        for(Cookie cookie: list) {
            if("apt-miniproject-1078.appspot.com".equals(cookie.getDomain())) {
                myCookieStore.deleteCookie(cookie);
            }
        }
        signedIn = false;
        status_textView.setText("You have signed out");
    }
    /************************
     * End of Buttons onClick
     * *********************/

    @Override
    public void onAccountClick(int position) {
        Account account = accounts[position];
        accountManager.invalidateAuthToken("com.google", null);
        accountManager.getAuthToken(account, "ah", null, this, new GetAuthTokenCallback(), null);
        Log.v(TAG, "account name = " + account.name);
        userEmail = account.name;
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            AsyncHttpClient client = new AsyncHttpClient();
            PersistentCookieStore myCookieStore = new PersistentCookieStore(getApplicationContext());
            client.setCookieStore(myCookieStore);
            try {
                Bundle bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    startActivity(intent);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    String url = "http://apt-miniproject-1078.appspot.com/_ah/login?continue=http://localhost/&auth=" + token;
                    client.post(url, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                            status_textView.setText("You have already signed in!");
                            signedIn = true;
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                            status_textView.setText("You have already signed in!");
                            signedIn = true;
                        }
                    });

                }
            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
