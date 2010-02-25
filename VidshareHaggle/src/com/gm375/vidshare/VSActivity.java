package com.gm375.vidshare;

import java.util.ArrayList;

import org.haggle.Attribute;
import org.haggle.DataObject;
import org.haggle.Interface;
import org.haggle.Node;
import org.haggle.DataObject.DataObjectException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class VSActivity extends TabActivity implements OnClickListener, TabHost.OnTabChangeListener {
    
    public static final int MENU_RECORD_VIDEO = 1;
    public static final int MENU_INTERESTS = 2;
    public static final int MENU_SHUTDOWN_HAGGLE = 3;
    
    public static final int REGISTRATION_FAILED_DIALOG = 1;
    public static final int SPAWN_DAEMON_FAILED_DIALOG = 2;
    public static final int PICTURE_ATTRIBUTES_DIALOG = 3;
    
    //ListView mListView = null;
    //ArrayAdapter<String> mArrayAdapter = null;
    
    private NodeAdapter nodeAdpt = null;
    private Vidshare vs = null;
    private TextView neighListHeader = null;
    private boolean shouldRegisterWithHaggle = true;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onCreate() Thread ID ="+ Thread.currentThread().getId() +" ***");
        setContentView(R.layout.main);
        
        vs = (Vidshare) getApplication();
        vs.setVSActivity(this);
        
        neighListHeader = (TextView) findViewById(R.id.list_header);
        
        ListView neighList = (ListView) findViewById(R.id.neighbor_list);
        nodeAdpt = new NodeAdapter(this);
        neighList.setAdapter(nodeAdpt);
        // We also want to show context menu for longpressed items in the neighbor list.
        registerForContextMenu(neighList);
        
        //mListView = (ListView) findViewById(R.id.neighbor_list);
        //mArrayAdapter = new ArrayAdapter<String>(this, R.id.textview1);
        //mListView.setAdapter(mArrayAdapter);
        
        TabHost mTabHost = getTabHost();
        
        
        mTabHost.addTab(mTabHost.newTabSpec("tab1").setIndicator(getResources().getText(R.string.tab1)).setContent(R.id.neighbor_list));
        mTabHost.addTab(mTabHost.newTabSpec("tab2").setIndicator(getResources().getText(R.string.tab2)).setContent(R.id.textview2));
        mTabHost.addTab(mTabHost.newTabSpec("tab3").setIndicator(getResources().getText(R.string.tab3)).setContent(R.id.textview3));
        
        mTabHost.setCurrentTab(0);
        setDefaultTab(0);
        
    }
    
    
    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onRestart()***");
        // Indicate that we are restarting the dialog and that we shouldn't
        // reregister with Haggle
        shouldRegisterWithHaggle = false;
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onStart(): Free memory = " + Runtime.getRuntime().freeMemory() +" ***");
        
        if (shouldRegisterWithHaggle) {
            int ret = vs.initHaggle();

            if (ret != Vidshare.STATUS_OK) {
                String errorMsg = "Unknown error.";
                
                if (ret == Vidshare.STATUS_SPAWN_DAEMON_FAILED) {
                    errorMsg = "Vidshare could not start Haggle daemon.";
                } else if (ret == Vidshare.STATUS_REGISTRATION_FAILED) {
                    errorMsg = "Vidshare could not connect to Haggle.";
                }
                Log.d(Vidshare.LOG_TAG, "***Registration failed, showing alert dialog.***");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                .setMessage(errorMsg)
                .setCancelable(false)
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();

            }  else {
                Log.d(Vidshare.LOG_TAG, "***Registration with Haggle successful***");
            }
        }
        shouldRegisterWithHaggle = true;
        
    } // end onStart()
    
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onResume()***");
        
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Vidshare.LOG_TAG, "VSACtivity: ***onPause()***");

    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onStop()***");
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Vidshare.LOG_TAG, "VSActivity: ***onDestroy()***");
        vs.finiHaggle();
        
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_HOME:
            Log.d(Vidshare.LOG_TAG,"VSActivity: ***Key back, exit application and deregister with Haggle***");
            //vs.finiHaggle();
            break;
        }
        
        return super.onKeyDown(keyCode, event);
        
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        
    }
    
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        // TODO: Change titles and messages to point to XML strings.
        case Vidshare.STATUS_REGISTRATION_FAILED:
            return new AlertDialog.Builder(this)
            .setTitle("Haggle registration failed")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage("Haggle registration failed")
            .setPositiveButton(android.R.string.ok, this)
            .setCancelable(false)
            .create();
        case Vidshare.STATUS_SPAWN_DAEMON_FAILED:
            return new AlertDialog.Builder(this)
            .setTitle("Spawning of daemon failed")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage("Spawning of Haggle daemon failed")
            .setPositiveButton(android.R.string.ok, this)
            .setCancelable(false)
            .create();
        }

        return null;
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //menu.add(0, MENU_TAKE_PICTURE, 0, R.string.menu_take_picture).setIcon(R.drawable.ic_camera_indicator_photo);
        //menu.add(0, MENU_INTERESTS, 0, R.string.menu_interests);
        //menu.add(0, MENU_SHUTDOWN_HAGGLE, 0, R.string.menu_shutdown_haggle);

        // TODO: Change strings to strings in XML.
        
        menu.add(0, MENU_RECORD_VIDEO, 0, "Stream Video");
        menu.add(0, MENU_INTERESTS, 0, "Interests");
        menu.add(0, MENU_SHUTDOWN_HAGGLE, 0, "Shutdown Haggle");

        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent i = new Intent();
        
        switch (item.getItemId()) {
        case MENU_RECORD_VIDEO:
            //i.setClass(getApplicationContext(), VideoRecord.class);
            i.setClass(getApplicationContext(), AddVideoAttributeView.class);
            this.startActivityForResult(i, Vidshare.ADD_STREAM_ATTRIBUTES_REQUEST);
            return true;
        case MENU_INTERESTS:
            i.setClass(getApplicationContext(), InterestView.class);
            this.startActivityForResult(i, Vidshare.ADD_INTEREST_REQUEST);
            return true;
        case MENU_SHUTDOWN_HAGGLE:
            vs.shutdownHaggle();
            return true;
        }
        
        return false;
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (requestCode == Vidshare.ADD_STREAM_ATTRIBUTES_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                // Pass on data Intent with tags packaged inside.
                data.setClass(getApplicationContext(), VideoStream.class);
                this.startActivityForResult(data, Vidshare.STREAM_VIDEO_REQUEST);
            }
        } else if (requestCode == Vidshare.STREAM_VIDEO_REQUEST) {
            
            // TODO: This block will deal with the case when the video stream is stopped.
            
            Log.d(Vidshare.LOG_TAG, "***Got response from VideoStream activity.***");
            
            if (resultCode == Activity.RESULT_OK) {
                // Stream completely finished.
                
                Log.d(Vidshare.LOG_TAG, "!!! THIS IS A TEST, WE ARE TESTING IF A DATA OBJECT CAN BE SENT FROM HERE");
                try {
                    DataObject dObj = new DataObject();
                    dObj.addAttribute("tag", "case", 1);
                    //dObj.addAttribute("Video", "case", 1);
                    //dObj.addAttribute("Picture", "case", 1);
                    dObj.addAttribute("testtag", "THIS IS A TEST", 1);
                    dObj.addHash();
                    vs.getHaggleHandle().publishDataObject(dObj);
                } catch (DataObjectException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            } 
        } else if (requestCode == Vidshare.ADD_INTEREST_REQUEST) {
            
            String[] deletedInterests = data.getStringArrayExtra("deleted");
            String[] addedInterests = data.getStringArrayExtra("added");
            
            if (addedInterests != null && addedInterests.length != 0) {
                Attribute[] aa = new Attribute[addedInterests.length];
                for (int i = 0; i < addedInterests.length; i++) {
                    aa[i] = new Attribute("tag", addedInterests[i], 1);
                    Log.d(Vidshare.LOG_TAG, "***Added interest "+ addedInterests[i] +" ***");
                }
                vs.getHaggleHandle().registerInterests(aa);
            }

            if (deletedInterests != null && deletedInterests.length != 0) {
                Attribute[] aa = new Attribute[deletedInterests.length];
                for (int i = 0; i < deletedInterests.length; i++) {
                    aa[i] = new Attribute("tag", deletedInterests[i], 1);
                    Log.d(Vidshare.LOG_TAG, "***Deleted interest "+ deletedInterests[i] +" ***");
                }
                vs.getHaggleHandle().unregisterInterests(aa);
            }
            
        } else {
            Log.d(Vidshare.LOG_TAG, "***Unknown activity result***");
        }
    }
    
    
    @Override
    public void onTabChanged(String tabId) {
        // TODO Auto-generated method stub
        
    }
    
    // TODO: Use this when implementing video gallery thing to play back videos (Will we need this if we're streaming? Probably.)
    /*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        Log.d(Vidshare.LOG_TAG, "***onCreateContextMenu()***");
        
        if (v == gallery) {
            menu.setHeaderTitle("Picture");
            menu.add("Delete");
            menu.add("View Attributes");
            menu.add("Cancel");
        } else { 
            // TODO We should check for the correct view like for the gallery
            /* ListView lv = (ListView) v;
            NodeAdapter na = (NodeAdapter) lv.getAdapter();
            */
            /*
            menu.setHeaderTitle("Node Information");
            menu.add("Cancel");
        }
    }
    */
    
    
    public class NodeAdapter extends BaseAdapter {
        private Context mContext;
        private Node[] neighbours = null;
        
        public NodeAdapter(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            if (neighbours == null || neighbours.length == 0)
                return 1;
            else
                return neighbours.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
        public synchronized void updateNeighbors(Node[] neighs) {
            neighbours = neighs;

            Log.d(Vidshare.LOG_TAG, "Updated neighbour array.");
            notifyDataSetChanged();
        }

        public void refresh() {
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(mContext).inflate(R.layout.neighbour_list_item, parent, false);
            } else {
                tv = (TextView) convertView;
            }
            
            if (neighbours == null || neighbours.length == 0) {
                tv.setText("No active neighbours.");
            } else {
                Node node = neighbours[position];
                
                String ifaceInfo = new String(" [");
                for (int i=0; i<node.getNumInterfaces(); i++) {
                    Interface iface = node.getInterfaceN(i);
                    if (iface != null) {
                        if (iface.getType() == Interface.IFTYPE_BLUETOOTH && iface.getStatus() == Interface.IFSTATUS_UP) {
                            ifaceInfo += " BT";
                        }
                        if (iface.getType() == Interface.IFTYPE_ETHERNET && iface.getStatus() == Interface.IFSTATUS_UP) {
                            ifaceInfo += " WiFi";
                        }
                    }
                }
                ifaceInfo += " ]";
                tv.setText(node.getName() + ifaceInfo);
            }
            
            return tv;
        }
        
        public String getInformation(int position) {
            if ((neighbours.length > 0) && (position >= 0)) {
                Node n = neighbours[position];
                return n.toString();
            } else {
                return "Node Information";
            }
        }

    }
    
    
    
    public class DataUpdater implements Runnable {
        int type;
        DataObject dObj = null;
        Node[] neighbours = null;
        
        public DataUpdater(DataObject dObj) {
            this.type = org.haggle.EventHandler.EVENT_NEW_DATAOBJECT;
            this.dObj = dObj;
        }
        
        public DataUpdater(Node[] neighbours) {
            this.type = org.haggle.EventHandler.EVENT_NEIGHBOR_UPDATE;
            this.neighbours = neighbours;
        }

        @Override
        public void run() {
            Log.d(Vidshare.LOG_TAG, "***Running data updater*** Thread ID="+ Thread.currentThread().getId());
            
            switch(type) {
            case org.haggle.EventHandler.EVENT_NEIGHBOR_UPDATE:
                Log.d(Vidshare.LOG_TAG, "EVENT_NEIGHBOR_UPDATE");
                nodeAdpt.updateNeighbors(neighbours);
                break;
            case org.haggle.EventHandler.EVENT_NEW_DATAOBJECT:
                Log.d(Vidshare.LOG_TAG, "EVENT_NEW_DATAOBJECT");
                // TODO: Do stuff with new data objects.
                Toast.makeText(getApplicationContext(), "New data object! "+ dObj.getFileName(), Toast.LENGTH_SHORT).show();
                //imgAdpt.updatePictures(dObj);
                break;
            }
            Log.d(Vidshare.LOG_TAG, "***Data updater finished***");
        }

    }




}
