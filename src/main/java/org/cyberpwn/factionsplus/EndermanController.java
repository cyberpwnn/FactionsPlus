package org.cyberpwn.factionsplus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.cyberpwn.react.util.Cuboid;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;
import org.phantomapi.lang.GList;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.util.C;
import org.phantomapi.util.M;
import de.dustplanet.util.SilkUtil;

public class EndermanController extends Controller
{
	private SilkUtil u;
	
	public EndermanController(Controllable parentController)
	{
		super(parentController);
	}
	
	@Override
	public void onStart()
	{
		u = SilkUtil.hookIntoSilkSpanwers();
	}
	
	@Override
	public void onStop()
	{
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void on(ExplosionPrimeEvent e)
	{
		if(e.getEntityType().equals(EntityType.CREEPER))
		{
			Cuboid c = new Cuboid(e.getEntity().getLocation().add(3, 3, 3), e.getEntity().getLocation().add(-3, -3, -3));
			new TaskLater(2)
			{
				@Override
				public void run()
				{
					for(Block i : new GList<Block>(c.iterator()))
					{
						Location l = i.getLocation();
						
						if(l.getBlock().getType().equals(Material.MOB_SPAWNER) && M.r(0.45))
						{
							short k = u.getSpawnerEntityID(l.getBlock());
							l.getBlock().setType(Material.AIR);
							l.getWorld().dropItem(l, u.newSpawnerItem(k, C.YELLOW.toString() + u.getCreatureName(k) + C.WHITE.toString() + " Spawner"));
						}
					}
				}
			};
		}
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
}
