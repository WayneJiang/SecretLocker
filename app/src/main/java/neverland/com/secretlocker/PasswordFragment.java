package neverland.com.secretlocker;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class PasswordFragment extends DialogFragment {

    public PasswordFragment() {
    }

    interface PasswordDialogListener {
        void OnPositiveClick();

        void OnNegativeClick();
    }

    private PasswordDialogListener mPasswordDialogListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mPasswordDialogListener = (PasswordDialogListener) getTargetFragment();
        } catch (Exception e) {

        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(R.layout.password_dialog);
        builder.setMessage(getString(R.string.password_dialog_message));
        builder.setPositiveButton(getString(R.string.password_dialog_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPasswordDialogListener.OnPositiveClick();
            }
        });

        builder.setNegativeButton(getString(R.string.password_dialog_negative), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPasswordDialogListener.OnNegativeClick();
            }
        });
        return builder.create();
    }
}
