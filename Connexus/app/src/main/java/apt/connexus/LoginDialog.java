package apt.connexus;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import apt.connexus.adapters.LoginAdapter;

public class LoginDialog extends DialogFragment {
    public interface dialogOnClick {
        void onAccountClick(int position);
    }
    private Account[] accounts;
    private dialogOnClick mCallBack;
    private Context mContext;
    public void setAccounts(Account[] accounts, Context context){
        this.accounts = accounts;
        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallBack = (dialogOnClick) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Login").setAdapter(new LoginAdapter(accounts, mContext), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallBack.onAccountClick(which);
            }
        });
        return builder.create();
    }
}