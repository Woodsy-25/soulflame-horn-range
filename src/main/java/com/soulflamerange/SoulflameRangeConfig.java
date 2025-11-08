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
		description = "Display the range of Soulflame horns when equipped"
	)
	default boolean showRange()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRangeSquare",
		name = "Show Range Square",
		description = "Display a square on the game world showing the range"
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
		keyName = "varbitId",
		name = "Radius Varbit ID",
		description = "Varbit ID that stores the horn's radius (0-3). Use a varbit inspector to find this. Leave 0 to auto-detect."
	)
	default int varbitId()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showInEquipmentPanel",
		name = "Show in Equipment Panel",
		description = "Display range information in the equipment panel"
	)
	default boolean showInEquipmentPanel()
	{
		return true;
	}
}

