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
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RailLoader extends JavaPlugin implements Listener
{
    private final Multimap<Minecart, Chunk> chunks;
    
    public RailLoader()
    {
        HashMultimap<Minecart, Chunk> tmp = HashMultimap.create();
        this.chunks = (Multimap<Minecart, Chunk>)tmp;
    }
    
    @Override
    public void onEnable()
    {
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
    }
    
    @EventHandler
    public void onVehicleMove(final VehicleMoveEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart))
        {
            return;
        }
        final Minecart minecart = (Minecart)event.getVehicle();
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (!from.getChunk().equals(to.getChunk())) {
            this.ensureChunksLoaded(minecart, 2);
        }
    }
    
    @EventHandler
    public void onVehicleUpdate(final VehicleUpdateEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart))
        {
            return;
        }
        if (event.getVehicle().getVelocity().lengthSquared() <= 0.08)
        {
            this.removeFromChunkMap((Minecart)event.getVehicle());
        }
    }
    
    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart))
        {
            return;
        }
        this.removeFromChunkMap((Minecart)event.getVehicle());
    }
    
    private void removeFromChunkMap(final Minecart minecart)
    {
        if (this.chunks.containsKey(minecart))
        {
            for (final Chunk chunk : this.chunks.removeAll(minecart))
            {
                chunk.setForceLoaded(false);
            }
        }
    }
    
    private void ensureChunksLoaded(final Minecart minecart, final int radius)
    {
        final Set<Chunk> oldChunks = new HashSet<Chunk>();
        if (this.chunks.containsKey(minecart))
        {
            oldChunks.addAll(this.chunks.get(minecart));
        }
        final int x = minecart.getLocation().getChunk().getX();
        final int z = minecart.getLocation().getChunk().getZ();
        for (final Chunk chunk : oldChunks)
        {
            if (!this.chunks.containsValue(chunk))
            {
                chunk.setForceLoaded(false);
            }
        }
        for (int nx = x - radius; nx <= x + radius; ++nx)
        {
            for (int nz = z - radius; nz <= z + radius; ++nz)
            {
                final Chunk chunk = minecart.getWorld().getChunkAt(nx, nz);
                chunk.setForceLoaded(true);
                this.chunks.put(minecart, chunk);
            }
        }
    }
}
