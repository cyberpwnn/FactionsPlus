package org.cyberpwn.factionsplus;

import java.util.HashSet;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.phantomapi.lang.GList;
import org.phantomapi.world.W;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Role;

public class FA
{
	public static boolean isClaimed(Faction f)
	{
		return !f.isWilderness() && !f.isSafeZone() && !f.isWarZone();
	}
	
	public static boolean isClaimed(Location l)
	{
		return isClaimed(getFaction(l));
	}
	
	public static boolean isClaimed(Chunk c)
	{
		return isClaimed(getFaction(c));
	}
	
	public static Faction getFaction(Player p)
	{
		return getFPlayer(p).getFaction();
	}
	
	public static FPlayer getFPlayer(Player p)
	{
		return FPlayers.getInstance().getByPlayer(p);
	}
	
	public static Faction getFaction(Location l)
	{
		return Board.getInstance().getFactionAt(new FLocation(l));
	}
	
	public static Faction getFaction(Chunk c)
	{
		return getFaction(c.getBlock(0, 0, 0).getLocation());
	}
	
	public static Chunk getChunk(FLocation fchunk)
	{
		return fchunk.getWorld().getChunkAt((int) fchunk.getX(), (int) fchunk.getZ());
	}
	
	public static FLocation getFLocation(Chunk c)
	{
		return new FLocation(c.getWorld().getName(), c.getX(), c.getZ());
	}
	
	public static GList<Chunk> getClaims(Player p)
	{
		return getClaims(getFaction(p));
	}
	
	public static boolean isOwnFaction(Player p, Chunk c)
	{
		return isClaimed(c) && getFaction(c).equals(getFaction(p));
	}
	
	public static boolean isOwner(Player p, Faction f)
	{
		return f.getFPlayerAdmin().getName().equals(p.getName());
	}
	
	public static boolean isOwner(Player p)
	{
		return isOwner(p, getFaction(p));
	}
	
	public static boolean isModerator(Player p)
	{
		return getFPlayer(p).getRole().equals(Role.MODERATOR);
	}
	
	public static boolean isAdmin(Player p)
	{
		return getFPlayer(p).getRole().equals(Role.ADMIN);
	}
	
	public static boolean isStaffed(Player p)
	{
		return isAdmin(p) || isModerator(p);
	}
	
	public static boolean isWithFaction(Player p, Faction f)
	{
		return getPlayers(f).contains(p);
	}
	
	public static GList<Chunk> getClaims(Faction f)
	{
		GList<Chunk> cx = new GList<Chunk>();
		
		for(FLocation i : f.getAllClaims())
		{
			cx.add(getChunk(i));
		}
		
		return cx;
	}
	
	public static GList<Chunk> getClaims(Faction f, Location c, int rad)
	{
		GList<Chunk> cx = getClaims(f);
		GList<Chunk> cs = W.chunkRadius(c.getChunk(), rad);
		
		for(Chunk i : cx.copy())
		{
			if(!cs.contains(i))
			{
				cx.remove(i);
			}
		}
		
		return cx;
	}
	
	public static GList<Player> getPlayers(Faction f)
	{
		GList<Player> p = new GList<Player>();
		
		for(FPlayer i : f.getFPlayers())
		{
			if(i.isOnline())
			{
				p.add(i.getPlayer());
			}
		}
		
		return p;
	}
	
	public static void addOwnership(Player p, Chunk c)
	{
		if(isOwnFaction(p, c))
		{
			FLocation l = getFLocation(c);
			Faction f = getFaction(p);
			
			if(!f.getClaimOwnership().containsKey(l))
			{
				f.getClaimOwnership().put(l, new HashSet<String>());
			}
			
			f.getClaimOwnership().get(l).add(p.getUniqueId().toString());
		}
	}
	
	public static void removeOwnership(Player p, Chunk c)
	{
		if(isOwnFaction(p, c))
		{
			FLocation l = getFLocation(c);
			Faction f = getFaction(p);
			
			if(!f.getClaimOwnership().containsKey(l))
			{
				return;
			}
			
			f.getClaimOwnership().get(l).remove(p.getUniqueId().toString());
		}
	}
}
