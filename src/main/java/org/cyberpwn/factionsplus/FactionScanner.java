package org.cyberpwn.factionsplus;

import java.io.File;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.phantomapi.Phantom;
import org.phantomapi.async.A;
import org.phantomapi.clust.Comment;
import org.phantomapi.clust.ConfigurableController;
import org.phantomapi.clust.Keyed;
import org.phantomapi.command.Command;
import org.phantomapi.command.CommandFilter;
import org.phantomapi.command.PhantomCommand;
import org.phantomapi.command.PhantomSender;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.ControllerMessage;
import org.phantomapi.construct.Ticked;
import org.phantomapi.lang.GChunk;
import org.phantomapi.lang.GList;
import org.phantomapi.lang.GLocation;
import org.phantomapi.lang.GMap;
import org.phantomapi.nest.Nest;
import org.phantomapi.statistics.Monitorable;
import org.phantomapi.sync.S;
import org.phantomapi.sync.Task;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.text.GText;
import org.phantomapi.text.Tabulator;
import org.phantomapi.util.C;
import org.phantomapi.util.F;
import org.phantomapi.util.RunVal;
import org.phantomapi.world.MaterialBlock;
import org.phantomapi.world.W;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.event.FactionRenameEvent;
import de.dustplanet.util.SilkUtil;
import net.milkbowl.vault.economy.Economy;

@Ticked(0)
public class FactionScanner extends ConfigurableController implements Monitorable
{
	private SilkUtil su;
	private Economy econ;
	private Boolean loaded;
	private GMap<String, Double> shopCache;
	private GList<GChunk> queue;
	private GMap<GChunk, ProcessedChunk> activeQueue;
	private FactionDataHandler factionDataHandler;
	private Tabulator<String> topulator;
	private Integer overdriveLast;
	private Integer lastTake;
	private Integer lastAdd;
	private Integer rebuildTick;
	private Boolean od;
	
	@Comment("The shop to read value data")
	@Keyed("worker.shop")
	public String shop = "shop";
	
	@Comment("The size of a tab")
	@Keyed("worker.top-tab-size")
	public int tabSize = 10;
	
	@Comment("Max worker threads.\nI KNOW WHAT YOU ARE THINKING SWIFT\nKEEP THIS LOW")
	@Keyed("worker.max-threads")
	public int maxThreads = 2;
	
	@Comment("The minimum amount of ticks an overdrive can initiate.")
	@Keyed("worker.overdrive.overdrive-cooldown")
	public int overdriveCooldown = 17;
	
	@Comment("The maximum overdrive duration")
	@Keyed("worker.overdrive.maximum-overdrive-cycle")
	public int overdriveDuration = 5;
	
	@Comment("Overdrive is kicked on when the following is met\nQUEUE > (MULT * MAX_OVERDRIVE_THREADS)\nSINCE_LAST_OVERDRIVE > OVERDRIVE_COOLDOWN\nSINCE_LAST_OVERDRIVE < OVERDRIVE_DURATION")
	@Keyed("worker.overdrive.overdrive-multiplier")
	public double overdriveMultiplier = 2.367;
	
	@Comment("The maximum worker threads while overdrive is active.")
	@Keyed("worker.overdrive.max-overdrive-threads")
	public int maxOverdriveThreads = 8;
	
	@Keyed("worker.override")
	@Comment("Override id's with this.\nID:META=VALUE\nID is required as id or name\nMETA is not required\nVALUE is required NO DECIMALS")
	public GList<String> override = new GList<String>().qadd("1:0=0");
	
	@Keyed("worker.ignore")
	public GList<String> ignored = new GList<String>().qadd("STONE").qadd("STONE:1").qadd("STONE:2").qadd("STONE:3").qadd("STONE:4").qadd("STONE:5").qadd("STONE:6");
	
	public FactionScanner(Controllable parentController)
	{
		super(parentController, "config");
		
		shopCache = new GMap<String, Double>();
		loaded = false;
		queue = new GList<GChunk>();
		activeQueue = new GMap<GChunk, ProcessedChunk>();
		factionDataHandler = new FactionDataHandler(this);
		lastTake = 0;
		lastAdd = 0;
		rebuildTick = 10;
		overdriveLast = overdriveCooldown;
		od = false;
		
		register(factionDataHandler);
	}
	
	@EventHandler
	public void on(FactionRenameEvent e)
	{
		clearFaction(e.getFaction());
	}
	
	public void clearFaction(Faction f)
	{
		factionDataHandler.remove(f);
	}
	
	@Override
	public void onStart()
	{
		loadCluster(this);
		
		su = SilkUtil.hookIntoSilkSpanwers();
		
		setupEconomy();
		
		new Task(20)
		{
			@Override
			public void run()
			{
				ControllerMessage message = new ControllerMessage(FactionScanner.this);
				message.set("check-load", shop);
				ControllerMessage response = sendMessage("CurrencyShops", message);
				
				if(response.contains("result") && response.getBoolean("result"))
				{
					v("CurrencyShop <" + shop + ">  Loaded, Starting sampler");
					cancel();
					preCache();
				}
			}
		};
	}
	
	@Override
	public void onReadConfig()
	{
		topulator = new Tabulator<String>(tabSize);
		
		for(String i : ignored)
		{
			try
			{
				MaterialBlock mb = W.getMaterialBlock(i);
				mb.getMaterial();
			}
			
			catch(Exception e)
			{
				f("INVALID ITEM: " + i);
			}
		}
		
		for(String i : override)
		{
			try
			{
				String id = i.split("=")[0];
				String val = i.split("=")[1];
				MaterialBlock mb = W.getMaterialBlock(id);
				Integer am = Integer.valueOf(val);
				s("OVERRIDE: " + mb.getMaterial() + ":" + mb.getData() + " > " + am);
				shopCache.put(mb.getMaterial() + ":" + mb.getData(), am.doubleValue());
			}
			
			catch(Exception e)
			{
				f("Invalid: " + i);
			}
		}
	}
	
	@Override
	public void onStop()
	{
		
	}
	
	@Override
	public void onTick()
	{
		lastTake = 0;
		lastAdd = 0;
		overdriveLast++;
		rebuildTick--;
		
		if(queue.size() > overdriveMultiplier * maxOverdriveThreads && overdriveLast > overdriveCooldown)
		{
			od = true;
			overdriveLast = 0;
		}
		
		while(loaded && activeQueue.size() < (od ? maxOverdriveThreads : maxThreads) && !queue.isEmpty())
		{
			GChunk next = queue.pop();
			lastTake++;
			
			dispatchThread(next, new ProcessedChunk()
			{
				@Override
				public void run()
				{
					Faction f = Board.getInstance().getFactionAt(new FLocation(next.toChunk().getBlock(0, 0, 0).getLocation()));
					FactionData fd = factionDataHandler.get(f.getTag());
					fd.setValue(next, getValue());
					fd.setChestValue(next, getChestValue());
					fd.setSpawnerValue(next, getSpawnerValue());
					fd.setChestCount(next, getChests());
					fd.setSpawnerCount(next, getSpawners());
					fd.updateValues();
					factionDataHandler.getCache().put(f.getTag(), fd);
				}
			});
		}
		
		if(overdriveLast > overdriveDuration)
		{
			od = false;
		}
		
		if(rebuildTick <= 0)
		{
			rebuildTick = 20;
			
			GList<String> faqs = factionDataHandler.getCache().k();
			
			for(Faction i : Factions.getInstance().getAllFactions())
			{
				faqs.remove(i.getTag());
			}
			
			for(String i : faqs)
			{
				factionDataHandler.getCache().remove(i);
			}
			
			rebuildTabs();
		}
	}
	
	public void displayTop(Player p)
	{
		displayTop(p, 1);
	}
	
	public void displayTop(Player p, int tab)
	{
		if(tab <= topulator.getTabCount() - 1 && tab > 0)
		{
			p.sendMessage(C.AQUA + "Top Factions " + C.BOLD + C.WHITE + "(" + tab + "/" + (topulator.getTabCount() - 1) + ")");
			
			int k = 1 + (tab < 1 ? 1 : tab) * topulator.getTabSize() - 10;
			
			for(String i : topulator.getTab(tab - 1))
			{
				getText(i, k + ". ").tellRawTo(p);
				k++;
			}
		}
		
		else
		{
			p.sendMessage(C.RED + "/f top [1-" + (topulator.getTabCount() - 1) + "]");
		}
	}
	
	public GText getText(String faction, String ind)
	{
		GText gt = new GText();
		FactionData fd = factionDataHandler.get(faction);
		Faction f = null;
		
		for(Faction i : Factions.getInstance().getAllFactions())
		{
			if(i.getTag().equalsIgnoreCase(fd.getCodeName()))
			{
				f = i;
				break;
			}
		}
		
		if(f == null)
		{
			String h = C.AQUA + "Faction: " + C.WHITE + faction + "\n";
			h = h + C.AQUA + "Worth: " + C.GOLD + "$" + F.f(fd.getTotalValue()) + "\n";
			h = h + C.AQUA + "Spawners: " + C.GOLD + F.f(fd.getTotalSpawners()) + " ($" + F.f(fd.getTotalSpawnerValue()) + ")\n";
			h = h + C.AQUA + "Chests: " + C.GOLD + F.f(fd.getTotalChests()) + " ($" + F.f(fd.getTotalChestValue()) + ")\n";
			gt.addWithHoverSuggestCommand(C.YELLOW + ind + C.AQUA + faction + C.GOLD + " $" + F.f(factionDataHandler.get(faction).getTotalValue()), h, "/f who " + faction);
			
			return gt;
		}
		
		String h = C.AQUA + "Faction: " + C.WHITE + faction + "\n";
		h = h + C.AQUA + "Claims: " + C.GREEN + F.f(f.getAllClaims().size()) + "\n";
		h = h + C.AQUA + "Leader: " + C.GREEN + f.getFPlayerAdmin().getName() + "\n";
		h = h + C.AQUA + "Members: " + C.GREEN + F.f(f.getFPlayers().size()) + "\n";
		h = h + C.AQUA + "Worth: " + C.GOLD + "$" + F.f(fd.getTotalValue()) + "\n";
		h = h + C.AQUA + "Spawners: " + C.GOLD + F.f(fd.getTotalSpawners()) + " ($" + F.f(fd.getTotalSpawnerValue()) + ")\n";
		h = h + C.AQUA + "Chests: " + C.GOLD + F.f(fd.getTotalChests()) + " ($" + F.f(fd.getTotalChestValue()) + ")\n";
		
		gt.addWithHoverSuggestCommand(C.YELLOW + ind + C.AQUA + faction + C.GOLD + " $" + F.f(factionDataHandler.get(faction).getTotalValue()), h, "/f who " + faction);
		
		return gt;
	}
	
	public void rebuildTabs()
	{
		GMap<String, FactionData> cache = factionDataHandler.getCache().copy();
		GMap<String, Long> map = new GMap<String, Long>();
		GList<Long> order = new GList<Long>();
		Tabulator<String> newTabulator = new Tabulator<String>(tabSize);
		
		for(String i : cache.k())
		{
			map.put(i, cache.get(i).getTotalValue());
			order.add(cache.get(i).getTotalValue());
		}
		
		Collections.sort(order);
		Collections.reverse(order);
		
		for(Long i : order)
		{
			for(String j : map.k())
			{
				if(map.get(j).equals(i))
				{
					if(!newTabulator.contains(j))
					{
						newTabulator.add(j);
					}
				}
			}
		}
		
		topulator = newTabulator;
	}
	
	public void preCache()
	{
		File base = new File(getPlugin().getDataFolder(), "faction-data");
		
		if(!base.exists())
		{
			base.mkdirs();
		}
		
		new A()
		{
			@Override
			public void async()
			{
				s("Force Cache Loading");
				
				new S()
				{
					@Override
					public void sync()
					{
						loaded = true;
						
						forceCache();
						s("Force Cache Started");
					}
				};
			}
		};
	}
	
	public void dispatchThread(GChunk c, ProcessedChunk r)
	{
		activeQueue.put(c, r);
		
		GList<Material> mat = new GList<Material>();
		
		for(String i : ignored)
		{
			try
			{
				MaterialBlock mb = W.getMaterialBlock(i);
				mat.add(mb.getMaterial());
			}
			
			catch(Exception e)
			{
				f("INVALID ITEM: " + i);
			}
		}
		
		new A()
		{
			@Override
			public void async()
			{
				long[] value = new long[] {0};
				long[] chestValue = new long[] {0};
				long[] spawnerValue = new long[] {0};
				long[] chests = new long[] {0};
				long[] spawners = new long[] {0};
				
				GMap<GLocation, MaterialBlock> chunk = W.getChunkBlocksAsync(c);
				
				for(GLocation i : chunk.k())
				{
					MaterialBlock mb = chunk.get(i);
					
					if(mb.getMaterial().equals(Material.AIR) || mb.getMaterial().toString().contains("WATER") || mb.getMaterial().toString().contains("LAVA"))
					{
						continue;
					}
					
					if(mat.contains(mb.getMaterial()))
					{
						continue;
					}
					
					double val = getValue(mb);
					
					if(mb.getMaterial().equals(Material.MOB_SPAWNER))
					{
						val = getSpawnerPrice(i.toLocation().getBlock());
						spawners[0]++;
						spawnerValue[0] += val;
					}
					
					if(mb.getMaterial().equals(Material.CHEST) || mb.getMaterial().equals(Material.TRAPPED_CHEST))
					{
						val = getChestValue(i);
						chests[0]++;
						chestValue[0] += val;
					}
					
					value[0] += val;
				}
				
				new S()
				{
					@Override
					public void sync()
					{
						new TaskLater(0)
						{
							@Override
							public void run()
							{
								r.run(value[0], chestValue[0], spawnerValue[0], chests[0], spawners[0]);
								activeQueue.remove(c);
							}
						};
					}
				};
			}
		};
	}
	
	public void queue(GChunk c)
	{
		if(queue.contains(c) || !partOfFaction(c))
		{
			return;
		}
		
		queue.add(c);
		lastAdd++;
	}
	
	public void queue(Chunk c)
	{
		queue(new GChunk(c));
	}
	
	public void queue(Location l)
	{
		queue(l.getChunk());
	}
	
	public void queue(Faction f)
	{
		for(FLocation i : f.getAllClaims())
		{
			GChunk gc = new GChunk(i.getWorld().getChunkAt((int) i.getX(), (int) i.getZ()));
			queue(gc);
		}
	}
	
	private boolean setupEconomy()
	{
		if(getPlugin().getServer().getPluginManager().getPlugin("Vault") == null)
		{
			return false;
		}
		
		RegisteredServiceProvider<Economy> rsp = getPlugin().getServer().getServicesManager().getRegistration(Economy.class);
		
		if(rsp == null)
		{
			return false;
		}
		
		econ = rsp.getProvider();
		
		return econ != null;
	}
	
	@Override
	public String getMonitorableData()
	{
		if(!loaded)
		{
			File base = new File(getPlugin().getDataFolder(), "faction-data");
			
			if(!base.exists())
			{
				base.mkdirs();
			}
			
			return "Loading: " + C.LIGHT_PURPLE + factionDataHandler.getMonitorableData();
		}
		
		else
		{
			C x = od ? C.LIGHT_PURPLE : C.DARK_PURPLE;
			C y = od ? C.AQUA : C.DARK_AQUA;
			
			if(activeQueue.isEmpty())
			{
				x = C.DARK_GRAY;
				y = C.DARK_GRAY;
			}
			
			String s = x + "" + activeQueue.size() + " Threads, " + y + F.f(queue.size() + activeQueue.size()) + " Queued" + (lastAdd > 0 ? C.GREEN : C.DARK_GRAY) + " +" + lastAdd + " " + (lastTake > 0 ? C.RED : C.DARK_GRAY) + " -" + lastTake;
			
			return s;
		}
	}
	
	public Double getChestValue(GLocation l)
	{
		double value = Phantom.sync(new RunVal<Double>()
		{
			@SuppressWarnings("deprecation")
			@Override
			public void run(Double d)
			{
				double vv = 0.0;
				
				try
				{
					for(ItemStack i : ((Chest) l.toLocation().getBlock().getState()).getBlockInventory().getContents())
					{
						try
						{
							if(i.getType().equals(Material.MOB_SPAWNER))
							{
								vv += i.getAmount() * getSpawnerPrice(i);
							}
							
							else
							{
								vv += i.getAmount() * getValue(new MaterialBlock(i.getType(), i.getData().getData()));
							}
						}
						
						catch(Exception e)
						{
							
						}
					}
				}
				
				catch(Exception e)
				{
					
				}
				
				value = vv;
			}
		});
		
		return value;
	}
	
	public double getValue(MaterialBlock mb)
	{
		if(!shopCache.containsKey(mb.getMaterial() + ":" + mb.getData()))
		{
			double value = Phantom.sync(new RunVal<Double>()
			{
				@Override
				public void run(Double d)
				{
					try
					{
						ControllerMessage message = new ControllerMessage(FactionScanner.this);
						message.set("value", mb.getMaterial() + ":" + mb.getData());
						message.set("shop", shop);
						
						ControllerMessage response = sendMessage("CurrencyShops", message);
						
						if(response.contains("result"))
						{
							value = response.getDouble("result");
						}
					}
					
					catch(Exception e)
					{
						
					}
					
					Double vv = 0.0;
					Plugin cs = Bukkit.getServer().getPluginManager().getPlugin("CurrencyShops");
					
					try
					{
						vv = (Double) cs.getClass().getMethod("value", String.class, MaterialBlock.class).invoke(cs, shop, mb);
					}
					
					catch(Exception e)
					{
						
					}
					
					value = vv;
				}
			});
			
			shopCache.put(mb.getMaterial() + ":" + mb.getData(), value);
		}
		
		return shopCache.get(mb.getMaterial() + ":" + mb.getData());
	}
	
	public double getSpawnerPrice(Block b)
	{
		return Phantom.sync(new RunVal<Double>()
		{
			@Override
			public void run(Double d)
			{
				Block bx = new Location(Bukkit.getWorld(b.getWorld().getName()), b.getX(), b.getY(), b.getZ()).getBlock();
				MaterialBlock mb = new MaterialBlock(Material.MOB_SPAWNER, (byte) su.getSpawnerEntityID(bx));
				double cx = 0;
				
				if(Nest.getBlock(bx).contains("t.s.v"))
				{
					cx = getValue(mb) * Nest.getBlock(bx).getDouble("t.s.v");
				}
				
				value = getValue(mb) + cx;
			}
		});
	}
	
	public double getSpawnerPrice(ItemStack is)
	{
		return getValue(new MaterialBlock(Material.MOB_SPAWNER, (byte) su.getStoredSpawnerItemEntityID(is)));
	}
	
	public boolean partOfFaction(GChunk c)
	{
		Faction f = Board.getInstance().getFactionAt(new FLocation(c.toChunk().getBlock(0, 0, 0).getLocation()));
		
		if(!f.isWilderness() && !f.isWarZone() && !f.isSafeZone())
		{
			return true;
		}
		
		return false;
	}
	
	@EventHandler
	public void on(BlockPlaceEvent e)
	{
		queue(e.getBlock().getLocation());
	}
	
	@EventHandler
	public void on(BlockBreakEvent e)
	{
		queue(e.getBlock().getLocation());
	}
	
	@EventHandler
	public void on(InventoryCloseEvent e)
	{
		if(e.getInventory().getType().equals(InventoryType.CHEST))
		{
			for(HumanEntity i : e.getViewers())
			{
				queue(i.getLocation());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void on(PlayerCommandPreprocessEvent e)
	{
		if(e.getMessage().toLowerCase().equalsIgnoreCase("/f top"))
		{
			displayTop(e.getPlayer());
			e.setCancelled(true);
			return;
		}
		
		if(e.getMessage().toLowerCase().startsWith("/f top "))
		{
			try
			{
				int tab = Integer.valueOf(e.getMessage().split(" ")[2]);
				displayTop(e.getPlayer(), tab);
				e.setCancelled(true);
			}
			
			catch(Exception ex)
			{
				e.getPlayer().sendMessage(C.RED + "Invalid tab.");
				e.setCancelled(true);
			}
		}
	}
	
	@CommandFilter.Permission("fplus.god")
	@CommandFilter.PlayerOnly
	@Command("f+")
	public boolean onForceCache(PhantomSender sender, PhantomCommand cmd)
	{
		if(cmd.getArgs()[0].equalsIgnoreCase("forcecache"))
		{
			sender.sendMessage(C.RED + "Force Caching...");
			forceCache();
		}
		
		return true;
	}
	
	public void forceCache()
	{
		for(Faction i : Factions.getInstance().getAllFactions())
		{
			if(!i.isWilderness() && !i.isWarZone() && !i.isSafeZone())
			{
				queue(i);
			}
		}
	}
}
