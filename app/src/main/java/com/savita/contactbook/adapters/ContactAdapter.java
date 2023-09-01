package com.savita.contactbook.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.savita.contactbook.R;
import com.savita.contactbook.models.Contact;

import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private final LayoutInflater inflater;
    private final int layout;
    private List<Contact> contacts;
    private boolean isEditable;
    private Notification onDataChanged;

    public ContactAdapter(Context context, int layout, List<Contact> contacts) {
        super(context, layout);
        inflater = LayoutInflater.from(context);
        this.layout = layout;
        this.contacts = contacts;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean editable) {
        isEditable = editable;
    }

    public Notification getOnDataChanged() {
        return onDataChanged;
    }

    public void setOnDataChanged(Notification onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Contact contact = contacts.get(position);

        viewHolder.contact_index.setText(String.valueOf(position + 1));
        viewHolder.contact_name.setText(contact.getDisplayName());

        if(isEditable) {
            viewHolder.contact_remove_btn.setVisibility(View.VISIBLE);
            viewHolder.contact_remove_btn.setOnClickListener(x -> removeContact(contact.getId()));
        } else {
            viewHolder.contact_remove_btn.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void removeContact(String id) {
    }

    private class ViewHolder {
        private final TextView contact_index;
        private final TextView contact_name;
        private final ImageButton contact_remove_btn;
        ViewHolder(View view){
            contact_index = view.findViewById(R.id.contact_item_index);
            contact_name = view.findViewById(R.id.contact_item_name);
            contact_remove_btn = view.findViewById(R.id.remove_contact_btn);
        }
    }

    @FunctionalInterface
    public interface Notification {
        void Notify();
    }
}
