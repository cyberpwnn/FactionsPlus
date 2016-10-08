package org.cyberpwn.factionsplus;

public class ProcessedChunk implements Runnable
{
	private long value;
	private long chestValue;
	private long spawnerValue;
	private long chests;
	private long spawners;
	
	public void run(long value, long chestValue, long spawnerValue, long chests, long spawners)
	{
		this.value = value;
		this.chestValue = chestValue;
		this.spawnerValue = spawnerValue;
		this.chests = chests;
		this.spawners = spawners;
		
		run();
	}
	
	@Override
	public void run()
	{
		
	}

	public long getValue()
	{
		return value;
	}

	public long getChestValue()
	{
		return chestValue;
	}

	public long getSpawnerValue()
	{
		return spawnerValue;
	}

	public long getChests()
	{
		return chests;
	}

	public long getSpawners()
	{
		return spawners;
	}
}
