package com.android.intentfuzzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.android.intentfuzzer.util.LogUtils;
import com.android.intentfuzzer.util.SerializableTest;
import com.android.intentfuzzer.util.Utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class FuzzerActivity extends Activity{

	private ArrayList<String> cmpTypes = new ArrayList<String>();
	private String currentType = null;
	private Spinner typeSpinner = null;
	private ListView cmpListView = null;
	private ListView blackListView = null;
	private Button fuzzAllNullBtn = null;
	private Button fuzzAllSeBtn = null;
	private EditText blackeditEdit = null;
	private Button blackBtn = null;

	
	private ArrayAdapter<String> cmpAdapter = null;
	
	private ArrayList<String> cmpNames = new ArrayList<String>();
	private ArrayList<String> blackNames = new ArrayList<String>();
	private ArrayList<ComponentName> components = new ArrayList<ComponentName>();
	private PackageInfo pkgInfo = null;
	
	
	private static Map<Integer, String> ipcTypesToNames = new TreeMap<Integer, String>();
	private static Map<String, Integer> ipcNamesToTypes = new HashMap<String, Integer>();
	
	static {
		ipcTypesToNames.put(Utils.ACTIVITIES, "Activities");
		ipcTypesToNames.put(Utils.RECEIVERS, "Receivers");
		ipcTypesToNames.put(Utils.SERVICES, "Services");
		
		for (Entry<Integer, String> entry : ipcTypesToNames.entrySet()) {
			ipcNamesToTypes.put(entry.getValue(), entry.getKey());
		}
	}
	
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fuzzer);
		
		for (String name : ipcTypesToNames.values())
			cmpTypes.add(name);
		currentType = cmpTypes.get(0);
		
		initView();
		initTypeSpinner();
		
		//pkgInfo = getPkgInfo();
		pkgInfo = ((MyApp)getApplication()).packageInfo;
		if(pkgInfo == null){
			Toast.makeText(this, R.string.pkginfo_null, Toast.LENGTH_LONG).show();
			return;
		}
		
		
	}
	

	private PackageInfo getPkgInfo()
	{
		PackageInfo pkgInfo = null;
		
		Intent intent = getIntent();
		if (intent.hasExtra(Utils.PKGINFO_KEY)){
			pkgInfo = intent.getParcelableExtra(Utils.PKGINFO_KEY);
		}
		
		return pkgInfo;
	}
	
	private void initView(){
		typeSpinner = (Spinner) findViewById(R.id.type_select);
		cmpListView = (ListView) findViewById(R.id.cmp_listview);
		blackListView = (ListView) findViewById(R.id.black_listview);
		fuzzAllNullBtn = (Button) findViewById(R.id.fuzz_all_null);
		fuzzAllSeBtn = (Button) findViewById(R.id.fuzz_all_se);
		blackeditEdit = (EditText) findViewById(R.id.blackedit);
		blackBtn = (Button) findViewById(R.id.blackbtn);
		
	    cmpListView.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// TODO Auto-generated method stub
					ComponentName toSend = null;
					Intent intent = new Intent();
					String className =  cmpAdapter.getItem(position).toString();
					for (ComponentName cmpName : components) {
						if (cmpName.getClassName().equals(className)) {
							toSend = cmpName;
							break;
						}
					}
					
					intent.setComponent(toSend);

					if (sendIntentByType(intent, currentType)) {
						Toast.makeText(FuzzerActivity.this, "Sent Null " + intent, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(FuzzerActivity.this, "Send " + intent + " Failed!", Toast.LENGTH_LONG).show();
					}
				}
	       	
	       });
	    
	    cmpListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				ComponentName toSend = null;
				Intent intent = new Intent();
				String className =  cmpAdapter.getItem(position).toString();
				for (ComponentName cmpName : components) {
					if (cmpName.getClassName().equals(className)) {
						toSend = cmpName;
						break;
					}
				}
				
				intent.setComponent(toSend);
				//intent.putExtra("test", new SerializableTest());

				if (sendIntentByType(intent, currentType)) {
					Toast.makeText(FuzzerActivity.this, "Sent Serializeable " + intent, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(FuzzerActivity.this, "Send " + intent + " Failed!", Toast.LENGTH_LONG).show();
				}
				return true;
			}
       	
       });

	    
	    fuzzAllNullBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				for(ComponentName cmpName : components){
					Intent intent = new Intent();
					intent.setComponent(cmpName);
					if (sendIntentByType(intent, currentType)) {
						Toast.makeText(FuzzerActivity.this, "Sent Null " + intent, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(FuzzerActivity.this, R.string.send_faild, Toast.LENGTH_LONG).show();
					}
				}
			}
	    	
	    });
	    
	    fuzzAllSeBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				for(ComponentName cmpName : components){
					Intent intent = new Intent();
					intent.setComponent(cmpName);
					intent.putExtra("test", new SerializableTest());
					if (sendIntentByType(intent, currentType)) {
						Toast.makeText(FuzzerActivity.this, "Sent Serializeable " + intent, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(FuzzerActivity.this, R.string.send_faild, Toast.LENGTH_LONG).show();
					}
				}
			}
	    	
	    });
		blackBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String actstr = blackeditEdit.getText().toString();
				blackNames.add(actstr);
				ArrayAdapter<String>  blackAdapter = new ArrayAdapter<String>(v.getContext(), R.layout.component, blackNames );
				blackListView.setAdapter(blackAdapter);
				Toast.makeText(FuzzerActivity.this, "Add Black: " + actstr, Toast.LENGTH_LONG).show();
				//setListView();
			}

		});

	}
	
	private void initTypeSpinner(){
		ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, cmpTypes);
		typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpinner.setAdapter(typeAdapter);
		
		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				updateType();
				updateComponents(currentType);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}
	
	private void updateType() {
		Object selector = typeSpinner.getSelectedItem();
		if (selector != null) {
			currentType = typeSpinner.getSelectedItem().toString();
		}
	}
	
	
	private void updateComponents(String currentType){
		fuzzAllNullBtn.setVisibility(View.INVISIBLE);
		fuzzAllSeBtn.setVisibility(View.INVISIBLE);
		components = getComponents(currentType);
		cmpNames.clear();
		if(!components.isEmpty())
		{
			for(ComponentName cmpName : components){
				if (!cmpNames.contains(cmpName.getClassName()))
				{
					cmpNames.add(cmpName.getClassName());
				}
			}
			
			fuzzAllNullBtn.setVisibility(View.VISIBLE);
			fuzzAllSeBtn.setVisibility(View.VISIBLE);
			
		}else{
			Toast.makeText(this, R.string.no_compt, Toast.LENGTH_LONG).show();
		}
		setListView();
		
	}
	
	
	private ArrayList<ComponentName> getComponents(String currentType){
		PackageItemInfo items[] = null;
		ArrayList<ComponentName> cmpsFound = new ArrayList<ComponentName>();
		switch(ipcNamesToTypes.get(currentType)){
		case Utils.ACTIVITIES:
			items = pkgInfo.activities;
			break;
		case Utils.RECEIVERS:
			items = pkgInfo.receivers;
			break;
		case Utils.SERVICES:
			items = pkgInfo.services;
			break;
		default:
			items = pkgInfo.activities;
			break;
		}
			
		if (items != null){
			for (PackageItemInfo pkgItemInfo : items){
					cmpsFound.add(new ComponentName(pkgInfo.packageName, pkgItemInfo.name));
				}
		}
		
		return cmpsFound;
	}
	
	private void setListView(){
		cmpAdapter = new ArrayAdapter<String>(this, R.layout.component, cmpNames );
		cmpListView.setAdapter(cmpAdapter);
	}
	private boolean filterBlackList(String instr){
		for(String val:blackNames){
			if (instr.indexOf(val) >= 0){
				return true;
			}
		}
		return false;
	}
	
	private boolean sendIntentByType(Intent intent, String type) {
		boolean bret = false;
		LogUtils.e("getClassName:" +intent.getComponent().getClassName().toString());
		if(filterBlackList(intent.getComponent().getClassName().toString())){
			LogUtils.e("blackList:" + intent.getComponent().getClassName().toString());
			return false;
		}
		try {
				switch (ipcNamesToTypes.get(type)) {
				case Utils.ACTIVITIES:
					startActivity(intent);
					bret =  true;
					break;
				case Utils.RECEIVERS:
					sendBroadcast(intent);
					bret =  true;
					break;
				case Utils.SERVICES:
					startService(intent);
					bret =  true;
					break;
				default:
					bret =  true;
					break;
				}
		} catch (Exception e) {
			//e.printStackTrace();
			bret = false;
		}
		if(bret){
			LogUtils.e("sendIntentByType:" + intent.toString());
		}

		return bret;
		
	}
	
}
