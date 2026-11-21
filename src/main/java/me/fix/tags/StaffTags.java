package me.fix.tags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts rank badges in front of names above players' heads and orders the tablist.
 * UltimateDonutSMP gives every player a private scoreboard, so the nametag team is added
 * to whichever scoreboard each viewer is using right now.
 */
public class StaffTags extends JavaPlugin implements Listener {

    private static final String TEAM_PREFIX = "dtag";
    private static final LegacyComponentSerializer SER =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private record Rank(String permission, String prefix, int order, boolean stack) {
    }

    /** What one player shows: the full prefix text and their tablist order. */
    private record Tag(String prefix, int order) {
    }

    private final List<Rank> ranks = new ArrayList<>();
    private boolean reverse;
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        load();
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListOrder(0);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                removeFrom(viewer.getScoreboard(), p.getName());
            }
        }
    }

    private void load() {
        reloadConfig();
        ranks.clear();
        reverse = getConfig().getBoolean("reverse-tab-order", false);
        ConfigurationSection sec = getConfig().getConfigurationSection("ranks");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection r = sec.getConfigurationSection(key);
                if (r == null) continue;
                String perm = r.getString("permission", "");
                if (perm.isEmpty()) continue;
                ranks.add(new Rank(perm, r.getString("prefix", ""), r.getInt("order", 0), r.getBoolean("stack", false)));
            }
        }
        if (task != null) task.cancel();
        long every = Math.max(5, getConfig().getInt("refresh-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(this, this::refresh, 20L, every);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            load();
            refresh();
            s.sendMessage("Staff tags reloaded.");
        } else {
            s.sendMessage("/stafftags reload");
        }
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // the other plugin swaps scoreboards around join, so apply a few times
        for (long d : new long[]{5L, 20L, 60L}) {
            Bukkit.getScheduler().runTaskLater(this, this::refresh, d);
        }
    }

    private Tag tagFor(Player p) {
        StringBuilder prefix = new StringBuilder();
        int order = 0;
        boolean usedMain = false;
        for (Rank r : ranks) {
            if (!p.hasPermission(r.permission())) continue;
            if (!r.stack()) {
                if (usedMain) continue;
                usedMain = true;
            }
            prefix.append(r.prefix());
            order = Math.max(order, r.order());
        }
        return prefix.length() == 0 ? null : new Tag(prefix.toString(), order);
    }

    private void refresh() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Tag[] tags = new Tag[online.size()];
        for (int i = 0; i < online.size(); i++) {
            Player p = online.get(i);
            tags[i] = tagFor(p);
            int order = tags[i] == null ? 0 : tags[i].order();
            p.setPlayerListOrder(reverse ? -order : order);
        }
        for (Player viewer : online) {
            Scoreboard sb = viewer.getScoreboard();
            for (int i = 0; i < online.size(); i++) {
                String name = online.get(i).getName();
                Tag tag = tags[i];
                if (tag == null) {
                    removeFrom(sb, name);
                    continue;
                }
                String teamName = TEAM_PREFIX + String.format("%02d", 99 - Math.min(99, tag.order() / 2))
                        + "_" + Integer.toHexString(tag.prefix().hashCode());
                Team team = sb.getTeam(teamName);
                if (team == null) {
                    team = sb.registerNewTeam(teamName);
                    team.color(NamedTextColor.WHITE);
                }
                Component pre = SER.deserialize(tag.prefix());
                if (!pre.equals(team.prefix())) team.prefix(pre);
                if (!team.hasEntry(name)) team.addEntry(name);
            }
        }
    }

    private static void removeFrom(Scoreboard sb, String name) {
        Team t = sb.getEntryTeam(name);
        if (t != null && t.getName().startsWith(TEAM_PREFIX)) t.removeEntry(name);
    }
}
