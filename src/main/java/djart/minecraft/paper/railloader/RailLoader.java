package djart.minecraft.paper.railloader;

import java.util.Set;
import java.util.HashSet;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;
import com.google.common.collect.HashMultimap;
import org.bukkit.Chunk;
import org.bukkit.entity.Minecart;
import com.google.common.collect.Multimap;
import java.util.logging.Logger;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RailLoader extends JavaPlugin implements Listener
{
    private final Multimap<Minecart, Chunk> chunks;
    private final Logger logger;
    
    public RailLoader()
    {
        HashMultimap<Minecart, Chunk> tmp = HashMultimap.create();
        this.chunks = (Multimap<Minecart, Chunk>)tmp;
        logger = getLogger();
    }
    
    @Override
    public void onEnable()
    {
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
    }
    
    @EventHandler
    public void onVehicleMove(final VehicleMoveEvent event)
    {
        if (event.getVehicle() instanceof Minecart)
        {
            final Minecart minecart = (Minecart)event.getVehicle();
            final Location from = event.getFrom();
            final Location to = event.getTo();
            if (!from.getChunk().equals(to.getChunk()))
            {
                this.ensureChunksLoaded(minecart, 2);
            }
        }
    }
    
    @EventHandler
    public void onVehicleUpdate(final VehicleUpdateEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart))
        {
            return;
        }
        if (event.getVehicle().getVelocity().lengthSquared() <= 0.01)
        {
            Minecart minecart = (Minecart)event.getVehicle();
            if (this.chunks.containsKey(minecart))
            {
                for (final Chunk chunk : this.chunks.removeAll(minecart))
                {
                    if(removeChunkTicket(chunk))
                    {
                        //logger.warning("chunk " + chunk.getX() + " " + chunk.getZ() + " now without ticket because minecart is stopped");
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent event)
    {
        if (event.getVehicle() instanceof Minecart)
        {
            Minecart minecart = (Minecart)event.getVehicle();
            if (this.chunks.containsKey(minecart))
            {
                for (final Chunk chunk : this.chunks.removeAll(minecart))
                {
                    if(removeChunkTicket(chunk))
                    {
                        //logger.warning("chunk " + chunk.getX() + " " + chunk.getZ() + " now without ticket because minecart is destroed");
                    }
                }
            }
        }
    }
    
    private void ensureChunksLoaded(final Minecart minecart, final int radius)
    {
        final Set<Chunk> oldChunks = new HashSet<>();
        if (this.chunks.containsKey(minecart))
        {
            oldChunks.addAll(this.chunks.get(minecart));
        }
        chunks.removeAll(minecart);
        final int x = minecart.getLocation().getChunk().getX();
        final int z = minecart.getLocation().getChunk().getZ();
        final Set<Chunk> newChunks = new HashSet();
        for (int nx = x - radius; nx <= x + radius; ++nx)
        {
            for (int nz = z - radius; nz <= z + radius; ++nz)
            {
                newChunks.add(minecart.getWorld().getChunkAt(nx, nz));
            }
        }
        oldChunks.removeAll(newChunks);
        for (final Chunk chunk : oldChunks)
        {
            if(removeChunkTicket(chunk))
            {
                //logger.warning("chunk " + chunk.getX() + " " + chunk.getZ() + " now without ticket");
            }
        }
        for (final Chunk chunk : newChunks)
        {
            chunk.addPluginChunkTicket(this);
            //logger.warning("chunk " + chunk.getX() + " " + chunk.getZ() + " now with ticket");
            this.chunks.put(minecart, chunk);
        }
    }
    
    private boolean removeChunkTicket(Chunk chunk)
    {
        if (!chunks.values().contains(chunk))
        {
            chunk.removePluginChunkTicket(this);
            return true;
        }
        else
        {
            return false;
        }
    }
}
