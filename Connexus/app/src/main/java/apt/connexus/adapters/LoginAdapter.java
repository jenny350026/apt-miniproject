package apt.connexus.adapters;

import android.accounts.Account;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import apt.connexus.R;

public class LoginAdapter extends BaseAdapter {
    private Account[] accounts;
    private Context context;
    public LoginAdapter(Account[] accounts, Context context) {
        this.accounts = accounts;
        this.context = context;
    }

    @Override
    public int getCount() {
        return accounts.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if(convertView == null){
            LayoutInflater a = LayoutInflater.from(context);
            convertView = a.inflate(R.layout.account_list, null);

        }
        textView = (TextView) convertView.findViewById(R.id.account_list_textView);
        textView.setText(accounts[position].name);

        return textView;
    }
}
