package me.c10coding.events;

import me.c10coding.OneBlock;
import me.c10coding.files.AreaConfigManager;
import me.c10coding.files.PlayerAreaConfigManager;
import me.c10coding.files.PlayersConfigManager;
import me.c10coding.managers.OneBlockLogicManager;
import me.c10coding.managers.OneBlockManager;
import me.c10coding.phases.Phase;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class OneBlockListener implements Listener {

    private OneBlock plugin;
    private AreaConfigManager acm;
    private OneBlockLogicManager lm;

    public OneBlockListener(OneBlock plugin){
        this.plugin = plugin;
        this.acm = new AreaConfigManager(plugin);
        this.lm = new OneBlockLogicManager(plugin);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        if(!e.isCancelled()){
            Player p = e.getPlayer();
            Block b = e.getBlock();
            Location blockLocation = b.getLocation();
            //Refreshes the config for those that just got an area
            acm.reloadConfig();
            if(acm.hasArea(p.getUniqueId())){
                //If the block is within their own region
                if(lm.isInsideArea(b, p)){
                    if(!lm.isInfiniteBlock(b)){
                        PlayerAreaConfigManager pacm = new PlayerAreaConfigManager(plugin, "playerAreas/" + p.getUniqueId() + ".yml");
                        pacm.removeBlock(b);
                    }else{

                        Phase.Phases currentPhase = acm.getPhase(p);
                        Material oldBlockMat = e.getBlock().getType();
                        Material newBlockMat = lm.getRandomMaterial(currentPhase);
                        Location dropLocation = new Location(blockLocation.getWorld(), blockLocation.getX() + 0.5, blockLocation.getY() + 2, blockLocation.getZ() + 0.5);

                        /*
                        Cancels the breaking of the block, drops the itemstack in a more desirable place to prevent it from falling
                        Also sets the block to air to simulate the breaking of a block. Then it sets it to the new block
                         */

                        ItemStack droppedItem = new ItemStack(oldBlockMat);
                        List<Byte> log1Bytes = lm.getLog1ByteList();

                        List<Byte> log2Bytes = lm.getLog2ByteList();

                        /*
                        Sets the dropped item as the proper log
                         */
                        if(oldBlockMat.equals(Material.LOG) || oldBlockMat.equals(Material.LOG_2)) {
                            MaterialData mData = e.getBlock().getState().getData();
                            droppedItem = setDroppedItem(mData);
                        }

                        blockLocation.getBlock().setType(Material.AIR);
                        blockLocation.getBlock().setType(newBlockMat);
                        b.getWorld().dropItemNaturally(dropLocation, droppedItem);
                        e.setCancelled(true);

                        /*
                           Since there aren't different materials for the different logs, I adjust the data for the log here.
                           If the new material is something other than a log, it doesn't do anything
                         */
                        if(newBlockMat.equals(Material.LOG) || newBlockMat.equals(Material.LOG_2)){

                            Random rnd = new Random();
                            int randomNum;
                            Byte chosenLog;

                            if(newBlockMat.equals(Material.LOG)){
                                randomNum = rnd.nextInt(log1Bytes.size());
                                chosenLog = log1Bytes.get(randomNum);
                            }else{
                                randomNum = rnd.nextInt(log2Bytes.size());
                                chosenLog = log2Bytes.get(randomNum);
                            }

                            blockLocation.getBlock().setData(chosenLog);
                        }

                    }
                }else{
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        if(!e.isCancelled()){
            if(e.canBuild()){
                Player p = e.getPlayer();
                Block b = e.getBlock();
                //Refreshes the config for those that just got an area
                acm.reloadConfig();
                if(acm.hasArea(p.getUniqueId())){
                    //If the block is within their own region
                    if(lm.isInsideArea(b, p)){
                        PlayerAreaConfigManager pacm = new PlayerAreaConfigManager(plugin, "playerAreas/" + p.getUniqueId() + ".yml");
                        pacm.addBlock(b);
                    }else{
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSandFall(EntityChangeBlockEvent event){
        if(event.getEntityType()== EntityType.FALLING_BLOCK && event.getTo()== Material.AIR){
            String blockWorldName = event.getBlock().getWorld().getName();
            String pluginWorldName = plugin.getConfig().getString("World");
            if(pluginWorldName != null){
                if(blockWorldName.equalsIgnoreCase(pluginWorldName)){
                    if(event.getBlock().getType() == Material.SAND){
                        event.setCancelled(true);
                        //Update the block to fix a visual client bug, but don't apply physics
                        event.getBlock().getState().update(false, false);
                    }
                }
            }
        }
    }

    /*
    Validates the players UUID and name. This saves headaches down the road.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){

        PlayersConfigManager pc = new PlayersConfigManager(plugin);
        List<String> knownPlayersUUIDs = pc.getKnownPlayers();
        Player p = e.getPlayer();

        if(!knownPlayersUUIDs.contains(p.getUniqueId().toString())){
            pc.addPlayer(e.getPlayer().getUniqueId());
        }
    }

    public ItemStack setDroppedItem(MaterialData md){
        return md.getItemType().equals(Material.LOG) ? new ItemStack(Material.LOG, 1, md.getData()) : new ItemStack(Material.LOG_2, 1, md.getData());
    }

}
