package org.acoustixaudio.tonesmith;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.shajikhan.ladspa.amprack.AudioEngine;

import org.acoustixaudio.tonesmith.MainActivity;
import org.acoustixaudio.tonesmith.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class DataAdapter extends RecyclerView.Adapter <DataAdapter.ViewHolder> {
    MainActivity mainActivity;
    int totalItems = 0;
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList <Integer> plugins = new ArrayList<>();
    ArrayList<ViewHolder> holders = new ArrayList<>();

    public static class ControlDefault {
        Slider slider ;
        float def;
    }

    ArrayList <ControlDefault> controlDefaults = new ArrayList<>() ;

    public DataAdapter(MainActivity _mainActivity) {
        context = mainActivity = _mainActivity ;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        totalItems++;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        holders.add(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinearLayout linearLayout = holder.linearLayout;
        linearLayout.removeAllViews();
        holder.sliders = new ArrayList<>();
        if (linearLayout == null) {
            Log.wtf(TAG, "linear layout for plugin!") ;
            return ;
        }

        holder.reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0 ; i < controlDefaults.size() ; i ++) {
                    ControlDefault controlDefault = controlDefaults.get(i);
                    controlDefault.slider.setValue(controlDefault.def);
                }
            }
        });
        holder.switchMaterial.setUseMaterialThemeColors(true);
        holder.showControls.setChecked(false);
        holder.switchMaterial.setChecked(AudioEngine.getActivePluginEnabled(position));
        holder.buttonBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.showControls.performClick();
            }
        });

        holder.showControls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    linearLayout.setVisibility(View.VISIBLE);
                else
                    linearLayout.setVisibility(View.GONE);
            }
        });

        holder.switchMaterial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.switchMaterial.isChecked() && ! mainActivity.proVersion && AudioEngine.getActiveEnabledPlugins() > 1) {
                    mainActivity.startActivity(new Intent(mainActivity,Purchase.class));
                }
            }
        });
        
        holder.switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AudioEngine.togglePlugin(holder.getAdapterPosition(), isChecked);
            }
        });

        int ID = plugins.get(position);
        JSONObject data = null;
        try {
            data = mainActivity.allPlugins.getJSONObject(String.valueOf(ID));
//            Log.d(TAG, "onBindViewHolder: " + data);
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder: ", e);
            return;
        }

        String name;

        try {
             name = data.getString("name");
             Log.d(TAG, String.format ("----------------| %s | ------------------- ", name));
             holder.pluginName.setText(name);
             JSONObject _controls = data.getJSONObject("controls");
             Iterator<String> keys = _controls.keys();
//            Log.d(TAG, String.format ("_controls: %s", _controls));
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject controls = _controls.getJSONObject(key);
//                Log.d(TAG, String.format ("[control: %s] %s: %s", name, key, controls));
                double def = controls.getDouble("default");
                double min = controls.getDouble("minimum");
                double max = controls.getDouble("maximum");
                String cname = controls.getString("name");
                Log.d(TAG, String.format ("[control]: %s [%f %f %f]",
                        cname,
                        def, min, max));
                if (max < min) {
                    double t = max ;
                    max = min ;
                    min = t ;
                }

//                Log.d(TAG, String.format ("[controls]: %f %f %f", def, min, max));
                ArrayList<Integer> index = new ArrayList<>();
                if (controls.get("index") instanceof Integer) {
                    index.add(controls.getInt("index"));
                } else {
                    JSONArray indexes = controls.getJSONArray("index");
                    for (int i = 0; i < indexes.length(); i++) {
                        index.add(indexes.getInt(i));
                    }
                }

                Log.d(TAG, String.format ("[control index]: %s", index));

                TextView textView = new TextView(mainActivity);
                textView.setText(cname);
//                Log.d(TAG, String.format ("[%s]: [%s]", name,cname));
                linearLayout.addView(textView);

                Slider slider = new Slider(mainActivity);
                ControlDefault controlDefault = new ControlDefault();
                controlDefault.slider = slider ;
                controlDefault.def = (float) def;
                controlDefaults.add(controlDefault);
                holder.sliders.add(slider);

                try {
                    float value = AudioEngine.getActivePluginValueByIndex(position, index.get(0)) ;
                    Log.i(TAG, name +
                            String.format (" value %d:%d %f", position, index.get(0), value));
                    slider.setValue(value);
                    slider.setValueTo((float) max);
                    slider.setValueFrom((float) min);
                } catch (IllegalStateException e) {
                    Log.e(TAG, String.format("onBindViewHolder: error setting slider value: %d, %d", min, max), e);
                }

                boolean isSpinner = false;
                Spinner spinner ;
                spinner = new Spinner(context);

                Button prev = new Button(context);
                Button next = new Button(context);
                LinearLayout layout = new LinearLayout(context);

                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.HORIZONTAL);

                if (mainActivity.ampModels.has(name)) {
                    JSONObject control ;
                    ArrayList <String> models = new ArrayList<>();
                    try {
                        control = mainActivity.ampModels.getJSONObject(name) ;
                        if (control.has(String.valueOf(index.get(0)))) {
                            isSpinner = true ;
                            JSONArray modelsData = control.getJSONArray(String.valueOf(index.get(0)));
                            for (int x_ = 0 ; x_ < modelsData.length() ; x_++) {
                                models.add(modelsData.getString(x_));
                            }

                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "onBindViewHolder: error parsing amp model " + name, e);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                            android.R.layout.simple_spinner_item, models);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    prev = new Button(context);
                    next = new Button(context);

                    prev.setText("<");
                    next.setText(">");

                    prev.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int selected = spinner.getSelectedItemPosition();
                            if (selected > 0)
                                spinner.setSelection(selected - 1);
                        }
                    });

                    next.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int selected = spinner.getSelectedItemPosition();
                            if (selected < adapter.getCount() - 1)
                                spinner.setSelection(selected + 1);
                        }
                    });
                }

                if (isSpinner) {
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                    next.setLayoutParams(layoutParams);
                    prev.setLayoutParams(layoutParams);

                    prev.setBackgroundColor(0);
                    next.setBackgroundColor(0);

                    layout.addView(prev);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    spinner.setLayoutParams(params);
                    layout.addView(next);
                    layout.addView(spinner);

                    slider.setVisibility(View.GONE);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            slider.setValue(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }

                linearLayout.addView(slider);
                linearLayout.addView(layout);

                Log.i(TAG, "onBindViewHolder: slider set on click listener");
                slider.addOnChangeListener(new Slider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float v, boolean b) {
                        if (! b) {
                            if ( spinner.getSelectedItemPosition() != v) {
                                spinner.setSelection((int) v);
                            }

                            return;
                        }

                        for (int control = 0; control < index.size(); control++)
                            AudioEngine.setPluginControlByIndex(holder.getAdapterPosition(), index.get(control), v);
                    }
                });
            }
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder: ", e);
        }

    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ArrayList <Slider> sliders ;
        public LinearLayout linearLayout, buttonBox ;
        public LinearLayout root ;
        public Button reset;
        public TextView pluginName ;
        public SwitchMaterial switchMaterial ;
        public ToggleButton showControls ;

        public ViewHolder(View view) {
            super(view);

            sliders = new ArrayList<>();
            root = (LinearLayout) itemView ;
            linearLayout = root.findViewById(R.id.controls_layout);
            buttonBox = root.findViewById(R.id.pl1);
            pluginName = root.findViewById(R.id.name);
            switchMaterial = root.findViewById(R.id.toggle);
            showControls = root.findViewById(R.id.show_controls);
            reset = root.findViewById(R.id.reset);
        }
    }

    void addItem(int pluginID, int index) {
//        Log.d(TAG, String.format ("adding plugin: %d", pluginID));
        plugins.add(pluginID);
        notifyItemInserted(index);
    }

    @Override
    public long getItemId(int position) {
        return plugins.get(position);
    }

    public String getPreset () {
//        Log.d(TAG, String.format ("[save preset]: %d plugins", holders.size()));
        JSONObject jsonObject = new JSONObject();
        try {
            for (int i = 0 ; i < holders.size() ; i ++) {
                Log.d(TAG, String.format ("[%d]: {%s]", i, holders.get(i).pluginName.getText()));
                JSONObject object = new JSONObject();
                ViewHolder viewHolder = holders.get(i);

                object.put(String.valueOf(-1), viewHolder.switchMaterial.isChecked());

                for (int j = 0 ; j < viewHolder.sliders.size(); j ++) {
                    object.put(String.valueOf(j), viewHolder.sliders.get(j).getValue());
                }

                jsonObject.put(String.valueOf(i), object);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonObject.toString();
    }

    void loadPreset (String preset) {
//        Log.d(TAG, String.format ("loading preset: %s", preset));
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(preset);
            mainActivity.loadPreset(jsonObject);
        } catch (JSONException e) {
            Log.e(TAG, "loadPreset: ", e);
            return;
        }

    }

    void loadPreset (JSONObject jsonObject) {
        for (int key = 0 ; key < jsonObject.length() ; key ++) {
            try {
                JSONObject object = jsonObject.getJSONObject(String.valueOf(key));
                Iterator<String> controls = object.keys();

                Log.d(TAG, String.format("[%d] %s", key, object));
                while (controls.hasNext()) {
                    String control = controls.next();
                    if (object.get(control) instanceof JSONArray) {
                        Log.d(TAG, "loadPreset: " + object.getJSONArray(control));
                    }

                    if (Objects.equals(control, "-1")) {
                        boolean enabled = object.getBoolean("-1");
                        holders.get(key).switchMaterial.setChecked(enabled);
                    } else {
                        double value = object.getDouble(String.valueOf(control));
//                        Log.d(TAG, String.format ("[control]: %s [%f]", control, value));
                        Log.d(TAG, String.format ("[%s / %d]: [%s / %d]", key, holders.size(), control, holders.get(key).sliders.size()));
                        holders.get(key).sliders.get(Integer.parseInt(control)).setValue((float) value);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "loadPreset: ", e);
                return;
            }
        }
    }

    String[] getFactoryPresets () {
        try {
//            Log.d(TAG, "getFactoryPresets: getting list ...");
            String[] presets = mainActivity.getAssets().list("presets");
//            Log.d(TAG, String.format ("presets: %s", presets));
            return presets;
        } catch (IOException e) {
            Log.e(TAG, "getFactoryPresets: ", e);
            return null ;
        }
    }

    public void clear () {
        plugins.clear();
        notifyDataSetChanged();
    }
}
