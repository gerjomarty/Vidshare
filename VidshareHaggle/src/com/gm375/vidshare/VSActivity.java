package com.gm375.vidshare;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

import org.haggle.Attribute;
import org.haggle.DataObject;
import org.haggle.Interface;
import org.haggle.Node;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.gm375.vidshare.util.DateHelper;

public class VSActivity extends TabActivity implements OnClickListener, TabHost.OnTabChangeListener {
    
    public static final int MENU_RECORD_VIDEO = 1;
    public static final int MENU_INTERESTS = 2;
    public static final int MENU_SHUTDOWN_HAGGLE = 3;
    
    public static final int REGISTRATION_FAILED_DIALOG = 1;
    public static final int SPAWN_DAEMON_FAILED_DIALOG = 2;
    public static final int PICTURE_ATTRIBUTES_DIALOG = 3;
    
    public static final String STREAM_ID_KEY = "com.gm375.vidshare.streamId";
    public static final String IS_STREAM_OVER_KEY = "com.gm375.vidshare.isStreamOver";
    
    private NodeAdapter nodeAdpt = null;
    private StreamAdapter streamAdpt = null;
    private ArrayAdapter<String> settingsAdpt = null;
    private Vidshare vs = null;
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
        
        ListView streamList = (ListView) findViewById(R.id.stream_list);
        streamAdpt = new StreamAdapter(this);
        streamList.setAdapter(streamAdpt);
        
        streamList.setOnItemClickListener(mOnStreamClicked);
        
        ListView neighList = (ListView) findViewById(R.id.neighbor_list);
        nodeAdpt = new NodeAdapter(this);
        neighList.setAdapter(nodeAdpt);
        
        ArrayList<String> settingsArrayList = new ArrayList<String>(1);
        settingsArrayList.add("Edit Interests");
        settingsAdpt = new ArrayAdapter<String>(this, R.layout.list_text_item, settingsArrayList);

        ListView settingsList = (ListView) findViewById(R.id.settings_list);
        settingsList.setAdapter(settingsAdpt);
        
        settingsList.setOnItemClickListener(mOnSettingClicked);
        
        TabHost mTabHost = getTabHost();
        
        mTabHost.addTab(mTabHost.newTabSpec("watchTab").setIndicator(getResources().getText(R.string.tab1)).setContent(R.id.stream_list));
        mTabHost.addTab(mTabHost.newTabSpec("neighboursTab").setIndicator(getResources().getText(R.string.tab2)).setContent(R.id.neighbor_list));
        mTabHost.addTab(mTabHost.newTabSpec("settingsTab").setIndicator(getResources().getText(R.string.tab3)).setContent(R.id.settings_list));
        
        mTabHost.setCurrentTab(0);
        setDefaultTab(0);
        
    }
    
    private AdapterView.OnItemClickListener mOnStreamClicked =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            Object obj = parent.getItemAtPosition(position);
            if (obj == null) {
                return;
            }
            String mapKey = (String) obj;
            Intent i = new Intent();
            i.setClass(getApplicationContext(), StreamViewer.class);
            i.putExtra(STREAM_ID_KEY, mapKey);
            startActivity(i);
        }
    };
    
    private AdapterView.OnItemClickListener mOnSettingClicked = 
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (position == 0) {
                // Interests
                final Intent i = new Intent();
                i.setClass(getApplicationContext(), InterestView.class);
                startActivityForResult(i, Vidshare.ADD_INTEREST_REQUEST);
            } else {
                return;
            }
        }
    };
    
    
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
        
        menu.add(0, MENU_RECORD_VIDEO, 0, "Stream Video");
        menu.add(0, MENU_SHUTDOWN_HAGGLE, 0, "Shutdown Haggle");

        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent i = new Intent();
        
        switch (item.getItemId()) {
        case MENU_RECORD_VIDEO:
            i.setClass(getApplicationContext(), AddVideoAttributeView.class);
            this.startActivityForResult(i, Vidshare.ADD_STREAM_ATTRIBUTES_REQUEST);
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
            
            Log.d(Vidshare.LOG_TAG, "***Got response from VideoStream activity.***");
            
            if (resultCode == Activity.RESULT_OK) {
                Log.d(Vidshare.LOG_TAG, "***Stream completely finished.***");

                
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
        // Nothing
        
    }
    
    
    public class StreamAdapter extends BaseAdapter {
        private Context mContext;
        private ConcurrentHashMap<String, Stream> mStreamMap;
        private ConcurrentHashMap<String, Boolean> mStreamAliveMap;
        
        StreamAdapter(Context mContext) {
            this.mContext = mContext;
            mStreamMap = ((Vidshare) getApplication()).mStreamMap;
            mStreamAliveMap = ((Vidshare) getApplication()).mStreamAliveMap;
        }

        @Override
        public int getCount() {
            if (mStreamMap == null || mStreamMap.size() == 0) {
                return 1;
            }
            return mStreamMap.size();
        }

        @Override
        public Object getItem(int position) {
            if (mStreamMap == null || mStreamMap.size() == 0) {
                Log.d(Vidshare.LOG_TAG, "*** StreamAdapter: getItem("+ position +") = NULL ***");
                return null;
            }
            Object obj = mStreamMap.keySet().toArray()[position];
            Log.d(Vidshare.LOG_TAG, "*** StreamAdapter: getItem("+ position +") = "+ obj +" ***");
            return obj;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
        public void refresh() {
            notifyDataSetChanged();
        }
        
        public synchronized void updateStreams(DataObject dObj) {
            String mapKey = dObj.getAttribute("id", 0).getValue();
            
            if (mStreamMap.containsKey(mapKey)) {
                Log.d("EVAL", "mStreamMap contains key: "+ mapKey);
                Log.d(Vidshare.LOG_TAG, "*** updateStreams() *** stream map contains this stream already, adding dObj ***");
                Stream str = mStreamMap.get(mapKey);
                if (str.hasStreamEnded()) {
                    Log.d("EVAL", "Stream has ended! Remove it!");
                    mStreamMap.remove(mapKey);
                    mStreamAliveMap.put(mapKey, false);
                } else {
                    Log.d("EVAL", "Stream has NOT ended! Add the dObj!");
                    str.addDataObject(dObj);
                }
            } else {
                Log.d("EVAL", "mStreamMap does NOT contain key: "+ mapKey);
                if (mStreamAliveMap.get(mapKey) == null) {
                    Log.d("EVAL", "Stream isn't in the alive map! Add it as true!");
                    mStreamAliveMap.put(mapKey, true);
                }
                if (mStreamAliveMap.get(mapKey) == true) {
                    Log.d("EVAL", "Stream is in the alive map as TRUE! Create a stream!");
                    Log.d(Vidshare.LOG_TAG, "*** updateStreams() *** stream map does not have this one, creating new entry ***");
                    mStreamMap.put(mapKey, new Stream(dObj, vs));
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout rl;
            if (convertView == null) {
                rl = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.stream_list_item, parent, false);
            } else {
                rl = (RelativeLayout) convertView;
            }
            
            if (mStreamMap == null || mStreamMap.size() == 0) {
                
                ((TextView) rl.findViewById(R.id.stream_list_item_date))
                    .setText("No streams available.");
                
                ((TextView) rl.findViewById(R.id.stream_list_item_tags))
                    .setText("Touch 'Settings', then 'Edit Interests' to edit your interests.");
                
            } else {
                Stream stream = (Stream) mStreamMap.values().toArray()[position];
                
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(stream.getStartTimeLong());
                
                ((TextView) rl.findViewById(R.id.stream_list_item_date))
                    .setText(DateHelper.dateFormatter(cal));
                
                String tagString = "Tags:";
                String[] tagArray = stream.getTags();
                for (String tagElement : tagArray) {
                    tagString = tagString + " " + tagElement;
                }
                
                ((TextView) rl.findViewById(R.id.stream_list_item_tags))
                    .setText(tagString);
            }
            
            return rl;
        }
        
    }
    
    
    public class NodeAdapter extends BaseAdapter {
        private Context mContext;
        private Node[] neighbours = null;
        
        // neighbours are updated for use here by synchronised method updateNeighbours(),
        // which is called from DataUpdater below.
        
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
                streamAdpt.updateStreams(dObj);
                break;
            }
            Log.d(Vidshare.LOG_TAG, "***Data updater finished***");
        }

    }




}
