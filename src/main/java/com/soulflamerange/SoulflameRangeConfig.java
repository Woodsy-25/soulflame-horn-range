package com.soulflamerange;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("soulflamerange")
public interface SoulflameRangeConfig extends Config
{
	@ConfigItem(
		keyName = "showRange",
		name = "Show Range",
		description = "Display the range text panel showing the range in tiles"
	)
	default boolean showRange()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRangeSquare",
		name = "Show Range Square",
		description = "Display the border/square outline on the game world showing the range"
	)
	default boolean showRangeSquare()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rangeColor",
		name = "Range Color",
		description = "Color of the range square"
	)
	default Color rangeColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "showOtherPlayers",
		name = "Show Other Players' Range",
		description = "Display range squares for other players who have the Soulflame horn equipped"
	)
	default boolean showOtherPlayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "otherPlayersColor",
		name = "Other Players' Range Color",
		description = "Color of the range square for other players"
	)
	default Color otherPlayersColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "otherPlayersRadius",
		name = "Other Players' Radius",
		description = "Radius (0-3) to display for other players' Soulflame horns"
	)
	default int otherPlayersRadius()
	{
		return 3;
	}
}

