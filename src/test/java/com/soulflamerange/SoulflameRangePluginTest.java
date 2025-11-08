package com.soulflamerange;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

public class SoulflameRangePluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin((Class<? extends Plugin>) SoulflameRangePlugin.class);
		RuneLite.main(args);
	}
}