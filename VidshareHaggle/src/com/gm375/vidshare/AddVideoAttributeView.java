package com.gm375.vidshare;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class AddVideoAttributeView extends Activity {
    
    private ArrayList<String> attributeList = new ArrayList<String>();
    private ArrayAdapter<String> attributeAdpt = null;
    private EditText entry;
    private ListView attributeListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.add_video_attribute_view);
        
        entry = (EditText) findViewById(R.id.entry);
        
        entry.setOnKeyListener(new View.OnKeyListener() {
            
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                
                switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                    if (entry.hasFocus()) {
                        addEnteredAttribute();
                    }
                    return true;
                }
                return false;
            }
        });
        
        attributeListView = (ListView) findViewById(R.id.attribute_list);
        attributeAdpt = new ArrayAdapter<String>(this, R.layout.list_text_item);
        attributeListView.setAdapter(attributeAdpt);
        
        registerForContextMenu(attributeListView);
        
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    } 
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    private void addAttribute(String attr) {
        if (attributeAdpt.getPosition(attr) >= 0)
            return;

        attributeAdpt.add(attr);
        attributeList.add(attr);
    }
    
    private String removeAttribute(int pos) {
        String str = attributeAdpt.getItem(pos);
        if (str != null) {
            attributeAdpt.remove(str);
            attributeList.remove(str);
        }
        return str;
    }
    
    private void addEnteredAttribute() {
        Editable e = entry.getText();
        
        String attrStr = e.toString();
    
        if (attrStr.length() == 0)
            return;
    
        String interestArray[] = attrStr.split(" ");
        
        for (int i = 0; i < interestArray.length; i++) {
            addAttribute(interestArray[i].trim());
        }
        
        Log.d(Vidshare.LOG_TAG, "***Entry. "+ attrStr +" ***");
    
        e.clear();
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == "Delete") {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            removeAttribute(info.position);
        }
        
        return super.onContextItemSelected(item);     
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        
        menu.setHeaderTitle("Attribute");
        menu.add("Delete");
        menu.add("Cancel");
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        
        case KeyEvent.KEYCODE_BACK:
            if (attributeList.size() == 0) {
                setResult(RESULT_CANCELED);
                break;
            }

            Intent i = new Intent();
            i.putExtra("attributes",
                    attributeList.toArray(new String[attributeList.size()]));
            setResult(RESULT_OK, i);
            break;
        case KeyEvent.KEYCODE_ENTER:
            if (entry.hasFocus()) {
                addEnteredAttribute();
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }
    
}
