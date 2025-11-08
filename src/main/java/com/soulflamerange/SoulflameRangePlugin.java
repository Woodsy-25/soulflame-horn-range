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
		log.info("Soulflame Range plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		log.debug("Soulflame Range plugin stopped!");
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
		
		if (containerId == 94)
		{
			logAllEquipmentSlots(container);
			checkEquippedHorns(container);
		}
		else
		{
			log.debug("Ignoring container {} - not equipment", containerId);
		}
	}

	private void logAllEquipmentSlots(ItemContainer equipment)
	{
		if (equipment == null)
		{
			log.debug("Equipment container is null");
			return;
		}

		log.info("=== Equipment Slots ===");
		for (int i = 0; i < equipment.size(); i++)
		{
			Item item = equipment.getItem(i);
			if (item != null)
			{
				log.info("Slot {}: Item ID {} (Quantity: {})", i, item.getId(), item.getQuantity());
			}
			else
			{
				log.debug("Slot {}: Empty", i);
			}
		}
		log.info("=======================");
	}

	private void checkEquippedHorns()
	{
		ItemContainer equipment = client.getItemContainer(93);
		checkEquippedHorns(equipment);
	}

	private void checkEquippedHorns(ItemContainer equipment)
	{
		if (equipment == null)
		{
			log.info("Equipment container is null - setting equipped to false");
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
				log.info("Found Soulflame horn in equipment slot {}", i);
				break;
			}
		}

		boolean wasEquipped = isSoulflameHornsEquipped;
		isSoulflameHornsEquipped = found;
		
		if (!found)
		{
			log.info("Soulflame horn NOT found in equipment - checked all {} slots", equipment.size());
		}
		
		if (wasEquipped != isSoulflameHornsEquipped)
		{
			log.info("Soulflame horns equipped status changed: {} -> {} (slot: {})", wasEquipped, isSoulflameHornsEquipped, foundSlot);
			if (isSoulflameHornsEquipped)
			{
				updateRange();
			}
			else
			{
				currentRange = 0;
			}
		}
		else if (isSoulflameHornsEquipped)
		{
			// Update range even if already equipped (in case config changed)
			updateRange();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// If horn is equipped, check if this varbit change affects the radius
		if (isSoulflameHornsEquipped)
		{
			int varbitId = event.getVarbitId();
			int configuredVarbitId = config.varbitId();
			
			// Log all varbit changes when horn is equipped to help find the right one
			if (configuredVarbitId == 0)
			{
				int value = client.getVarbitValue(varbitId);
				if (value >= 0 && value <= 3)
				{
					log.info("Varbit {} changed to {} (possible radius value?)", varbitId, value);
				}
			}
			
			// If this is the configured varbit, update the range
			if (configuredVarbitId > 0 && varbitId == configuredVarbitId)
			{
				log.info("Configured radius varbit {} changed, updating range", varbitId);
				updateRange();
			}
		}
	}

	private void updateRange()
	{
		// The Soulflame horn stores its radius (0-3) in a varbit
		// First try the manually configured varbit ID
		int configuredVarbitId = config.varbitId();
		
		log.info("Updating range - configured varbit ID: {}", configuredVarbitId);
		
		if (configuredVarbitId > 0)
		{
			try
			{
				int radiusValue = client.getVarbitValue(configuredVarbitId);
				log.info("Read varbit {} value: {}", configuredVarbitId, radiusValue);
				
				if (radiusValue >= 0 && radiusValue <= 3)
				{
					currentRange = radiusValue;
					log.info("Successfully read horn radius from configured varbit {}: {}", configuredVarbitId, currentRange);
					return;
				}
				else
				{
					log.warn("Configured varbit {} returned invalid value: {} (expected 0-3)", configuredVarbitId, radiusValue);
				}
			}
			catch (Exception e)
			{
				log.warn("Error reading configured varbit {}: {}", configuredVarbitId, e.getMessage());
			}
		}
		else
		{
			log.info("No varbit ID configured. Please set 'Radius Varbit ID' in plugin settings.");
			log.info("To find it: 1) Use a varbit inspector plugin, 2) Change horn radius in-game, 3) Note which varbit changes to 0-3");
		}
		
		// Try common varbit IDs if no manual config
		// Common ranges for item configs: 8000-9000, 10000-11000
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
					log.info("Found horn radius from varbit {}: {}", varbitId, currentRange);
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
			log.warn("Could not detect horn radius. Current range set to: {}. Configure the 'Radius Varbit ID' in plugin settings.", currentRange);
		}
	}

	public boolean isSoulflameHornsEquipped()
	{
		return isSoulflameHornsEquipped;
	}

	public int getRange()
	{
		if (!isSoulflameHornsEquipped)
		{
			return 0;
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
