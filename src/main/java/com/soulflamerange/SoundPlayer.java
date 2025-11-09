package com.soulflamerange;

import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SoundPlayer
{
	private Clip currentClip;
	private Thread playbackThread;

	static
	{
		// Try to initialize MP3 SPI provider
		try
		{
			// Check if MP3 is supported
			AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
			// log.info("Supported audio file types: {}", java.util.Arrays.toString(types));
		}
		catch (Exception e)
		{
			// log.warn("Could not check audio file types: {}", e.getMessage());
		}
	}

	/**
	 * Play an MP3 file from plugin resources asynchronously
	 * @param resourcePath Path to the resource file (e.g., "/surprise.mp3")
	 * @return true if playback started successfully
	 */
	boolean playMP3FromResource(String resourcePath)
	{
		if (resourcePath == null || resourcePath.isEmpty())
		{
			// log.warn("Sound resource path is empty");
			return false;
		}

		// Stop any currently playing sound
		stop();

		try
		{
			// Load from classpath resources
			// log.info("Attempting to load sound resource: {}", resourcePath);
			InputStream audioStream = SoundPlayer.class.getResourceAsStream(resourcePath);
			if (audioStream == null)
			{
				// log.error("Sound resource not found: {}. Make sure the file is in src/main/resources/", resourcePath);
				// Try alternative paths
				String[] alternativePaths = {
					"/wedhorn.mp3",
					"/wedhorn.wav",
					resourcePath.replace("/com/soulflamerange/", "/")
				};
				for (String altPath : alternativePaths)
				{
					// log.info("Trying alternative path: {}", altPath);
					audioStream = SoundPlayer.class.getResourceAsStream(altPath);
					if (audioStream != null)
					{
						// log.info("Found sound at alternative path: {}", altPath);
						resourcePath = altPath;
						break;
					}
				}
				if (audioStream == null)
				{
					// log.error("Could not find sound file in any location");
					return false;
				}
			}
			// else
			// {
			// 	log.info("Successfully loaded sound resource stream");
			// }

			// log.info("Creating AudioInputStream...");
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioStream);
			if (audioInputStream == null)
			{
				// log.error("Failed to create AudioInputStream - format may not be supported");
				return false;
			}
			
			AudioFormat format = audioInputStream.getFormat();
			// log.info("Audio format: {}, channels: {}, sample rate: {}, encoding: {}", 
			// 	format, format.getChannels(), format.getSampleRate(), format.getEncoding());

			// If the format is not PCM (e.g., MP3), convert it to PCM
			AudioFormat targetFormat = null;
			if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && 
			    format.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED)
			{
				// log.info("Converting audio format from {} to PCM...", format.getEncoding());
				// Convert to PCM format that Java can play
				targetFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					format.getSampleRate(),
					16, // 16-bit samples
					format.getChannels(),
					format.getChannels() * 2, // frame size for 16-bit
					format.getSampleRate(),
					false // little-endian
				);
				
				// Convert the stream to PCM
				if (AudioSystem.isConversionSupported(targetFormat, format))
				{
					audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
					format = targetFormat;
					// log.info("Converted to PCM format: {}", format);
				}
				else
				{
					// log.error("Cannot convert format {} to PCM", format.getEncoding());
					return false;
				}
			}

			// log.info("Getting Clip...");
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			if (!AudioSystem.isLineSupported(info))
			{
				// log.error("Line not supported for format: {}", format);
				return false;
			}
			
			currentClip = (Clip) AudioSystem.getLine(info);
			// log.info("Opening clip...");
			currentClip.open(audioInputStream);
			// log.info("Clip opened successfully");

			// Play in a separate thread to avoid blocking
			playbackThread = new Thread(() -> {
				try
				{
					currentClip.start();
					// Wait for playback to finish
					currentClip.drain();
				}
				catch (Exception e)
				{
					// log.error("Error playing sound file: {}", e.getMessage(), e);
				}
			});

			playbackThread.start();
			// log.info("Started playing sound from resource: {}", resourcePath);
			return true;
		}
		catch (Exception e)
		{
			// log.error("Failed to play sound resource {}: {}", resourcePath, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Stop any currently playing sound
	 */
	void stop()
	{
		if (currentClip != null)
		{
			try
			{
				if (currentClip.isRunning())
				{
					currentClip.stop();
				}
				currentClip.close();
			}
			catch (Exception e)
			{
				// log.debug("Error stopping sound: {}", e.getMessage());
			}
			currentClip = null;
		}

		if (playbackThread != null && playbackThread.isAlive())
		{
			playbackThread.interrupt();
			playbackThread = null;
		}
	}

	/**
	 * Check if a sound is currently playing
	 */
	boolean isPlaying()
	{
		return currentClip != null && currentClip.isRunning();
	}
}
