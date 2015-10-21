package apt.connexus;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;

public class AccountList extends ListActivity {
    protected AccountManager accountManager;
    public static final String TAG="ACCOUNT_LIST";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        this.setListAdapter(new ArrayAdapter<Account>(this, R.layout.list_item, accounts));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Account account = (Account)getListView().getItemAtPosition(position);
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
                    // User input required
                    startActivity(intent);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.v("TOKEN", "account" + token);
                    String url = "http://apt-miniproject-1078.appspot.com/_ah/login?continue=http://localhost/&auth=" + token;
                    client.post(url, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Toast.makeText(AccountList.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Toast.makeText(AccountList.this, "Login fail.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent backToMainActivity = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(backToMainActivity);
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
