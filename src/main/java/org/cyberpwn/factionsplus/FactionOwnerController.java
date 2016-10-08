package org.cyberpwn.factionsplus;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.phantomapi.clust.AsyncConfig;
import org.phantomapi.clust.Comment;
import org.phantomapi.clust.ConfigurableController;
import org.phantomapi.clust.Keyed;
import org.phantomapi.construct.Controllable;
import org.phantomapi.util.C;
import org.phantomapi.util.F;
import com.massivecraft.factions.Faction;

@AsyncConfig
public class FactionOwnerController extends ConfigurableController
{
	@Comment("The max radius of owner claims.\nThis is a chunk radius, f owner radis 4 means 9x9 chunks")
	@Keyed("limits.max-radius")
	public int maxRadius = 8;
	
	@Comment("Admins are always allowed, but what about mods?")
	@Keyed("limits.allow-moderators")
	public boolean allowMods = false;
	
	public FactionOwnerController(Controllable parentController)
	{
		super(parentController, "owner-radius");
	}

	@Override
	public void onStart()
	{
		loadCluster(this);
	}

	@Override
	public void onStop()
	{
		
	}
	
	@EventHandler
	public void on(PlayerCommandPreprocessEvent e)
	{
		if(e.getMessage().toLowerCase().startsWith("/f owner radius "))
		{
			try
			{
				int rad = Integer.valueOf(e.getMessage().split(" ")[e.getMessage().split(" ").length - 1]);
				
				if(rad > 0 && rad <= maxRadius)
				{
					Faction f = FA.getFaction(e.getPlayer());
					
					if(FA.isClaimed(f))
					{
						if(FA.isAdmin(e.getPlayer()) || (allowMods && FA.isModerator(e.getPlayer())))
						{
							int m = 0;
							
							for(Chunk i : FA.getClaims(FA.getFaction(e.getPlayer()), e.getPlayer().getLocation(), rad))
							{
								FA.addOwnership(e.getPlayer(), i);
								m++;
							}
							
							e.getPlayer().sendMessage(C.GREEN + "Added your ownership in " + F.f(m) + " claims.");
						}
						
						else
						{
							e.getPlayer().sendMessage(C.RED + "Must be an admin " + (allowMods ? "or moderator" : "") + "to use that.");
						}
					}
					
					else
					{
						e.getPlayer().sendMessage(C.RED + "Try actually claiming land :P");
					}
				}
				
				else
				{
					e.getPlayer().sendMessage(C.RED + "Invalid range (1 - " + maxRadius + ")");
				}
			}
			
			catch(Exception ex)
			{
				e.getPlayer().sendMessage(C.RED + "Invalid input. /f owner radius <NUMBER>");
			}
			
			e.setCancelled(true);
		}
		
		if(e.getMessage().toLowerCase().startsWith("/f unowner radius "))
		{
			try
			{
				int rad = Integer.valueOf(e.getMessage().split(" ")[e.getMessage().split(" ").length - 1]);
				
				if(rad > 0 && rad <= maxRadius)
				{
					Faction f = FA.getFaction(e.getPlayer());
					
					if(FA.isClaimed(f))
					{
						if(FA.isAdmin(e.getPlayer()) || (allowMods && FA.isModerator(e.getPlayer())))
						{
							int m = 0;
							
							for(Chunk i : FA.getClaims(FA.getFaction(e.getPlayer()), e.getPlayer().getLocation(), rad))
							{
								FA.removeOwnership(e.getPlayer(), i);
								m++;
							}
							
							e.getPlayer().sendMessage(C.GREEN + "Removed your ownership in " + F.f(m) + " claims.");
						}
						
						else
						{
							e.getPlayer().sendMessage(C.RED + "Must be an admin " + (allowMods ? "or moderator" : "") + "to use that.");
						}
					}
					
					else
					{
						e.getPlayer().sendMessage(C.RED + "Try actually claiming land :P");
					}
				}
				
				else
				{
					e.getPlayer().sendMessage(C.RED + "Invalid range (1 - " + maxRadius + ")");
				}
			}
			
			catch(Exception ex)
			{
				e.getPlayer().sendMessage(C.RED + "Invalid input. /f owner radius <NUMBER>");
			}
			
			e.setCancelled(true);
		}
	}
}
