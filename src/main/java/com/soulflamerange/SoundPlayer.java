package com.soulflamerange;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;

@Slf4j
class SoundPlayer
{
	private final AudioPlayer audioPlayer;

	SoundPlayer(AudioPlayer audioPlayer)
	{
		this.audioPlayer = audioPlayer;
	}

	/**
	 * Play a WAV file from plugin resources asynchronously
	 * @param resourcePath Path to the resource file (e.g., "/com/soulflamerange/webhorn.wav")
	 * @return true if playback started successfully
	 */
	boolean playWAVFromResource(String resourcePath)
	{
		if (resourcePath == null || resourcePath.isEmpty())
		{
			// log.warn("Sound resource path is empty");
			return false;
		}

		try
		{
			// Try the provided path first
			String[] pathsToTry = {
				resourcePath,
				"/webhorn.wav",
				"/com/soulflamerange/webhorn.wav",
				resourcePath.replace(".mp3", ".wav"),
				resourcePath.replace("/com/soulflamerange/", "/")
			};

			// Try each path until one works
			for (String path : pathsToTry)
			{
				try
				{
					// Use AudioPlayer to play from class resource
					// gain of 0.0f means no volume adjustment (0 dB)
					audioPlayer.play(SoundPlayer.class, path, 0.0f);
					// log.info("Started playing sound from resource: {}", path);
					return true;
				}
				catch (Exception e)
				{
					// Try next path
					// log.debug("Failed to play sound from path {}: {}", path, e.getMessage());
				}
			}

			// log.error("Could not find or play sound file in any location");
			return false;
		}
		catch (Exception e)
		{
			// log.error("Failed to play sound resource {}: {}", resourcePath, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Stop any currently playing sound
	 * Note: AudioPlayer doesn't provide a way to stop individual sounds,
	 * so this method is kept for API compatibility but doesn't do anything.
	 */
	void stop()
	{
		// AudioPlayer doesn't expose a way to stop individual sounds
		// This method is kept for API compatibility
	}

	/**
	 * Check if a sound is currently playing
	 * Note: AudioPlayer doesn't provide a way to check if sounds are playing,
	 * so this method always returns false.
	 */
	boolean isPlaying()
	{
		// AudioPlayer doesn't expose a way to check if sounds are playing
		return false;
	}
}
