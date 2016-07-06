package com.lyl.wifipassword;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wing_Li
 * 2016/7/6.
 */
public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private TextView mHeaderHint;

    private ArrayList<WifiInfo> mWifiInfoList;
    private MyAdapter myAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHeaderHint = (TextView) findViewById(R.id.header_hint);


        setListView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWifiInfo();
    }

    private void setListView() {
        mListView = (ListView) findViewById(R.id.listview);
        myAdapter = new MyAdapter();
        mListView.setAdapter(myAdapter);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cmb.setPrimaryClip(ClipData.newPlainText(getString(R.string.item_pasword_hint), mWifiInfoList.get(position)
                        .getPassword()));
                Toast.makeText(getApplicationContext(), R.string.copy_success, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void getWifiInfo() {
        Process process = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        StringBuffer wifiConf = new StringBuffer();
        try {
            process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataInputStream = new DataInputStream(process.getInputStream());
            dataOutputStream.writeBytes("cat /data/misc/wifi/wpa_supplicant.conf\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            InputStreamReader inputStreamReader = new InputStreamReader(dataInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                wifiConf.append(line);
            }
            bufferedReader.close();
            inputStreamReader.close();
            process.waitFor();
        } catch (Exception e) {
            return;
        } finally {

            if (TextUtils.isEmpty(wifiConf.toString())) {
                mHeaderHint.setText(R.string.not_root_hint_txt);
                Toast.makeText(getApplicationContext(), R.string.not_root_hint, Toast.LENGTH_LONG).show();
            }

            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }

        mWifiInfoList = new ArrayList<>();

        Pattern network = Pattern.compile("network=\\{([^\\}]+)\\}", Pattern.DOTALL);
        Matcher networkMatcher = network.matcher(wifiConf.toString());
        WifiInfo wifiInfo;
        while (networkMatcher.find()) {
            String networkBlock = networkMatcher.group();
            Pattern ssid = Pattern.compile("ssid=\"([^\"]+)\"");
            Matcher ssidMatcher = ssid.matcher(networkBlock);
            if (ssidMatcher.find()) {
                wifiInfo = new WifiInfo();
                wifiInfo.setName(ssidMatcher.group(1));
                Pattern psk = Pattern.compile("psk=\"([^\"]+)\"");
                Matcher pskMatcher = psk.matcher(networkBlock);
                if (pskMatcher.find()) {
                    wifiInfo.setPassword(pskMatcher.group(1));
                } else {
                    wifiInfo.setPassword(getString(R.string.empty_password));
                }
                mWifiInfoList.add(wifiInfo);
            }
        }
        myAdapter.notifyDataSetChanged();
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mWifiInfoList == null ? 0 : mWifiInfoList.size();
        }

        @Override
        public WifiInfo getItem(int position) {
            return mWifiInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_list, null);
                holder.name = (TextView) convertView.findViewById(R.id.item_name);
                holder.password = (TextView) convertView.findViewById(R.id.item_password);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            WifiInfo wifi = getItem(position);

            holder.name.setText(getString(R.string.iten_name_hint) + wifi.getName());
            holder.password.setText(getString(R.string.item_pasword_hint) + wifi.getPassword());

            return convertView;
        }

        class ViewHolder {
            TextView name;
            TextView password;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
        }
        return true;
    }
}
