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
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

public class LoginActivity extends Activity implements
        LoginDialog.dialogOnClick {


    private final static String TAG = "LOGIN_ACTIVITY";
    protected AccountManager accountManager;
    private Account[] accounts;
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        accountManager = AccountManager.get(getApplicationContext());
        accounts = accountManager.getAccountsByType("com.google");
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    public void onViewStream(View view){
        Log.v(TAG, "view stream clicked.");
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

    @Override
    public void onAccountClick(int position) {
        Account account = accounts[position];
        accountManager.invalidateAuthToken("com.google", null);
        accountManager.getAuthToken(account, "ah", null, this, new GetAuthTokenCallback(), null);
    }


    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            AsyncHttpClient client = new AsyncHttpClient();
            PersistentCookieStore myCookieStore = new PersistentCookieStore(getApplicationContext());
            Log.v(TAG, myCookieStore.toString());
            client.setCookieStore(myCookieStore);
            try {
                Bundle bundle;
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    startActivity(intent);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.v("TOKEN", token);
                    String url = "http://apt-miniproject-1078.appspot.com/_ah/login?continue=http://localhost/&auth=" + token;
                    client.post(url, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            String response = "";
                            try {
                                response = new String(responseBody, "UTF-8");
                                System.out.println("Fail response: " + response);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(LoginActivity.this, "Login fail.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
