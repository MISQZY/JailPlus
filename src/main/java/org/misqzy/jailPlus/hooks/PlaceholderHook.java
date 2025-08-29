package org.misqzy.jailPlus.hooks;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;


public interface PlaceholderHook {


    @Nullable
    String onPlaceholderRequest(OfflinePlayer player, String params);

    String getHookPrefix();

    String getDescription();
}