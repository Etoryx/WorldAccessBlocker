package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.denfry.worldAccessBlocker.runtime.PlatformRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BypassManagerTest {

    @TempDir
    Path dataFolder;

    private WorldAccessBlocker plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(WorldAccessBlocker.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BypassManagerTest"));
        // runLater is a no-op mock so the debounced save never fires automatically during tests.
        when(plugin.getRuntime()).thenReturn(mock(PlatformRuntime.class));
    }

    @Test
    void noBypassByDefaultMeansRestrictionApplies() {
        BypassManager manager = new BypassManager(plugin);
        assertTrue(manager.hasNoBypass(UUID.randomUUID(), "nether"));
    }

    @Test
    void activeBypassExemptsPlayer() {
        BypassManager manager = new BypassManager(plugin);
        UUID id = UUID.randomUUID();
        manager.grantBypass(id, "nether", Instant.now().plusSeconds(3600));

        assertFalse(manager.hasNoBypass(id, "nether"));
        assertNotNull(manager.getActiveBypassUntil(id, "nether", Instant.now()));
    }

    @Test
    void expiredBypassIsIgnoredAndCleanedUp() {
        BypassManager manager = new BypassManager(plugin);
        UUID id = UUID.randomUUID();
        manager.grantBypass(id, "nether", Instant.now().minusSeconds(1));

        assertTrue(manager.hasNoBypass(id, "nether"));
        assertNull(manager.getActiveBypassUntil(id, "nether", Instant.now()));
    }

    @Test
    void removeBypassReportsWhetherSomethingWasRemoved() {
        BypassManager manager = new BypassManager(plugin);
        UUID id = UUID.randomUUID();
        manager.grantBypass(id, "end", Instant.now().plusSeconds(3600));

        assertTrue(manager.removeBypass(id, "end"));
        assertFalse(manager.removeBypass(id, "end"));
        assertTrue(manager.hasNoBypass(id, "end"));
    }

    @Test
    void bypassesSurviveSaveAndReload() {
        BypassManager manager = new BypassManager(plugin);
        UUID id = UUID.randomUUID();
        Instant until = Instant.now().plusSeconds(7200);
        manager.grantBypass(id, "elytra", until);
        manager.saveBypasses();

        BypassManager reloaded = new BypassManager(plugin);
        reloaded.loadBypasses();

        assertFalse(reloaded.hasNoBypass(id, "elytra"));
        Instant active = reloaded.getActiveBypassUntil(id, "elytra", Instant.now());
        assertNotNull(active);
        assertEquals(until.getEpochSecond(), active.getEpochSecond());
    }
}
