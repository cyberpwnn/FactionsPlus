package org.cyberpwn.factionsplus;

import org.phantomapi.clust.DataController;
import org.phantomapi.construct.Controllable;
import org.phantomapi.statistics.Monitorable;
import org.phantomapi.util.F;
import com.massivecraft.factions.Faction;

public class FactionDataHandler extends DataController<FactionData, String> implements Monitorable
{
	public FactionDataHandler(Controllable parentController)
	{
		super(parentController);
	}
	
	@Override
	public FactionData onLoad(String identifier)
	{
		FactionData fd = new FactionData(identifier);
		
		return fd;
	}
	
	public void remove(Faction f)
	{
		cache.remove(f.getTag());
	}
	
	@Override
	public void onSave(String identifier)
	{
		
	}
	
	@Override
	public void onStart()
	{
		
	}
	
	@Override
	public void onStop()
	{

	}
	
	@Override
	public String getMonitorableData()
	{
		return F.pc(1.0);
	}
}
