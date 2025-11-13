package com.soulflamerange;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Soulflame Range"
)
public class SoulflameRangePlugin extends Plugin
{
	// Item IDs for Soulflame horns variants
	private static final int SOULFLAME_HORNS = 30759;

	@Inject
	private Client client;

	@Inject
	private SoulflameRangeConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SoulflameRangeOverlay overlay;

	private boolean isSoulflameHornsEquipped = false;
	private int currentRange = 0;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		// log.info("Soulflame Range plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		// log.debug("Soulflame Range plugin stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Check equipment when player logs in
			checkEquippedHorns();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer container = event.getItemContainer();
		if (container == null)
		{
			return;
		}

		int containerId = container.getId();
		// log.debug("ItemContainerChanged - Container ID: {} (94=Equipment, 93=Inventory)", containerId);
		
		// Check equipment container (94) to see if horn is equipped
		if (containerId == 94)
		{
			// logAllEquipmentSlots(container);
			checkEquippedHorns(container);
		}
		// else
		// {
		// 	log.debug("Ignoring container {} - not equipment", containerId);
		// }
	}

	// private void logAllEquipmentSlots(ItemContainer equipment)
	// {
	// 	if (equipment == null)
	// 	{
	// 		log.debug("Equipment container is null");
	// 		return;
	// 	}
	//
	// 	log.info("=== Equipment Slots ===");
	// 	for (int i = 0; i < equipment.size(); i++)
	// 	{
	// 		Item item = equipment.getItem(i);
	// 		if (item != null)
	// 		{
	// 			log.info("Slot {}: Item ID {} (Quantity: {})", i, item.getId(), item.getQuantity());
	// 		}
	// 		else
	// 		{
	// 			log.debug("Slot {}: Empty", i);
	// 		}
	// 	}
	// 	log.info("=======================");
	// }

	private void checkEquippedHorns()
	{
		ItemContainer equipment = client.getItemContainer(94);
		checkEquippedHorns(equipment);
	}

	private void checkEquippedHorns(ItemContainer equipment)
	{
		if (equipment == null)
		{
			// log.info("Equipment container is null - setting equipped to false");
			isSoulflameHornsEquipped = false;
			currentRange = 0;
			return;
		}

		// Check all equipment slots for the Soulflame horn
		// (It can be in different slots depending on how it's equipped)
		boolean found = false;
		int foundSlot = -1;
		for (int i = 0; i < equipment.size(); i++)
		{
			Item item = equipment.getItem(i);
			if (item != null && item.getId() == SOULFLAME_HORNS)
			{
				found = true;
				foundSlot = i;
				// log.info("Found Soulflame horn in equipment slot {}", i);
				break;
			}
		}

		boolean wasEquipped = isSoulflameHornsEquipped;
		isSoulflameHornsEquipped = found;
		
		// if (!found)
		// {
		// 	log.info("Soulflame horn NOT found in equipment - checked all {} slots", equipment.size());
		// }
		
		if (wasEquipped != isSoulflameHornsEquipped)
		{
			// log.info("Soulflame horns equipped status changed: {} -> {} (slot: {})", wasEquipped, isSoulflameHornsEquipped, foundSlot);
			if (isSoulflameHornsEquipped)
			{
				updateRange();
			}
			else
			{
				currentRange = 0;
				// log.info("Horn unequipped - range reset to 0");
			}
		}
		else if (isSoulflameHornsEquipped && found)
		{
			// Update range even if already equipped (in case config changed)
			updateRange();
		}
		else if (!found && isSoulflameHornsEquipped)
		{
			// Horn was equipped but now not found - reset
			// log.info("Horn no longer found in equipment - resetting");
			isSoulflameHornsEquipped = false;
			currentRange = 0;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// First verify the horn is actually equipped before updating range
		// Re-check equipment to ensure state is correct
		ItemContainer equipment = client.getItemContainer(94);
		boolean actuallyEquipped = false;
		
		if (equipment != null)
		{
			for (int i = 0; i < equipment.size(); i++)
			{
				Item item = equipment.getItem(i);
				if (item != null && item.getId() == SOULFLAME_HORNS)
				{
					actuallyEquipped = true;
					break;
				}
			}
		}
		
		// Only update if horn is actually equipped
		if (actuallyEquipped)
		{
			// Update equipped state if it was wrong
			if (!isSoulflameHornsEquipped)
			{
				isSoulflameHornsEquipped = true;
				// log.info("Horn detected as equipped via varbit change");
			}
			
			int varbitId = event.getVarbitId();
			
			// Skip invalid varbit IDs
			if (varbitId < 0)
			{
				return;
			}
			
			// Check if this is the known radius varbit (16264)
			// Auto-detect by checking if the varbit value is 0-3
			try
			{
				int value = client.getVarbitValue(varbitId);
				if (varbitId == 16264 || (value >= 0 && value <= 3))
				{
					// log.info("Radius varbit {} changed to {}, updating range", varbitId, value);
					updateRange();
				}
			}
			catch (Exception e)
			{
				// Varbit doesn't exist or can't be read, ignore it
				// log.debug("Could not read varbit {}: {}", varbitId, e.getMessage());
			}
		}
		else
		{
			// Horn not equipped - reset if it was marked as equipped
			if (isSoulflameHornsEquipped)
			{
				// log.info("Horn not found in equipment during varbit change - resetting");
				isSoulflameHornsEquipped = false;
				currentRange = 0;
			}
		}
	}

	private void updateRange()
	{
		// First verify the horn is actually equipped before reading the varbit
		ItemContainer equipment = client.getItemContainer(94);
		boolean actuallyEquipped = false;
		
		if (equipment != null)
		{
			for (int i = 0; i < equipment.size(); i++)
			{
				Item item = equipment.getItem(i);
				if (item != null && item.getId() == SOULFLAME_HORNS)
				{
					actuallyEquipped = true;
					break;
				}
			}
		}
		
		if (!actuallyEquipped)
		{
			// log.debug("Horn not equipped - not updating range");
			currentRange = 0;
			return;
		}
		
		// The Soulflame horn stores its radius (0-3) in varbit 16264
		int[] possibleVarbits = {
			16264, // Soulflame horn radius varbit
		};
		
		boolean found = false;
		for (int varbitId : possibleVarbits)
		{
			try
			{
				int radiusValue = client.getVarbitValue(varbitId);
				// Radius should be 0-3
				if (radiusValue >= 0 && radiusValue <= 3)
				{
					currentRange = radiusValue;
					// log.info("Found horn radius from varbit {}: {}", varbitId, currentRange);
					found = true;
					break;
				}
			}
			catch (Exception e)
			{
				// Continue to next varbit
			}
		}
		
		if (!found)
		{
			// Default to 0 if we can't detect it
			currentRange = 0;
			// log.warn("Could not detect horn radius. Current range set to: {}", currentRange);
		}
	}

	public boolean isSoulflameHornsEquipped()
	{
		return isSoulflameHornsEquipped;
	}

	public int getRange()
	{
		// Always verify the horn is actually equipped before returning range
		if (!isSoulflameHornsEquipped)
		{
			return 0;
		}
		
		// Double-check equipment to ensure state is correct
		ItemContainer equipment = client.getItemContainer(94);
		if (equipment != null)
		{
			boolean found = false;
			for (int i = 0; i < equipment.size(); i++)
			{
				Item item = equipment.getItem(i);
				if (item != null && item.getId() == SOULFLAME_HORNS)
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				// Horn not actually equipped - reset state
				// log.debug("Horn not found in equipment in getRange() - resetting");
				isSoulflameHornsEquipped = false;
				currentRange = 0;
				return 0;
			}
		}
		
		// Return the detected radius (0-3)
		return currentRange;
	}


	@Provides
	SoulflameRangeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SoulflameRangeConfig.class);
	}
}
