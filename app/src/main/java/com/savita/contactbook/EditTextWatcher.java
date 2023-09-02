package com.savita.contactbook;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.function.Consumer;

public class EditTextWatcher implements TextWatcher {
    private Consumer<String> callback;
    private EditText text;

    public EditTextWatcher(EditText text, Consumer<String> callback) {
        this(text);
        this.callback = callback;
    }

    public EditTextWatcher(EditText text) {
        this.text = text;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        if(callback != null) {
            callback.accept(text.getText().toString());
        }
    }
}
