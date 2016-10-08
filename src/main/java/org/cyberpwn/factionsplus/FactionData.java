package org.cyberpwn.factionsplus;

import org.phantomapi.clust.ColdLoad;
import org.phantomapi.clust.Configurable;
import org.phantomapi.clust.DataCluster;
import org.phantomapi.clust.HandledConfig;
import org.phantomapi.lang.GChunk;
import org.phantomapi.lang.GSet;

@ColdLoad
@HandledConfig
public class FactionData implements Configurable
{
	private String identifier;
	private DataCluster cc;
	private Long totalValue;
	private Long totalChestValue;
	private Long totalSpawnerValue;
	private Long totalChests;
	private Long totalSpawners;
	
	public FactionData(String identifier)
	{
		this.identifier = identifier;
		this.cc = new DataCluster();
		
		totalValue = 0l;
		totalChestValue = 0l;
		totalSpawnerValue = 0l;
		totalChests = 0l;
		totalSpawners = 0l;
	}
	
	public boolean equals(Object o)
	{
		if(o != null && o instanceof FactionData)
		{
			FactionData fd = (FactionData) o;
			
			if(fd.getCodeName().equals(getCodeName()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onNewConfig()
	{
		
	}
	
	@Override
	public void onReadConfig()
	{
		
	}
	
	@Override
	public DataCluster getConfiguration()
	{
		return cc;
	}
	
	@Override
	public String getCodeName()
	{
		return identifier;
	}
	
	public void updateValues()
	{
		try
		{
			totalValue = 0l;
			totalChestValue = 0l;
			totalSpawnerValue = 0l;
			totalChests = 0l;
			totalSpawners = 0l;
			
			GSet<String> kx = new GSet<String>();
			
			for(String i : cc.keys())
			{
				kx.add(i.split("\\.")[0]);
			}
			
			for(String i : kx)
			{
				String key = i;
				GChunk c = new GChunk(Integer.valueOf(key.split(",")[0]), Integer.valueOf(key.split(",")[1]), key.split(",")[2]);
				totalValue += getValue(c);
				totalChestValue += getChestValue(c);
				totalSpawnerValue += getSpawnerValue(c);
				totalChests += getChestCount(c);
				totalSpawners += getSpawnerCount(c);
			}
		}
		
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public Long getValue(GChunk c)
	{
		return cc.getLong(chunkKey(c) + ".v");
	}
	
	public Long getChestValue(GChunk c)
	{
		return cc.getLong(chunkKey(c) + ".cv");
	}
	
	public Long getSpawnerValue(GChunk c)
	{
		return cc.getLong(chunkKey(c) + ".sv");
	}
	
	public Long getChestCount(GChunk c)
	{
		return cc.getLong(chunkKey(c) + ".c");
	}
	
	public Long getSpawnerCount(GChunk c)
	{
		return cc.getLong(chunkKey(c) + ".s");
	}
	
	public void setValue(GChunk c, Long v)
	{
		cc.set(chunkKey(c) + ".v", v);
	}
	
	public void setChestValue(GChunk c, Long v)
	{
		cc.set(chunkKey(c) + ".cv", v);
	}
	
	public void setSpawnerValue(GChunk c, Long v)
	{
		cc.set(chunkKey(c) + ".sv", v);
	}
	
	public void setChestCount(GChunk c, Long v)
	{
		cc.set(chunkKey(c) + ".c", v);
	}
	
	public void setSpawnerCount(GChunk c, Long v)
	{
		cc.set(chunkKey(c) + ".s", v);
	}
	
	public String chunkKey(GChunk c)
	{
		return c.getX().toString() + "," + c.getZ().toString() + "," + c.getWorld();
	}
	
	public boolean hasChunk(GChunk c)
	{
		return cc.contains(chunkKey(c) + ".v");
	}
	
	public Long getTotalValue()
	{
		return totalValue;
	}
	
	public Long getTotalChestValue()
	{
		return totalChestValue;
	}
	
	public Long getTotalSpawnerValue()
	{
		return totalSpawnerValue;
	}
	
	public Long getTotalChests()
	{
		return totalChests;
	}
	
	public Long getTotalSpawners()
	{
		return totalSpawners;
	}
}
