package br.edu.ucsal.rotinewater;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WaterStorage storage;
    private WaterProgressView waterView;
    private TextView tvAlert;
    private TextView tvTotal;
    private EditText etGoal;
    private EditText etVolume;
    private ArrayAdapter<String> entriesAdapter;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storage = new WaterStorage(this);
        bindViews();
        etGoal.setText(String.valueOf(storage.getGoal()));
        refreshScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        storage.checkNewDay();
        refreshScreen();
    }

    private void bindViews() {
        waterView = findViewById(R.id.waterView);
        tvAlert = findViewById(R.id.tvAlert);
        tvTotal = findViewById(R.id.tvTotal);
        etGoal = findViewById(R.id.etGoal);
        etVolume = findViewById(R.id.etVolume);

        Button btnSaveGoal = findViewById(R.id.btnSaveGoal);
        Button btnAddEntry = findViewById(R.id.btnAddEntry);
        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnAddEntry.setOnClickListener(v -> addEntry());

        entriesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView lvEntries = findViewById(R.id.lvEntries);
        lvEntries.setAdapter(entriesAdapter);
        lvEntries.setEmptyView(findViewById(R.id.tvEmpty));
    }

    private void saveGoal() {
        int goal = readPositiveNumber(etGoal);
        if (goal == 0) {
            Toast.makeText(this, R.string.invalid_goal, Toast.LENGTH_SHORT).show();
            return;
        }
        storage.setGoal(goal);
        refreshScreen();
    }

    private void addEntry() {
        int volume = readPositiveNumber(etVolume);
        if (volume == 0) {
            Toast.makeText(this, R.string.invalid_volume, Toast.LENGTH_SHORT).show();
            return;
        }
        storage.addEntry(volume);
        etVolume.setText("");
        refreshScreen();
    }

    private int readPositiveNumber(EditText field) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            return Math.max(value, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refreshScreen() {
        int goal = storage.getGoal();
        int total = storage.getTotal();

        waterView.setMaxValue(goal);
        waterView.setCurrentValue(total);
        tvTotal.setText(getString(R.string.total_format, total, goal));

        boolean overGoal = total > goal;
        tvAlert.setVisibility(overGoal ? View.VISIBLE : View.GONE);
        if (overGoal) {
            tvAlert.setText(getString(R.string.alert_banner, total - goal));
        }

        refreshEntries();
    }

    private void refreshEntries() {
        List<WaterEntry> entries = storage.getEntries();
        entriesAdapter.clear();
        for (int i = entries.size() - 1; i >= 0; i--) {
            WaterEntry entry = entries.get(i);
            String time = timeFormat.format(entry.getTimestamp());
            entriesAdapter.add(getString(R.string.entry_format, time, entry.getVolumeMl()));
        }
    }
}
