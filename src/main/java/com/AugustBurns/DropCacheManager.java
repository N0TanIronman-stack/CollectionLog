package com.AugustBurns;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages local file caching of NPC drop table data.
 * Drop data is stored as JSON files in the .runelite/collection-log-expanded directory.
 */
@Slf4j
public class DropCacheManager
{
    private static final String CACHE_DIR_NAME = "collection-log-expanded";
    private final File cacheDir;
    private final Gson gson;

    /**
     * @param gson The client's injected Gson instance (from CollectionPlugin).
     *             We call .newBuilder().setPrettyPrinting().create() on it so the
     *             cache files are human-readable, without creating a fresh Gson.
     */
    public DropCacheManager(Gson gson)
    {
        this.cacheDir = new File(RuneLite.RUNELITE_DIR, CACHE_DIR_NAME);
        this.gson = gson.newBuilder().setPrettyPrinting().create();

        if (!cacheDir.exists())
        {
            cacheDir.mkdirs();
        }
    }

    /**
     * Loads cached drop data for the given NPC, or null if not cached.
     */
    public NpcDropData loadFromCache(String npcName)
    {
        File file = getCacheFile(npcName);
        if (!file.exists())
        {
            return null;
        }

        try (Reader reader = new FileReader(file))
        {
            NpcDropData data = gson.fromJson(reader, NpcDropData.class);

            // Backward compatibility: if obtained is true but obtainedCount is 0,
            // set obtainedCount to 1 (pre-count-tracking cache files)
            if (data != null)
            {
                for (NpcDropData.DropSection section : data.getSections())
                {
                    for (NpcDropData.DropItem item : section.getItems())
                    {
                        if (item.isObtained() && item.getObtainedCount() == 0)
                        {
                            item.setObtainedCount(1);
                        }
                    }
                }
            }

            return data;
        }
        catch (Exception e)
        {
            log.warn("Failed to read cache for {}: {}", npcName, e.getMessage());
            return null;
        }
    }

    /**
     * Saves drop data to local cache.
     */
    public void saveToCache(NpcDropData data)
    {
        if (data == null || data.getNpcName() == null)
        {
            return;
        }

        File file = getCacheFile(data.getNpcName());

        try (Writer writer = new FileWriter(file))
        {
            gson.toJson(data, writer);
            log.debug("Cached drop data for {}", data.getNpcName());
        }
        catch (IOException e)
        {
            log.warn("Failed to cache data for {}: {}", data.getNpcName(), e.getMessage());
        }
    }

    /**
     * Checks if cached data is still valid (not expired).
     */
    public boolean isCacheValid(NpcDropData data, int expiryDays)
    {
        if (data == null)
        {
            return false;
        }

        if (expiryDays <= 0)
        {
            return true;
        }

        long ageMs = System.currentTimeMillis() - data.getFetchedAt();
        long maxAgeMs = TimeUnit.DAYS.toMillis(expiryDays);
        return ageMs < maxAgeMs;
    }

    /**
     * Returns a list of NPC names that have been cached.
     * Reads each JSON file to get the original NPC name.
     */
    public List<String> getCachedNpcNames()
    {
        List<String> names = new ArrayList<>();
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null)
        {
            for (File file : files)
            {
                try (Reader reader = new FileReader(file))
                {
                    NpcDropData data = gson.fromJson(reader, NpcDropData.class);
                    if (data != null && data.getNpcName() != null)
                    {
                        names.add(data.getNpcName());
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        }
        return names;
    }

    /**
     * Deletes cached data for the given NPC.
     */
    public void removeFromCache(String npcName)
    {
        File file = getCacheFile(npcName);
        if (file.exists())
        {
            file.delete();
        }
    }

    /**
     * Clears all cached drop data.
     */
    public void clearCache()
    {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null)
        {
            for (File file : files)
            {
                file.delete();
            }
        }
        log.debug("Drop cache cleared");
    }

    /**
     * Returns the number of NPCs currently cached.
     */
    public int getCacheCount()
    {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }

    // ================================================================
    //  MONSTER NAME CACHE
    // ================================================================

    private static final String MONSTER_LIST_FILE = "monster_names.json";

    /**
     * Saves the full list of monster names to a local cache file.
     */
    public void saveMonsterNames(List<String> names)
    {
        File file = new File(cacheDir, MONSTER_LIST_FILE);
        try (Writer writer = new FileWriter(file))
        {
            gson.toJson(names, writer);
            log.debug("Cached {} monster names", names.size());
        }
        catch (IOException e)
        {
            log.warn("Failed to cache monster names: {}", e.getMessage());
        }
    }

    /**
     * Loads cached monster names, or returns an empty list if not cached.
     */
    public List<String> loadMonsterNames()
    {
        File file = new File(cacheDir, MONSTER_LIST_FILE);
        if (!file.exists())
        {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file))
        {
            String[] names = gson.fromJson(reader, String[].class);
            if (names != null)
            {
                List<String> list = new ArrayList<>(names.length);
                for (String name : names)
                {
                    list.add(name);
                }
                return list;
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to read monster name cache: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Checks if the monster name cache exists and is still fresh.
     */
    public boolean isMonsterCacheValid(int expiryDays)
    {
        File file = new File(cacheDir, MONSTER_LIST_FILE);
        if (!file.exists()) return false;
        if (expiryDays <= 0) return true;
        long ageMs = System.currentTimeMillis() - file.lastModified();
        long maxAgeMs = TimeUnit.DAYS.toMillis(expiryDays);
        return ageMs < maxAgeMs;
    }

    private File getCacheFile(String npcName)
    {
        String fileName = sanitizeFileName(npcName) + ".json";
        return new File(cacheDir, fileName);
    }

    private String sanitizeFileName(String name)
    {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_\\- ]", "")
                .replaceAll("\\s+", "_")
                .trim();
    }
}