package br.edu.ucsal.rotinewater;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_ABSOLUTE_MODE = "absolute_mode";
    private static final int[] QUICK_VOLUMES = {200, 300, 500};
    private static final int[] QUICK_BUTTON_IDS = {R.id.btnQuick1, R.id.btnQuick2, R.id.btnQuick3};

    private WaterStorage storage;
    private WaterProgressView waterView;
    private TextView tvDate;
    private TextView tvAlert;
    private TextView tvGoalValue;
    private TextView tvTotalValue;
    private TextView tvRemainingLabel;
    private TextView tvRemainingValue;
    private TextView tvEmpty;
    private LinearLayout llEntries;
    private EditText etGoal;
    private EditText etVolume;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
        tvDate = findViewById(R.id.tvDate);
        tvAlert = findViewById(R.id.tvAlert);
        tvGoalValue = findViewById(R.id.tvGoalValue);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvRemainingLabel = findViewById(R.id.tvRemainingLabel);
        tvRemainingValue = findViewById(R.id.tvRemainingValue);
        tvEmpty = findViewById(R.id.tvEmpty);
        llEntries = findViewById(R.id.llEntries);
        etGoal = findViewById(R.id.etGoal);
        etVolume = findViewById(R.id.etVolume);

        Button btnSaveGoal = findViewById(R.id.btnSaveGoal);
        Button btnAddEntry = findViewById(R.id.btnAddEntry);
        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnAddEntry.setOnClickListener(v -> addTypedEntry());

        for (int i = 0; i < QUICK_VOLUMES.length; i++) {
            int volume = QUICK_VOLUMES[i];
            Button quickButton = findViewById(QUICK_BUTTON_IDS[i]);
            quickButton.setText(getString(R.string.quick_add_format, volume));
            quickButton.setOnClickListener(v -> registerVolume(volume));
        }
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

    private void addTypedEntry() {
        int volume = readPositiveNumber(etVolume);
        if (volume == 0) {
            Toast.makeText(this, R.string.invalid_volume, Toast.LENGTH_SHORT).show();
            return;
        }
        etVolume.setText("");
        hideKeyboard(etVolume);
        registerVolume(volume);
    }

    private void registerVolume(int volumeMl) {
        storage.addEntry(volumeMl);
        refreshScreen();
    }

    private void confirmRemoval(int index) {
        List<WaterEntry> entries = storage.getEntries();
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

        tvDate.setText(formatToday());
        waterView.setMaxValue(goal);
        waterView.setCurrentValue(total);
        refreshStats(goal, total);
        refreshEntries();
    }

    private void refreshStats(int goal, int total) {
        boolean overGoal = total > goal;

        tvGoalValue.setText(getString(R.string.value_ml, goal));
        tvTotalValue.setText(getString(R.string.value_ml, total));

        tvAlert.setVisibility(overGoal ? View.VISIBLE : View.GONE);
        if (overGoal) {
            tvAlert.setText(getString(R.string.alert_banner, total - goal));
            tvRemainingLabel.setText(R.string.stat_exceeded);
            tvRemainingValue.setText(getString(R.string.value_ml_extra, total - goal));
            tvRemainingValue.setTextColor(getColor(R.color.alert_red));
        } else {
            tvRemainingLabel.setText(R.string.stat_remaining);
            tvRemainingValue.setText(getString(R.string.value_ml, goal - total));
            tvRemainingValue.setTextColor(getColor(R.color.text_primary));
        }
    }

    private void refreshEntries() {
        List<WaterEntry> entries = storage.getEntries();
        LayoutInflater inflater = LayoutInflater.from(this);

        llEntries.removeAllViews();
        tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = entries.size() - 1; i >= 0; i--) {
            WaterEntry entry = entries.get(i);
            int index = i;
            View row = inflater.inflate(R.layout.item_entry, llEntries, false);
            TextView tvTime = row.findViewById(R.id.tvEntryTime);
            TextView tvVolume = row.findViewById(R.id.tvEntryVolume);
            tvTime.setText(timeFormat.format(entry.getTimestamp()));
            tvVolume.setText(getString(R.string.value_ml, entry.getVolumeMl()));
            row.setOnLongClickListener(v -> {
                confirmRemoval(index);
                return true;
            });
            llEntries.addView(row);
        }
    }

    private String formatToday() {
        String text = dateFormat.format(new Date());
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
