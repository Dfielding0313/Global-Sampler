package edu.miami.cs.donaldfielding.screamer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class NoticeDialogFragment extends DialogFragment {

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    NoticeDialogListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (NoticeDialogListener) context;
        } catch (ClassCastException e) {
        }
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder dialogBuilder;

        dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setMessage("Would you like to add local samples?");
        dialogBuilder.setPositiveButton("Yes",yesNoListener);
        dialogBuilder.setNegativeButton("No",yesNoListener);
        dialogBuilder.setCancelable(false);
        return (dialogBuilder.create());
    }
    private DialogInterface.OnClickListener yesNoListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int whatWasClicked) {

                    switch (whatWasClicked) {
                        case DialogInterface.BUTTON_POSITIVE:
                            listener.onDialogPositiveClick(NoticeDialogFragment.this);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            listener.onDialogNegativeClick(NoticeDialogFragment.this);
                            break;
                        default:
                            break;
                    }
                }
            };
}
