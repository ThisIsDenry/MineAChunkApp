package MineAChunkApp;

import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.util.math.DistanceMetric;
import kaptainwutax.seedutils.util.math.Vec3i;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class RavineFilter {
    public static final String OPTIONS_PATHNAME = "options.txt";
    public static final String STRUCTURE_SEEDS_PATHNAME = "output-structureSeeds.txt";
    public static final String WORLD_SEEDS_PATHNAME = "output-worldSeeds.txt";

    public static Map<Long, BPos> viableSeeds = new HashMap<>();

    public static long START_STRUCTURE_SEED = 0L;
    public static int MAX_STRUCTURE_SEEDS = 1;

    //Get the intersection. Return false if there is none within [scale] blocks. If there is one, put the coords in x,z
    public static boolean intersect(double x1, double z1, double x2, double z2, double theta1, double theta2, double scale, AtomicInteger x, AtomicInteger z){
        double s1_x = scale*Math.cos(theta1);
        double s1_y = scale*Math.sin(theta1);
        double s2_x = scale*Math.cos(theta2);
        double s2_y = scale*Math.sin(theta2);
        double s = (-s1_y * (x1 - x2) + s1_x * (z1 - z2)) / (-s2_x * s1_y + s1_x * s2_y);
        double t = ( s2_x * (z1 - z2) - s2_y * (x1 - x2)) / (-s2_x * s1_y + s1_x * s2_y);
        if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
        {
            x.set((int) (x1+(t*s1_x)));
            z.set((int) (z1+(t*s1_y)));
            return true;
        }
        return false;
    }

    public static void filterBiomeSeeds(Map<Long, Integer[]> viableSeeds, Map<Long, Integer[]> worldSeeds) {
        for (Map.Entry mElement : viableSeeds.entrySet()) {
            long structureSeed = (long) mElement.getKey();
            Integer[] intersectPoint = (Integer[]) mElement.getValue();

            for (long biomeSeed = 0; biomeSeed < 1L << 16; biomeSeed++) {
                long worldSeed = biomeSeed << 48|structureSeed;
                OverworldBiomeSource overworldBiomeSource = new OverworldBiomeSource(MCVersion.v1_16_1, worldSeed);
                BPos spawnPoint = overworldBiomeSource.getSpawnPoint();
                double distance = spawnPoint.distanceTo(new Vec3i(intersectPoint[0], 0, intersectPoint[1]), DistanceMetric.EUCLIDEAN);
                if (distance < 60.0) {
                    String intersectBiome = overworldBiomeSource.getBiome(intersectPoint[0], 0, intersectPoint[1]).getCategory().getName();
                    String spawnBiome = overworldBiomeSource.getBiome(spawnPoint.getX(), 0, spawnPoint.getZ()).getCategory().getName();
                    if (!(intersectBiome.equals("ocean")) && !(intersectBiome.equals("river")) && (spawnBiome.equals("forest"))){
                        worldSeeds.put(worldSeed, intersectPoint);
                    }
                }
            }
        }
    }

    public static void filterStructureSeeds(int offset, int threadNumber) {
        ChunkRand chunkRand = new ChunkRand();
        for (long structureSeed = START_STRUCTURE_SEED + offset; structureSeed < 1L << 48; structureSeed += threadNumber) {
            // check for ravines at chunk 0,0
            ArrayList<RavineProperties> ravines = new ArrayList<>();

            for(int chunkX =- 1; chunkX <= 1; chunkX++){
                for(int chunkZ =- 1; chunkZ <= 1; chunkZ++) {
                    RavineProperties rp = new RavineProperties(structureSeed, new CPos(chunkX, chunkZ));
                    // check if ravine is wide
                    if (!rp.generate(chunkRand)) continue;
                    //Skip anything that either isn't a ravine or is less than 5.5 wide (max width: 6.0)
                    if (rp.width < 5.5F) continue;
                    if (rp.blockPosition.getY() < 35) continue;
                    ravines.add(rp);
                }
            }

            //If there are 2 or more ravines of 5.5
            if(ravines.size() >= 2) {
                for (int i = 0; i < ravines.size() - 1; i++) {
                    for (int j = i+1; j < ravines.size(); j++) {
                        RavineProperties rp1 = ravines.get(i);
                        RavineProperties rp2 = ravines.get(j);
                        AtomicInteger intersectX = new AtomicInteger(-1000);
                        AtomicInteger intersectZ = new AtomicInteger(-1000);
                        //Check if both ravines intersect and if they are relatively close to being parralel
                        if (intersect(rp1.blockPosition.getX(), rp1.blockPosition.getZ(), rp2.blockPosition.getX(), rp2.blockPosition.getZ(), rp1.yaw, rp2.yaw, 50, intersectX, intersectZ) &&
                                Math.abs(rp1.yaw - rp2.yaw) < 0.5) {

                            viableSeeds.put(structureSeed, new BPos(intersectX.get(), 0, intersectZ.get()));
                            System.out.println("{" + structureSeed + ", " + intersectX.get() + ", " + intersectZ.get() + "},");
                        }
                    }
                }
            }

            if (viableSeeds.size() >= MAX_STRUCTURE_SEEDS) break;
        }
        /*
        Map<Long, Integer[]> worldSeeds = new HashMap<>();
        filterBiomeSeeds(viableSeeds, worldSeeds);
        try {
            FileWriter writer = new FileWriter(WORLD_SEEDS_PATHNAME);
            for (Map.Entry mElement : worldSeeds.entrySet()) {
                long worldSeed = (long) mElement.getKey();
                Integer[] intersectPoint = (Integer[]) mElement.getValue();
                int x = intersectPoint[0];
                int z = intersectPoint[1];
                writer.write(worldSeed + ", x: " + x + ", z: " + z);
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
         */
    }
}
