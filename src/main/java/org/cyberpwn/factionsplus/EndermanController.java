package org.cyberpwn.factionsplus;

import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;

public class EndermanController extends Controller
{
	public EndermanController(Controllable parentController)
	{
		super(parentController);
	}
	
	@EventHandler
	public void on(EntityDamageEvent e)
	{
		try
		{
			if(e.getCause().equals(DamageCause.FALL) && e.getEntityType().equals(EntityType.ENDERMAN) && e.getDamage() >= 20.0)
			{
				((Enderman) e.getEntity()).setHealth(0);
			}
		}
		
		catch(Exception ex)
		{
			
		}
	}
	
	@Override
	public void onStart()
	{
		
	}
	
	@Override
	public void onStop()
	{
		
	}
}
