package cn.nukkit.level;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.network.protocol.LoginPacket;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.ByteStreams;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GlobalBlockPalette {
    private static final Int2IntMap legacyToRuntimeId = new Int2IntOpenHashMap();
    private static final Int2IntMap runtimeIdToLegacy = new Int2IntOpenHashMap();
    private static final AtomicInteger runtimeIdAllocator = new AtomicInteger(0);
    public static final byte[] BLOCK_PALETTE;

    static {
        legacyToRuntimeId.defaultReturnValue(-1);
        runtimeIdToLegacy.defaultReturnValue(-1);

        ListTag<CompoundTag> tag;
        try (InputStream stream = Server.class.getClassLoader().getResourceAsStream("runtime_block_states.dat")) {
            if (stream == null) {
                throw new AssertionError("Unable to locate block state nbt");
            }
            //noinspection unchecked
            tag = (ListTag<CompoundTag>) NBTIO.readTag(new ByteArrayInputStream(ByteStreams.toByteArray(stream)), ByteOrder.LITTLE_ENDIAN, false);
        } catch (IOException e) {
            throw new AssertionError("Unable to load block palette", e);
        }

        for (CompoundTag state : tag.getAll()) {
            int runtimeId = runtimeIdAllocator.getAndIncrement();
            if (!state.contains("LegacyStates")) continue;

            List<CompoundTag> legacyStates = state.getList("LegacyStates", CompoundTag.class).getAll();

            // Resolve to first legacy id
            CompoundTag firstState = legacyStates.get(0);
            runtimeIdToLegacy.put(runtimeId, firstState.getInt("id") << 6 | firstState.getShort("val"));

            for (CompoundTag legacyState : legacyStates) {
                int legacyId = legacyState.getInt("id") << 6 | legacyState.getShort("val");
                legacyToRuntimeId.put(legacyId, runtimeId);
            }
            state.remove("meta"); // No point in sending this since the client doesn't use it.
        }

        try {
            BLOCK_PALETTE = NBTIO.write(tag, ByteOrder.LITTLE_ENDIAN, true);
        } catch (IOException e) {
            throw new AssertionError("Unable to write block palette", e);
        }

        Map<Integer,Integer> realIdMap=new HashMap<>();
        //dropper and wooden_slab
        realIdMap.put(158,126);
        realIdMap.put(126,158);
        //activator_rail and double_wooden_slab
        realIdMap.put(125,157);
        realIdMap.put(157,125);

        Map<String,Integer> runtimeTempMap=new HashMap<>();
        JSONObject jsonObject=new JSONObject();
        int count=0;
        for (CompoundTag state : tag.getAll()) {
            String mcbeStringBlockName = state.getCompound("block").getString("name").split(":")[1];
            CompoundTag blockStates = state.getCompound("block").getCompound("states");
            if(blockStates.getAllTags().size()>0){
                ArrayList<String> runtimeArr=new ArrayList<>();
                for (Tag e : blockStates.getAllTags()) {
                    runtimeArr.add(e.getName());
                }
                Collections.sort(runtimeArr);
                mcbeStringBlockName+="[";
                for(String tagName:runtimeArr){
                    mcbeStringBlockName+=tagName;
                    mcbeStringBlockName+="=";
                    mcbeStringBlockName+=blockStates.get(tagName).parseValue();
                    mcbeStringBlockName+=",";
                }
                mcbeStringBlockName=mcbeStringBlockName.substring(0,mcbeStringBlockName.length()-1);
                mcbeStringBlockName+="]";
            }
            jsonObject.put(count+"",mcbeStringBlockName);
            runtimeTempMap.put(mcbeStringBlockName,count);
            count++;
        }

        JSONArray blocksJSON=new JSONArray();
        ArrayList<String> existsBlocksCache=new ArrayList<>();
        for(int i=0;i<=255;i++){
            int meta=0;
            int runtimeId;
            while ((runtimeId=legacyToRuntimeId.get(i << 6 | meta))!=-1){
                String name=jsonObject.getString(runtimeId+"");
                if(existsBlocksCache.contains(name)){
                    meta++;
                    continue;
                }
                existsBlocksCache.add(name);
                JSONObject obj=new JSONObject();
                Block block=Block.get(i,meta);
                obj.put("name",name);
                Integer realId=realIdMap.get(i);
                if(realId==null){
                    realId=i;
                }
                obj.put("id",realId);
                obj.put("meta",meta);
                if(block.getLightLevel()!=0) {
                    obj.put("light", block.getLightLevel());
                }
                meta++;
                blocksJSON.add(obj);
            }
        }
        writeFile("./blocks.json",blocksJSON.toJSONString());

        //grass_path and end_rod[facing_direction=0]
        jsonObject.put(runtimeTempMap.get("end_rod[facing_direction=0]")+"","grass_path");
        jsonObject.put(runtimeTempMap.get("grass_path")+"","end_rod[facing_direction=0]");
        writeFile("./block_runtime.json",jsonObject.toJSONString());
    }

    public static void writeFile(String path,String text) {
        try {
            Writer writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
            writer.write(text);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static int getOrCreateRuntimeId(int id, int meta) {
        int legacyId = id << 6 | meta;
        int runtimeId = legacyToRuntimeId.get(legacyId);
        if (runtimeId == -1) {
            runtimeId = legacyToRuntimeId.get(id << 6);
            if (runtimeId == -1) {
//                log.info("Creating new runtime ID for unknown block {}", id);
                runtimeId = runtimeIdAllocator.getAndIncrement();
                legacyToRuntimeId.put(id << 6, runtimeId);
                runtimeIdToLegacy.put(runtimeId, id << 6);
            }
        }
        return runtimeId;
    }

    public static int getOrCreateRuntimeId(int legacyId) throws NoSuchElementException {
        return getOrCreateRuntimeId(legacyId >> 4, legacyId & 0xf);
    }
}
