package br.edu.ucsal.rotinewater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WaterStorageTest {

    private static final String PREFS_NAME = "water_prefs";

    private Context context;
    private WaterStorage storage;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        storage = new WaterStorage(context);
    }

    @Test
    public void startsWithDefaultGoalAndNoEntries() {
        assertEquals(2000, storage.getGoal());
        assertEquals(0, storage.getTotal());
        assertTrue(storage.getEntries().isEmpty());
    }

    @Test
    public void addEntryUpdatesTotalAndEntries() {
        storage.addEntry(250);
        storage.addEntry(500);

        List<WaterEntry> entries = storage.getEntries();
        assertEquals(750, storage.getTotal());
        assertEquals(2, entries.size());
        assertEquals(250, entries.get(0).getVolumeMl());
        assertEquals(500, entries.get(1).getVolumeMl());
    }

    @Test
    public void totalCanExceedGoal() {
        storage.setGoal(1000);
        storage.addEntry(800);
        storage.addEntry(600);

        assertEquals(1400, storage.getTotal());
        assertTrue(storage.getTotal() > storage.getGoal());
    }

    @Test
    public void changingGoalKeepsEntries() {
        storage.addEntry(300);
        storage.setGoal(3000);

        assertEquals(3000, storage.getGoal());
        assertEquals(300, storage.getTotal());
        assertEquals(1, storage.getEntries().size());
    }

    @Test
    public void removeEntryRecalculatesTotal() {
        storage.addEntry(250);
        storage.addEntry(500);
        storage.removeEntry(0);

        assertEquals(500, storage.getTotal());
        assertEquals(1, storage.getEntries().size());
        assertEquals(500, storage.getEntries().get(0).getVolumeMl());
    }

    @Test
    public void removeEntryWithInvalidIndexKeepsData() {
        storage.addEntry(250);
        storage.removeEntry(5);
        storage.removeEntry(-1);

        assertEquals(250, storage.getTotal());
        assertEquals(1, storage.getEntries().size());
    }

    @Test
    public void dataSurvivesNewStorageInstance() {
        storage.setGoal(2500);
        storage.addEntry(400);

        WaterStorage reopened = new WaterStorage(context);

        assertEquals(2500, reopened.getGoal());
        assertEquals(400, reopened.getTotal());
        assertEquals(1, reopened.getEntries().size());
    }

    @Test
    public void previousDayEntriesAreDiscardedButGoalIsKept() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("goal", 3000)
                .putString("date", "2000-01-01")
                .putInt("total", 500)
                .putString("entries", "[{\"volumeMl\":500,\"timestamp\":946684800000}]")
                .commit();

        WaterStorage nextDay = new WaterStorage(context);

        assertEquals(3000, nextDay.getGoal());
        assertEquals(0, nextDay.getTotal());
        assertTrue(nextDay.getEntries().isEmpty());
    }
}
