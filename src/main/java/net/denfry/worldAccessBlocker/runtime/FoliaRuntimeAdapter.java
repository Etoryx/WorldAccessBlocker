package net.denfry.worldAccessBlocker.runtime;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoliaRuntimeAdapter implements PlatformRuntime {
    private final Plugin plugin;

    public FoliaRuntimeAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, i -> task.run(), initialDelayTicks, periodTicks);
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, i -> task.run());
    }

    @Override
    public void runLater(Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, i -> task.run(), Math.max(1L, delayTicks));
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        player.getScheduler().run(plugin, t -> task.run(), null);
    }

    @Override
    public void teleportPlayer(Player player, Location location) {
        // On Folia cross-region/cross-world teleports must be async.
        player.teleportAsync(location);
    }
}
