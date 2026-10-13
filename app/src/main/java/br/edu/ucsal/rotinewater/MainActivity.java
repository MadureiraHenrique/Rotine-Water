package br.edu.ucsal.rotinewater;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_ABSOLUTE_MODE = "absolute_mode";

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
        View root = findViewById(R.id.main);
        int basePadding = root.getPaddingLeft();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    basePadding + systemBars.left,
                    basePadding + systemBars.top,
                    basePadding + systemBars.right,
                    basePadding + systemBars.bottom);
            return insets;
        });

        storage = new WaterStorage(this);
        bindViews();

        if (savedInstanceState == null) {
            etGoal.setText(String.valueOf(storage.getGoal()));
        } else {
            waterView.setAbsoluteMode(savedInstanceState.getBoolean(STATE_ABSOLUTE_MODE));
        }
        refreshScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        storage.checkNewDay();
        refreshScreen();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ABSOLUTE_MODE, waterView.isAbsoluteMode());
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
        lvEntries.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmRemoval(position);
            return true;
        });
    }

    private void saveGoal() {
        int goal = readPositiveNumber(etGoal);
        if (goal == 0) {
            Toast.makeText(this, R.string.invalid_goal, Toast.LENGTH_SHORT).show();
            return;
        }
        storage.setGoal(goal);
        hideKeyboard(etGoal);
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
        hideKeyboard(etVolume);
        refreshScreen();
    }

    private void confirmRemoval(int position) {
        List<WaterEntry> entries = storage.getEntries();
        int index = entries.size() - 1 - position;
        if (index < 0 || index >= entries.size()) {
            return;
        }
        WaterEntry entry = entries.get(index);
        String time = timeFormat.format(entry.getTimestamp());
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_entry_title)
                .setMessage(getString(R.string.delete_entry_message, time, entry.getVolumeMl()))
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    storage.removeEntry(index);
                    refreshScreen();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int readPositiveNumber(EditText field) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            return Math.max(value, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void hideKeyboard(View view) {
        view.clearFocus();
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
