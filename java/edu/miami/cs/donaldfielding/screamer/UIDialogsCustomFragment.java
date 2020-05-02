package edu.miami.cs.donaldfielding.screamer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

public class UIDialogsCustomFragment extends DialogFragment {
    public interface CustomDialogListener {
        public void onDialogSubmit(DialogFragment dialog, String entryText);
    }
        CustomDialogListener listener;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            try {
                listener = (CustomDialogListener) context;
            } catch (ClassCastException e) {
            }
        }
    View dialogView;
    Button submit;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("File Name Entry");
        dialogView = inflater.inflate(R.layout.dialog,container);
        submit = dialogView.findViewById(R.id.submit);
        (dialogView.findViewById(R.id.submit)).setOnClickListener(myClickHandler);
        return(dialogView);
    }
    private View.OnClickListener myClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.submit:
                    EditText name = dialogView.findViewById(R.id.file_entry);
                    listener.onDialogSubmit(UIDialogsCustomFragment.this, name.getText().toString());
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    };
    }

